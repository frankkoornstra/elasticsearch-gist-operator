# Image to cache dependencies
FROM openjdk:11-jdk-slim AS dependencies

RUN mkdir /app
WORKDIR /app

# Only copy gradle and fail the build so we can pull in and cache dependencies with Docker
COPY build.gradle.kts settings.gradle.kts gradlew /app/
COPY gradle/ /app/gradle
RUN ./gradlew clean build || return 0

# Image used to create jar
FROM dependencies AS jar
COPY . /app
RUN ./gradlew bootJar

# Image used to to test
FROM dependencies AS test
COPY . /app
RUN ./gradlew assemble

# Image used to run the jar
FROM openjdk:11-jre-slim AS runtime

RUN groupadd -r app \
    && useradd --no-log-init -r -g app app \
    && mkdir /app \
    && chown app:app /app

USER app

WORKDIR /app

COPY --from=jar /app/build/libs/elasticsearch-gist-operator.jar /app

CMD ["java", "-jar", "elasticsearch-gist-operator.jar"]
