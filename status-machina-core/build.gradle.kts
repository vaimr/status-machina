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
    `maven-publish`
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
    // This dependency is exported to consumers, that is to say found on their compile classpath.
    // api("org.apache.commons:commons-math3:3.6.1")
    api("com.google.guava:guava:28.1-jre")

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.3.2")
    testImplementation("org.junit.jupiter:junit-jupiter-engine:5.3.2")
    testRuntimeOnly("com.h2database:h2")
    testImplementation("org.assertj:assertj-core:3.4.1")
}


tasks.getByName("jar") {
    enabled = true
}

tasks.register<Jar>("sourcesJar") {
    archiveClassifier.set("sources")
    from(sourceSets.main.get().allJava)
}

tasks.register<Jar>("javadocJar") {
    archiveClassifier.set("javadoc")
    from(tasks.javadoc.get().destinationDir)
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

publishing {
    repositories {
        maven {
            // change to point to your repo, e.g. http://my.org/repo
            name = "GitHub"
            url = uri("https://maven.pkg.github.com/entzik/status-machina")
            credentials {
                username = project.findProperty("gpr.user") as String? ?: System.getenv("GPR_USER")
                password = project.findProperty("gpr.key") as String? ?: System.getenv("GPR_API_KEY")
            }
        }
    }
    publications {
        register("mavenJava", MavenPublication::class) {
            from(components["java"])
            artifact(tasks["sourcesJar"])
            artifact(tasks["javadocJar"])
        }
    }
}
