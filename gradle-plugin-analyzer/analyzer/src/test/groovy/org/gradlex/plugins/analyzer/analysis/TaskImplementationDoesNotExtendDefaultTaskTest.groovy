package org.gradlex.plugins.analyzer.analysis

class TaskImplementationDoesNotExtendDefaultTaskTest extends AbstractAnalysisSpec {
    def "can detect task extending SourceTask"() {
        classLoader.parseClass("""
            class GoodTask extends org.gradle.api.DefaultTask {}
            class BadTask extends org.gradle.api.tasks.SourceTask {}
        """)

        when:
        analyzer.analyze(new TaskImplementationDoesNotExtendDefaultTask())

        then:
        reports == [
            "DEBUG: Type LGoodTask is a task and it extends DefaultTask directly",
            "WARN: Type LBadTask is a task but extends Lorg/gradle/api/tasks/SourceTask instead of DefaultTask",
        ]
    }
}
