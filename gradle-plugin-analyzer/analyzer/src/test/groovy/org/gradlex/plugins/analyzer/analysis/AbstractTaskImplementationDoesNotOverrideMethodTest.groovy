package org.gradlex.plugins.analyzer.analysis

import com.google.common.base.Predicates

class AbstractTaskImplementationDoesNotOverrideMethodTest extends AbstractAnalysisSpec {
    def analysis = new AbstractTaskImplementationDoesNotOverrideMethod("method", Predicates.alwaysTrue()) {}

    def "can detect type overriding method"() {
        compileJava("""
            class CustomTask extends org.gradle.api.DefaultTask {
                @Override
                public boolean getEnabled() {
                    return super.getEnabled();
                }
            }
        """)

        when:
        analyzer.analyze(analysis)

        then:
        reports == [
            "WARN: The method getEnabled() in LCustomTask overrides Gradle API from Lorg/gradle/api/DefaultTask"
        ]
    }
}
