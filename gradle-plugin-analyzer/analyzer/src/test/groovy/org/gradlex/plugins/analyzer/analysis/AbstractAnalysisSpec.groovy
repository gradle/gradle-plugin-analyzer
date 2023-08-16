package org.gradlex.plugins.analyzer.analysis

import org.codehaus.groovy.control.CompilerConfiguration
import org.gradlex.plugins.analyzer.Analyzer
import org.gradlex.plugins.analyzer.DefaultAnalyzer
import spock.lang.Specification

import java.nio.file.Paths

class AbstractAnalysisSpec extends Specification {
    GroovyClassLoader classLoader
    List<String> reports
    Analyzer analyzer

    def setup() {
        def gradleApi = System.getProperty("gradle-api")

        File targetDirectory = new File("build/test-classes/${getClass().simpleName}")
        // TODO Clear target directory before every run, though it causes some weird failure if we do this
//        assert targetDirectory.deleteDir()
        CompilerConfiguration config = new CompilerConfiguration()
        config.setTargetDirectory(targetDirectory)

        this.classLoader = new GroovyClassLoader(Thread.currentThread().contextClassLoader, config)
        classLoader.addClasspath(gradleApi)

        def files = [Paths.get(gradleApi), targetDirectory.toPath()]
        reports = []
        analyzer = new DefaultAnalyzer(files, { level, message ->
            reports += "$level: $message" as String
        })
    }
}
