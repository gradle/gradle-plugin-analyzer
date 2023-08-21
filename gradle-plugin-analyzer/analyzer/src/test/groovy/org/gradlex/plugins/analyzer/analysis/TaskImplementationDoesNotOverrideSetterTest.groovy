package org.gradlex.plugins.analyzer.analysis

class TaskImplementationDoesNotOverrideSetterTest extends AbstractAnalysisSpec {
    def "can detect type overriding setter"() {
        compileJava("""
            class CustomTask extends org.gradle.api.DefaultTask {
                @Override
                public void setEnabled(boolean enabled) {
                    super.setEnabled(enabled);
                }
            }
        """)

        when:
        analyzer.analyze(new TaskImplementationDoesNotOverrideSetter())

        then:
        reports == [
            "INFO: The setter setEnabled() in LCustomTask overrides Gradle API from Lorg/gradle/api/DefaultTask, but calls only super()"
        ]
    }
}
