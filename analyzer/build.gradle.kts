plugins {
    id("org.gradlex.plugins.analyzer.java-library-conventions")
}

val sootVersion = "1.1.2"

configurations {
    create("pluginUnderTest")
}

dependencies {
    implementation("org.soot-oss:sootup.core:${sootVersion}")
    implementation("org.soot-oss:sootup.java.core:${sootVersion}")
    implementation("org.soot-oss:sootup.java.sourcecode:${sootVersion}")
    implementation("org.soot-oss:sootup.java.bytecode:${sootVersion}") {
        exclude("com.github.ThexXTURBOXx.dex2jar", "dex-tools")
    }
    implementation("org.soot-oss:sootup.jimple.parser:${sootVersion}")
    implementation("org.soot-oss:sootup.callgraph:${sootVersion}")
    implementation("org.soot-oss:sootup.analysis:${sootVersion}")

    implementation("com.google.guava:guava:32.1.2-jre")

    "pluginUnderTest"("com.vaadin:vaadin-gradle-plugin:24.1.5")

    testRuntimeOnly("org.slf4j:slf4j-simple:2.0.7")
}

val prepareTest by tasks.register<Copy>("prepareTest") {
    from(project.configurations["pluginUnderTest"])
    destinationDir = project.layout.buildDirectory.dir("plugin-under-test").get().asFile
}

tasks.test {
    dependsOn(prepareTest)
    systemProperty("plugin-directory", prepareTest.destinationDir)
    systemProperty("gradle-api", "${gradle.gradleUserHomeDir}/caches/${gradle.gradleVersion}/generated-gradle-jars/gradle-api-${gradle.gradleVersion}.jar")
}
