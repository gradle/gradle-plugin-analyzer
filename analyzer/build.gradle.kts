import java.util.stream.Collectors

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
}

tasks.test {
    systemProperty("plugin-files", project.configurations["pluginUnderTest"].files.stream().map(File::getAbsolutePath).collect(Collectors.joining(File.pathSeparator)))
    systemProperty("gradle-api", "${gradle.gradleUserHomeDir}/caches/${gradle.gradleVersion}/generated-gradle-jars/gradle-api-${gradle.gradleVersion}.jar")
}
