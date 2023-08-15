package org.gradlex.plugins.analyzer

import spock.lang.Specification

class AnalyzerTest extends Specification {
    def "can instantiate analyzer"() {
        expect:
        new Analyzer()
    }
}
