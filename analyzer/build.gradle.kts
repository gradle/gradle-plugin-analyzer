import java.util.stream.Collectors

plugins {
    id("org.gradlex.plugins.analyzer.java-library-conventions")
}

val walaVersion = "1.6.2"

configurations {
    create("pluginUnderTest")
}

dependencies {

    implementation("com.ibm.wala:com.ibm.wala.shrike:${walaVersion}")
    implementation("com.ibm.wala:com.ibm.wala.util:${walaVersion}")
    implementation("com.ibm.wala:com.ibm.wala.core:${walaVersion}")
    implementation("com.ibm.wala:com.ibm.wala.cast:${walaVersion}")
    implementation("com.ibm.wala:com.ibm.wala.cast.java:${walaVersion}")
    implementation("com.ibm.wala:com.ibm.wala.cast.java.ecj:${walaVersion}")

    implementation("com.google.guava:guava:32.1.2-jre")

     "pluginUnderTest"("org.jfrog.buildinfo:build-info-extractor-gradle:5.1.0")
}

tasks.test {
    systemProperty("plugin-files", project.configurations["pluginUnderTest"].files.stream().map(File::getAbsolutePath).collect(Collectors.joining(File.pathSeparator)))
//    systemProperty("plugin-files", tasks.compileJava.get().destinationDirectory.get().asFile.absoluteFile)
    systemProperty("gradle-api", "${gradle.gradleUserHomeDir}/caches/${gradle.gradleVersion}/generated-gradle-jars/gradle-api-${gradle.gradleVersion}.jar")
    maxHeapSize = "4096m"
}
