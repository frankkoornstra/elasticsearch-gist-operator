import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    val kotlinVersion = "1.3.72"

    kotlin("jvm") version kotlinVersion
    kotlin("plugin.spring") version kotlinVersion
    id("org.springframework.boot") version "2.3.0.RELEASE"
    id("com.diffplug.gradle.spotless") version "4.1.0"
    id("com.github.ben-manes.versions") version "0.28.0"
    id("jacoco")
}

repositories {
    mavenCentral()
    jcenter()
}

val elasticsearchVersion = "6.8.+"
val operatorFrameworkVersion = "1.2.+"
val junitVersion = "5.6.+"

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation(kotlin("reflect"))

    implementation("com.github.containersolutions:operator-framework:$operatorFrameworkVersion")
    implementation("com.github.containersolutions:spring-boot-operator-framework-starter:$operatorFrameworkVersion")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.11.+")
    implementation("org.elasticsearch.client:elasticsearch-rest-high-level-client:$elasticsearchVersion")
    implementation("ch.qos.logback:logback-classic:1.2.+")
    implementation("org.apache.logging.log4j:log4j-core:2.13.+")

    testImplementation("org.springframework.boot:spring-boot-starter-test:2.3.0.RELEASE")
    testImplementation("org.junit.jupiter:junit-jupiter-api:$junitVersion")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junitVersion")
    testImplementation("io.mockk:mockk:1.10.+")
}

tasks.test {
    useJUnitPlatform() {
        val includedTags = System.getProperty("includeTags")
        if (!includedTags.isNullOrBlank()) {
            includeTags(includedTags)
        }

        val excludedTags = System.getProperty("excludeTags")
        if (!excludedTags.isNullOrBlank()) {
            excludeTags(excludedTags)
        }
    }
    finalizedBy(tasks.jacocoTestReport)
}

val excludeFromCoverage = listOf(
    "Application.class",
    "OperatorKt.class"
).map { "nl/frankkoornstra/elasticsearchgistoperator/$it" }
tasks.jacocoTestReport {
    reports {
        xml.isEnabled = true
        csv.isEnabled = false
        html.isEnabled = true
    }
    classDirectories.setFrom(
        sourceSets.main.get().output.asFileTree.matching {
            exclude(excludeFromCoverage)
        }
    )
}
tasks.jacocoTestCoverageVerification {
    violationRules {
        rule {
            limit {
                minimum = "1.000".toBigDecimal()
            }
        }
    }
    classDirectories.setFrom(
        sourceSets.main.get().output.asFileTree.matching {
            exclude(excludeFromCoverage)
        }
    )
}

tasks.withType<DependencyUpdatesTask> {
    checkForGradleUpdate = true
    gradleReleaseChannel = "current"

    rejectVersionIf {
        // Reject non-stable versions
        val nonStableKeyword = listOf("ALPHA", "BETA", "RC").any { candidate.displayName.toUpperCase().contains(it) }
        val stableKeyword = listOf("RELEASE", "FINAL", "GA").any { candidate.displayName.toUpperCase().contains(it) }
        val versionRegex = "^[0-9,.v-]+(-r)?$".toRegex()
        val isStable = !nonStableKeyword && (stableKeyword || versionRegex.matches(candidate.version))

        if (isStable.not()) {
            return@rejectVersionIf true
        }

        // Reject ES upgrades that don't match the major and minor targeted version
        // ES library major and minor version indicates its compatibility with the cluster
        val isElasticsearchClient = candidate.group == "org.elasticsearch.client" &&
                candidate.module == "elasticsearch-rest-high-level-client"
        val isHigherMajor = candidate.version.split(".")[0] > elasticsearchVersion.split(".")[0]
        val isHigherMinor = candidate.version.split(".")[1] > elasticsearchVersion.split(".")[1]

        isElasticsearchClient && (isHigherMajor || isHigherMinor)
    }
}

java.sourceCompatibility = JavaVersion.VERSION_11
java.targetCompatibility = JavaVersion.VERSION_11

tasks.withType<KotlinCompile> {
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
