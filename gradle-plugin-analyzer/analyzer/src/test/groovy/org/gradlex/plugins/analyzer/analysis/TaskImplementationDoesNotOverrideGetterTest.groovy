package org.gradlex.plugins.analyzer.analysis

class TaskImplementationDoesNotOverrideGetterTest extends AbstractAnalysisSpec {
    def "can detect type overriding getter"() {
        classLoader.parseClass("""
            class CustomTask extends org.gradle.api.tasks.SourceTask {
                @Override
                org.gradle.api.file.FileTree getSource() {
                    System.out.println("Overriding")
                    return super.getSource()
                }
            }
        """)

        when:
        analyzer.analyze(new TaskImplementationDoesNotOverrideGetter())

        then:
        reports == [
            "WARN: The getter getSource() in LCustomTask overrides Gradle API from Lorg/gradle/api/tasks/SourceTask"
        ]
    }
}
