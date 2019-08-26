/*
 * This file was generated by the Gradle 'init' task.
 *
 * This generated file contains a sample Java Library project to get you started.
 * For more details take a look at the Java Libraries chapter in the Gradle
 * user guide available at https://docs.gradle.org/5.0/userguide/java_library_plugin.html
 */

plugins {
    // Apply the java-library plugin to add support for Java Library
    `java-library`
    `idea`
    `eclipse`
    id("org.springframework.boot") version "2.1.6.RELEASE"
}

apply(plugin = "io.spring.dependency-management")

repositories {
    // Use jcenter for resolving your dependencies.
    // You can declare any Maven/Ivy/file repository here.
    jcenter()
    mavenCentral()
}

dependencies {
    api(project(":core"))

    // This dependency is exported to consumers, that is to say found on their compile classpath.
    // api("org.apache.commons:commons-math3:3.6.1")

    // This dependency is used internally, and not exposed to consumers on their own compile classpath.
    // implementation("com.google.guava:guava:26.0-jre")
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("javax.validation:validation-api")

    // Use JUnit test framework
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.junit.jupiter:junit-jupiter-api")
    testImplementation("org.junit.jupiter:junit-jupiter-engine")
    testRuntimeOnly("com.h2database:h2")
//    testRuntimeOnly("org.liquibase:liquibase-core")
    testImplementation("org.assertj:assertj-core:3.4.1")
}


tasks.getByName("jar") {
    enabled = true
}

tasks.getByName("bootJar") {
    enabled = false
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
}