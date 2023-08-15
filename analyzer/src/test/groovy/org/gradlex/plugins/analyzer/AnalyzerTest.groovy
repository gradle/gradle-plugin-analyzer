package org.gradlex.plugins.analyzer

import spock.lang.Specification

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.stream.Stream

class AnalyzerTest extends Specification {
    def "can instantiate analyzer"() {
        def pluginDirectory = System.getProperty("plugin-directory")
        def files = []

        try (Stream<Path> entries = Files.list(Paths.get(pluginDirectory))) {
            entries
                    .filter(path -> path.getFileName().toString().endsWith(".jar"))
                    .forEach(files::add)
        }
        def gradleApi = System.getProperty("gradle-api")
        files.add(Paths.get(gradleApi))

        expect:
        new Analyzer(files).analyze()
    }
}
