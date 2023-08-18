plugins {
    id("org.gradlex.plugins.analyzer.plugin")
}

repositories {
    mavenCentral()
    gradlePluginPortal()
    google()
}

pluginAnalyzer {
    plugin("org.springframework.boot")
    plugin("org.jetbrains.kotlin.plugin.scripting")
    plugin("org.jetbrains.kotlin.kapt")
    plugin("org.jetbrains.kotlin.android")
    plugin("org.jetbrains.kotlin.android.extensions")
    plugin("org.jetbrains.kotlin.jvm")
    plugin("org.jetbrains.kotlin.native.cocoapods")
    plugin("org.jetbrains.kotlin.multiplatform.pm20")
    plugin("org.jetbrains.kotlin.plugin.parcelize")
    plugin("org.jetbrains.kotlin.multiplatform")
    plugin("org.jetbrains.kotlin.js")
    plugin("io.spring.dependency-management")
    plugin("com.gradle.build-scan")
    plugin("com.github.johnrengelman.shadow")
    plugin("org.sonarqube")
    plugin("com.github.spotbugs")
    plugin("com.diffplug.gradle.spotless")
    plugin("com.diffplug.spotless")
    plugin("org.jetbrains.kotlin.plugin.allopen")
    plugin("org.jetbrains.kotlin.plugin.spring")
    plugin("de.undercouch.download") {
        val shadowed = objects.named(Bundling::class.java, Bundling.SHADOWED)
        configuration {
            attributes {
                attribute(Bundling.BUNDLING_ATTRIBUTE, shadowed)
            }
        }
    }
    plugin("org.flywaydb.flyway")
    // TODO No tag found
//    plugin("com.gradle.enterprise")
    plugin("com.github.spotbugs-base")
    plugin("io.freefair.lombok")
    plugin("com.jfrog.artifactory")
    plugin("org.gradle.kotlin.kotlin-dsl.base")
    plugin("org.gradle.kotlin.kotlin-dsl.precompiled-script-plugins")
    plugin("org.gradle.kotlin.kotlin-dsl.compiler-settings")
    plugin("org.gradle.kotlin.kotlin-dsl")
    plugin("org.gradle.kotlin.embedded-kotlin")
    plugin("com.github.ben-manes.versions")
    plugin("com.gorylenko.gradle-git-properties") {
        shadowed = true
    }
    plugin("com.github.johnrengelman.plugin-shadow")
    plugin("org.jetbrains.kotlin.plugin.serialization")
    plugin("com.google.cloud.tools.jib") {
        configuration {
            // For some reason these dependencies were not found
            exclude("com.fasterxml.jackson.core", "jackson-databind")
            exclude("com.fasterxml.jackson.datatype", "jackson-datatype-jsr310")
        }
    }
    plugin("org.jlleitschuh.gradle.ktlint")
    plugin("org.jlleitschuh.gradle.ktlint-idea")
    plugin("io.gitlab.arturbosch.detekt")
    plugin("org.owasp.dependencycheck")
    plugin("org.jetbrains.kotlin.plugin.noarg")
    plugin("org.jetbrains.kotlin.plugin.jpa")
    plugin("com.google.protobuf")
    plugin("com.github.node-gradle.node")
    plugin("com.github.node-gradle.gulp")
    plugin("com.github.node-gradle.grunt")
    plugin("com.bmuschko.docker-spring-boot-application") {
        shadowed = true
    }
    plugin("com.bmuschko.docker-java-application") {
        shadowed = true
    }
    plugin("com.bmuschko.docker-remote-api") {
        shadowed = true
    }
    plugin("com.adarshr.test-logger")
    plugin("org.openapi.generator")
    plugin("io.micronaut.minimal.library")
    plugin("io.micronaut.minimal.application")
    plugin("io.micronaut.docker")
    plugin("io.micronaut.graalvm")
    plugin("io.micronaut.aot")
    plugin("org.ajoberstar.grgit")
    plugin("com.palantir.docker-run")
    plugin("com.palantir.docker-compose")
    plugin("com.palantir.docker")
    plugin("org.asciidoctor.convert")
    plugin("com.github.jk1.dependency-license-report")
    plugin("org.jetbrains.gradle.plugin.idea-ext")
    plugin("org.asciidoctor.jvm.pdf")
    plugin("com.avast.gradle.docker-compose")
    plugin("nebula.info-basic")
    plugin("io.micronaut.library")
    plugin("io.micronaut.application")
    plugin("org.gradle.test-retry")
    plugin("com.russianprussian.avast.gradle.docker-compose")
    plugin("au.com.dius.pact")
    plugin("com.gradle.plugin-publish")
    plugin("nu.studer.jooq")
    plugin("net.ltgt.errorprone-javacplugin")
    plugin("org.jmailen.kotlinter")
    plugin("net.ltgt.errorprone")
    plugin("net.ltgt.errorprone-base")
    plugin("io.qameta.allure")
    plugin("org.ajoberstar.grgit-service")
    plugin("net.ltgt.apt")
    plugin("net.ltgt.apt-idea")
    plugin("net.ltgt.apt-eclipse")
    plugin("org.liquibase.gradle")
    plugin("com.atlassian.performance.tools.gradle-release")
    plugin("io.franzbecker.gradle-lombok")
    plugin("com.diffplug.gradle.equinoxlaunch")
    plugin("com.diffplug.eclipse.excludebuildfolder")
    plugin("com.diffplug.gradle.oomph.ide")
    plugin("com.diffplug.eclipse.mavencentral")
    plugin("com.diffplug.eclipse.projectdeps")
    plugin("com.diffplug.gradle.osgi.bndmanifest")
    plugin("com.diffplug.gradle.p2.asmaven")
    plugin("com.diffplug.osgi.equinoxlaunch")
    plugin("com.diffplug.p2.asmaven")
    plugin("com.diffplug.eclipse.resourcefilters")
    plugin("com.diffplug.gradle.swt.nativedeps")
    plugin("com.diffplug.swt.nativedeps")
    plugin("com.diffplug.gradle.eclipse.buildproperties")
    plugin("com.diffplug.configuration-cache-for-platform-specific-build")
    plugin("com.diffplug.gradle.eclipse.projectdeps")

//    // From popular plugins – https://docs.google.com/spreadsheets/d/1p-soKHdFdYyrrmokHXg9ug03hK4VoU8oAo7g28Knels/edit#gid=45645666)0
//    plugin("com.google.dagger:hilt-android-gradle-plugin")
//    plugin("com.github.johnrengelman:shadow")
//    plugin("com.github.ben-manes:gradle-versions-plugin")
//    plugin("org.sonarsource.scanner.gradle:sonarqube-gradle-plugin")
//    plugin("com.diffplug.spotless:spotless-plugin-gradle")
//    plugin("de.undercouch:gradle-download-task") {
//        val shadowed = objects.named(Bundling::class.java, Bundling.SHADOWED)
//        configuration {
//            attributes {
//                attribute(Bundling.BUNDLING_ATTRIBUTE, shadowed)
//            }
//        }
//    }
//    plugin("gradle.plugin.org.flywaydb:gradle-plugin-publishing")
//    plugin("com.github.spotbugs.snom:spotbugs-gradle-plugin")
//    plugin("io.freefair.gradle:lombok-plugin")
//    plugin("org.jfrog.buildinfo:build-info-extractor-gradle")
//    plugin("com.gorylenko.gradle-git-properties:gradle-git-properties") {
//        val shadowed = objects.named(Bundling::class.java, Bundling.SHADOWED)
//        configuration {
//            attributes {
//                attribute(Bundling.BUNDLING_ATTRIBUTE, shadowed)
//            }
//        }
//    }
//    plugin("com.google.cloud.tools:jib-gradle-plugin") {
//        configuration {
//            // For some reason these dependencies were not found
//            exclude("com.fasterxml.jackson.core", "jackson-databind")
//            exclude("com.fasterxml.jackson.datatype", "jackson-datatype-jsr310")
//        }
//    }
//    plugin("com.google.protobuf:protobuf-gradle-plugin")
//    plugin("org.owasp:dependency-check-gradle")
//    plugin("org.openapitools:openapi-generator-gradle-plugin")
//    // TODO Is this the correct one for firebase)?
//    plugin("com.google.gms:google-services")
//    plugin("com.palantir.gradle.docker:gradle-docker")
//    plugin("org.ajoberstar.grgit:grgit-gradle")
//    plugin("com.adarshr:gradle-test-logger-plugin")
//    plugin("com.guardsquare:proguard-gradle")
}
