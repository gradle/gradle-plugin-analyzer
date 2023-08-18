plugins {
    id("org.gradlex.plugins.analyzer.plugin")
}

repositories {
    mavenCentral()
    gradlePluginPortal()
    google()
}

pluginAnalyzer {
    // From top community plugins https://docs.google.com/spreadsheets/d/1eNmiNu6VNGQrLApwWPjCi4a2LldIHFGOcQTGj2A4qQM/
    plugin("au.com.dius.pact")
    plugin("com.adarshr.test-logger")
    plugin("com.atlassian.performance.tools.gradle-release")
    plugin("com.avast.gradle.docker-compose")
    plugin("com.bmuschko.docker-java-application") { shadowed() }
    plugin("com.bmuschko.docker-remote-api") { shadowed() }
    plugin("com.bmuschko.docker-spring-boot-application") { shadowed() }
    plugin("com.diffplug.configuration-cache-for-platform-specific-build")
    plugin("com.diffplug.eclipse.excludebuildfolder")
    plugin("com.diffplug.eclipse.mavencentral")
    plugin("com.diffplug.eclipse.projectdeps")
    plugin("com.diffplug.eclipse.resourcefilters")
    plugin("com.diffplug.gradle.eclipse.buildproperties")
    plugin("com.diffplug.gradle.eclipse.projectdeps")
    plugin("com.diffplug.gradle.equinoxlaunch")
    plugin("com.diffplug.gradle.oomph.ide")
    plugin("com.diffplug.gradle.osgi.bndmanifest")
    plugin("com.diffplug.gradle.p2.asmaven")
    plugin("com.diffplug.gradle.spotless")
    plugin("com.diffplug.gradle.swt.nativedeps")
    plugin("com.diffplug.osgi.equinoxlaunch")
    plugin("com.diffplug.p2.asmaven")
    plugin("com.diffplug.spotless")
    plugin("com.diffplug.swt.nativedeps")
    plugin("com.github.ben-manes.versions")
    plugin("com.github.jk1.dependency-license-report")
    plugin("com.github.johnrengelman.plugin-shadow")
    plugin("com.github.johnrengelman.shadow")
    plugin("com.github.node-gradle.grunt")
    plugin("com.github.node-gradle.gulp")
    plugin("com.github.node-gradle.node")
    plugin("com.github.spotbugs")
    plugin("com.github.spotbugs-base")
    plugin("com.google.cloud.tools.jib") {
        configuration {
            // For some reason these dependencies were not found
            exclude("com.fasterxml.jackson.core", "jackson-databind")
            exclude("com.fasterxml.jackson.datatype", "jackson-datatype-jsr310")
        }
    }
    plugin("com.google.protobuf")
    plugin("com.gorylenko.gradle-git-properties") { shadowed() }
    plugin("com.gradle.build-scan")
    plugin("com.gradle.plugin-publish") { shadowed() }
    plugin("com.jfrog.artifactory")
    plugin("com.palantir.docker")
    plugin("com.palantir.docker-compose")
    plugin("com.palantir.docker-run")
    plugin("com.russianprussian.avast.gradle.docker-compose")
    plugin("de.undercouch.download") { shadowed() }
    plugin("io.franzbecker.gradle-lombok")
    plugin("io.freefair.lombok")
    plugin("io.gitlab.arturbosch.detekt")
    // TODO These both require shadowed and non-shadowed??
//    plugin("io.micronaut.aot")
//    plugin("io.micronaut.application")
//    plugin("io.micronaut.docker")
//    plugin("io.micronaut.graalvm")
//    plugin("io.micronaut.library")
//    plugin("io.micronaut.minimal.application")
//    plugin("io.micronaut.minimal.library")
    plugin("io.qameta.allure")
    plugin("io.spring.dependency-management")
    plugin("nebula.info-basic")
    plugin("net.ltgt.apt")
    plugin("net.ltgt.apt-eclipse")
    plugin("net.ltgt.apt-idea")
    plugin("net.ltgt.errorprone")
    plugin("net.ltgt.errorprone-base")
    plugin("net.ltgt.errorprone-javacplugin")
    plugin("nu.studer.jooq")
    plugin("org.ajoberstar.grgit")
    plugin("org.ajoberstar.grgit-service")
    plugin("org.asciidoctor.convert")
    plugin("org.asciidoctor.jvm.pdf")
    plugin("org.flywaydb.flyway")
    plugin("org.gradle.kotlin.embedded-kotlin")
    plugin("org.gradle.kotlin.kotlin-dsl")
    plugin("org.gradle.kotlin.kotlin-dsl.base")
    plugin("org.gradle.kotlin.kotlin-dsl.compiler-settings")
    plugin("org.gradle.kotlin.kotlin-dsl.precompiled-script-plugins")
    plugin("org.gradle.test-retry") { shadowed() }
    plugin("org.jetbrains.gradle.plugin.idea-ext")
    plugin("org.jetbrains.kotlin.android")
    plugin("org.jetbrains.kotlin.android.extensions")
    plugin("org.jetbrains.kotlin.js")
    plugin("org.jetbrains.kotlin.jvm")
    plugin("org.jetbrains.kotlin.kapt")
    plugin("org.jetbrains.kotlin.multiplatform")
    plugin("org.jetbrains.kotlin.multiplatform.pm20")
    plugin("org.jetbrains.kotlin.native.cocoapods")
    plugin("org.jetbrains.kotlin.plugin.allopen")
    plugin("org.jetbrains.kotlin.plugin.jpa")
    plugin("org.jetbrains.kotlin.plugin.noarg")
    plugin("org.jetbrains.kotlin.plugin.parcelize")
    plugin("org.jetbrains.kotlin.plugin.scripting")
    plugin("org.jetbrains.kotlin.plugin.serialization")
    plugin("org.jetbrains.kotlin.plugin.spring")
    plugin("org.jlleitschuh.gradle.ktlint")
    plugin("org.jlleitschuh.gradle.ktlint-idea")
    plugin("org.jmailen.kotlinter")
    plugin("org.liquibase.gradle")
    plugin("org.openapi.generator")
    plugin("org.owasp.dependencycheck")
    plugin("org.sonarqube")
    plugin("org.springframework.boot")

    // From popular plugins – https://docs.google.com/spreadsheets/d/1p-soKHdFdYyrrmokHXg9ug03hK4VoU8oAo7g28Knels/
    plugin("com.google.dagger") {
        artifact = "com.google.dagger:hilt-android-gradle-plugin:2.47"
    }
    plugin("com.google.services") {
        artifact = "com.google.gms:google-services:4.3.15"
    }
    plugin("com.guardsquare.proguard") {
        artifact = "com.guardsquare:proguard-gradle:7.3.2"
    }
}
