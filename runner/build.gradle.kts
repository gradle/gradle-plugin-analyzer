import Org_gradlex_plugins_analyzer_plugin_gradle.PluginAnalyzerTask

plugins {
    id("org.gradlex.plugins.analyzer.plugin")
}

tasks.register<PluginAnalyzerTask>("analyzePlugin") {
}
