plugins {
    id("org.gradlex.plugins.analyzer.plugin")
}

repositories {
    mavenCentral()
    gradlePluginPortal()
    google()
}

pluginAnalyzer {
    plugin("org.gradle.kotlin:gradle-kotlin-dsl-plugins:4.1.1")
    plugin("org.springframework.boot:spring-boot-gradle-plugin:3.1.2")
    plugin("org.xbib.gradle.plugin:gradle-plugin-shadow:3.0.0")

    // From popular plugins – https://docs.google.com/spreadsheets/d/1p-soKHdFdYyrrmokHXg9ug03hK4VoU8oAo7g28Knels/edit#gid=45645666)0
    plugin("com.google.dagger:hilt-android-gradle-plugin:2.47")
    plugin("com.github.johnrengelman:shadow:8.1.1")
    plugin("com.github.ben-manes:gradle-versions-plugin:0.47.0")
    plugin("org.sonarsource.scanner.gradle:sonarqube-gradle-plugin:4.3.0.3225")
    plugin("com.diffplug.spotless:spotless-plugin-gradle:6.20.0")
    // TODO Misses gradle-api:8.0.1.ja)r
//    plugin("de.undercouch:gradle-download-task:5.4.0") {
//        attribute(Bundling.BUNDLING_ATTRIBUTE, objects.named(Bundling::class.java, Bundling.SHADOWED))
//    }
    plugin("gradle.plugin.org.flywaydb:gradle-plugin-publishing:9.21.1")
    plugin("com.github.spotbugs.snom:spotbugs-gradle-plugin:6.0.0-beta.3")
    plugin("io.freefair.gradle:lombok-plugin:8.2.2")
    plugin("org.jfrog.buildinfo:build-info-extractor-gradle:5.1.0")
    // TODO Misses gradle-api-7.0.jar
    plugin("com.gorylenko.gradle-git-properties:gradle-git-properties:2.4.1") {
        val shadowed = objects.named(Bundling::class.java, Bundling.SHADOWED)
        configuration {
            attributes {
                attribute(Bundling.BUNDLING_ATTRIBUTE, shadowed)
            }
        }
    }
    // TODO Could not find com.fasterxml.jackson.core:jackson-databind:).
    // plugin("com.google.cloud.tools:jib-gradle-plugin:3.3.2")
    plugin("com.google.protobuf:protobuf-gradle-plugin:0.9.4")
    plugin("org.owasp:dependency-check-gradle:8.3.1")
    plugin("org.openapitools:openapi-generator-gradle-plugin:7.0.0-beta")
    // TODO Is this the correct one for firebase)?
    plugin("com.google.gms:google-services:4.3.15")
    plugin("com.palantir.gradle.docker:gradle-docker:0.35.0")
    plugin("org.ajoberstar.grgit:grgit-gradle:5.0.0-rc.3")
    plugin("com.adarshr:gradle-test-logger-plugin:3.2.0")
    // TODO WALA: InvalidClassFileException: Class file invalid at 10: bad magic number: 134709325)2
    // plugin("com.github.bumptech.glide:glide:4.15.1")
    plugin("com.guardsquare:proguard-gradle:7.3.2")
}
