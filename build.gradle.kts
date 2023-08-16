plugins {
    base
}

tasks.check.configure {
    dependsOn(gradle.includedBuild("gradle-plugin-analyzer").task(":check"))
}
