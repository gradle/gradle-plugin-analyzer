package org.gradlex.plugins.analyzer.analysis

import com.google.common.collect.ImmutableList
import org.codehaus.groovy.control.CompilerConfiguration
import org.gradlex.plugins.analyzer.Analyzer
import org.gradlex.plugins.analyzer.DefaultAnalyzer
import spock.lang.Specification

import java.nio.file.Path
import java.nio.file.Paths

class AbstractAnalysisSpec extends Specification {
    GroovyClassLoader classLoader
    List<String> reports
    List<Path> files
    Analyzer analyzer

    def setup() {
        def gradleApi = System.getProperty("gradle-api")

        File targetDirectory = new File("build/test-classes/${getClass().simpleName}")
        assert targetDirectory.deleteDir()
        CompilerConfiguration config = new CompilerConfiguration()
        config.setTargetDirectory(targetDirectory)

        this.classLoader = new GroovyClassLoader(Thread.currentThread().contextClassLoader, config)
        classLoader.addClasspath(gradleApi)

        files = [Paths.get(gradleApi), targetDirectory.toPath()]
        reports = []
    }

    protected Analyzer getAnalyzer() {
        analyzer = new DefaultAnalyzer(files, { level, message ->
            reports += "$level: $message" as String
        })
    }

    protected List<String> getReports() {
        ImmutableList.sortedCopyOf(reports)
    }
}
