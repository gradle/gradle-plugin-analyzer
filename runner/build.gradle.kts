import Org_gradlex_plugins_analyzer_plugin_gradle.PluginAnalyzerTask

plugins {
    id("org.gradlex.plugins.analyzer.plugin")
}

repositories {
    mavenCentral()
    gradlePluginPortal()
    google()
}

pluginAnalyzer {
    plugins.addAll(listOf(
        "org.gradle.kotlin:gradle-kotlin-dsl-plugins:4.1.1",
        "org.springframework.boot:spring-boot-gradle-plugin:3.1.2",
        "org.xbib.gradle.plugin:gradle-plugin-shadow:3.0.0",

        // From popular plugins – https://docs.google.com/spreadsheets/d/1p-soKHdFdYyrrmokHXg9ug03hK4VoU8oAo7g28Knels/edit#gid=456456660
        "com.google.dagger:hilt-android-gradle-plugin:2.47",
        // TODO Shadow plugin has a problem with matching attributes
        // "com.github.johnrengelman:shadow:8.1.1",
        "com.github.ben-manes:gradle-versions-plugin:0.47.0",
        "org.sonarsource.scanner.gradle:sonarqube-gradle-plugin:4.3.0.3225",
        "com.diffplug.spotless:spotless-plugin-gradle:6.20.0",
        // TODO Misses gradle-api:8.0.1.jar
        // "de.undercouch:gradle-download-task:5.4.0",
        "gradle.plugin.org.flywaydb:gradle-plugin-publishing:9.21.1",
        "com.github.spotbugs.snom:spotbugs-gradle-plugin:6.0.0-beta.3",
        "io.freefair.gradle:lombok-plugin:8.2.2",
        "org.jfrog.buildinfo:build-info-extractor-gradle:5.1.0",
        // TODO Misses gradle-api-7.0.jar
        // "com.gorylenko.gradle-git-properties:gradle-git-properties:2.4.1",
        // TODO Could not find com.fasterxml.jackson.core:jackson-databind:.
        // "com.google.cloud.tools:jib-gradle-plugin:3.3.2",
        "com.google.protobuf:protobuf-gradle-plugin:0.9.4",
        "org.owasp:dependency-check-gradle:8.3.1",
        // TODO Could not resolve com.github.ben-manes.caffeine:caffeine:2.9.3 (because of shadow)
        // "org.openapitools:openapi-generator-gradle-plugin:7.0.0-beta",
        // TODO Is this the correct one for firebase?
        "com.google.gms:google-services:4.3.15",
        "com.palantir.gradle.docker:gradle-docker:0.35.0",
        "org.ajoberstar.grgit:grgit-gradle:5.0.0-rc.3",
        "com.adarshr:gradle-test-logger-plugin:3.2.0",
        // TODO WALA: InvalidClassFileException: Class file invalid at 10: bad magic number: 1347093252
        // "com.github.bumptech.glide:glide:4.15.1",

        "com.guardsquare:proguard-gradle:7.1.0",
    ))
}
