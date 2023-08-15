package org.gradlex.plugins.analyzer

import spock.lang.Specification

import java.nio.file.Paths

class AnalyzerTest extends Specification {
    def "can instantiate analyzer"() {
        def pluginDirectory = System.getProperty("plugin-directory")
        println pluginDirectory

        expect:
        new Analyzer().analyze(Paths.get(pluginDirectory))
    }
}
