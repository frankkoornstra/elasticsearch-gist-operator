plugins {
    kotlin("jvm") version "1.3.71"
    kotlin("plugin.spring") version "1.3.71"
    id("org.springframework.boot") version "2.2.6.RELEASE"
    id("com.diffplug.gradle.spotless") version "3.28.1"
}

repositories {
    mavenCentral()
    jcenter()
}

val elasticsearchVersion = "6.8.+"
val operatorFrameworkVersion = "1.1.+"
val springBootVersion = "2.2.6.RELEASE"
val junitVersion = "5.6.2"

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation(kotlin("reflect"))

    implementation("com.github.containersolutions:operator-framework:$operatorFrameworkVersion")
    implementation("com.github.containersolutions:spring-boot-operator-framework-starter:$operatorFrameworkVersion")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.10.+")
    implementation("org.elasticsearch.client:elasticsearch-rest-high-level-client:$elasticsearchVersion")
    implementation("ch.qos.logback:logback-classic:1.2.3")
    implementation("org.apache.logging.log4j:log4j-core:2.13.1")

    testImplementation("org.junit.jupiter:junit-jupiter-api:$junitVersion")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junitVersion")
}

tasks.test {
    useJUnitPlatform()
}

java.sourceCompatibility = JavaVersion.VERSION_11
java.targetCompatibility = JavaVersion.VERSION_11

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict")
        jvmTarget = "1.8"
    }
}

spotless {
    kotlin {
        ktlint()
    }
    kotlinGradle {
        ktlint()
    }
}
