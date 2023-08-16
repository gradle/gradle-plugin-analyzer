plugins {
    base
}

tasks.check.configure {
    dependsOn(project(":analyzer").tasks.check)
    dependsOn(project(":plugin").tasks.check)
}
