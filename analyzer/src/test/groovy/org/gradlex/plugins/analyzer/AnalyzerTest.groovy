package org.gradlex.plugins.analyzer

import spock.lang.Specification

import java.nio.file.Paths

class AnalyzerTest extends Specification {
    def "can instantiate analyzer"() {
        def pluginDirectory = System.getProperty("plugin-directory")

        expect:
        new Analyzer(Paths.get(pluginDirectory)).analyze()
    }
}
