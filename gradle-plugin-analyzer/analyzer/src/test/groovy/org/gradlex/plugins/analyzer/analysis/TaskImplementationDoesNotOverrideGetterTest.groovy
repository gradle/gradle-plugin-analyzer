package org.gradlex.plugins.analyzer.analysis

class TaskImplementationDoesNotOverrideGetterTest extends AbstractAnalysisSpec {
    def "can detect type overriding getter"() {
        classLoader.parseClass("""
            class CustomTask extends org.gradle.api.DefaultTask {
                @Override
                boolean getEnabled() {
                    System.out.println("Overriding")
                    return false
                }
            }
        """)

        when:
        analyzer.analyze(new TaskImplementationDoesNotOverrideGetter())

        then:
        reports == [
            "WARN: The getter getEnabled() in LCustomTask overrides Gradle API from Lorg/gradle/api/DefaultTask"
        ]
    }
}
