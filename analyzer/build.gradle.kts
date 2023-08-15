plugins {
    id("org.gradlex.plugins.analyzer.java-library-conventions")
}

val sootVersion = "1.1.2"

dependencies {
    implementation("org.soot-oss:sootup.core:${sootVersion}")
    implementation("org.soot-oss:sootup.java.core:${sootVersion}")
    implementation("org.soot-oss:sootup.java.sourcecode:${sootVersion}")
    implementation("org.soot-oss:sootup.java.bytecode:${sootVersion}")
    implementation("org.soot-oss:sootup.jimple.parser:${sootVersion}")
    implementation("org.soot-oss:sootup.callgraph:${sootVersion}")
    implementation("org.soot-oss:sootup.analysis:${sootVersion}")
}
