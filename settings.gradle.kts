import org.gradle.api.internal.FeaturePreviews

pluginManagement {
    includeBuild("gradle-plugin-analyzer")
}

plugins {
    // Apply the foojay-resolver plugin to allow automatic download of JDKs
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.4.0"
}

rootProject.name = "plugin-analyzer-runner"
include("runner")

enableFeaturePreview(FeaturePreviews.Feature.TYPESAFE_PROJECT_ACCESSORS.name)
enableFeaturePreview(FeaturePreviews.Feature.STABLE_CONFIGURATION_CACHE.name)
