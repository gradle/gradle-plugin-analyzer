package org.gradlex.plugins.analyzer.analysis

class TaskImplementationDoesNotOverrideGetterTest extends AbstractAnalysisSpec {
    def "can detect type overriding getter"() {
        compileJava("""
            class CustomTask extends org.gradle.api.tasks.SourceTask {
                @Override
                public org.gradle.api.file.FileTree getSource() {
                    return super.getSource();
                }
            }
        """)

        when:
        analyzer.analyze(new TaskImplementationDoesNotOverrideGetter())

        then:
        reports == [
            "INFO: The getter getSource() in LCustomTask overrides Gradle API from Lorg/gradle/api/tasks/SourceTask, but calls only super()"
        ]
    }
}
