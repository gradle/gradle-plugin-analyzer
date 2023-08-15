import Org_gradlex_plugins_analyzer_plugin_gradle.PluginAnalyzerTask

plugins {
    id("org.gradlex.plugins.analyzer.plugin")
}

repositories {
    mavenCentral()
    gradlePluginPortal()
}

pluginAnalyzer {
    plugins.addAll(listOf(
        "org.gradle.kotlin:gradle-kotlin-dsl-plugins:4.1.1",
        "org.springframework.boot:spring-boot-gradle-plugin:3.1.2",
        "org.xbib.gradle.plugin:gradle-plugin-shadow:3.0.0",
    ))
}

tasks.register<PluginAnalyzerTask>("analyzePlugin") {
}
