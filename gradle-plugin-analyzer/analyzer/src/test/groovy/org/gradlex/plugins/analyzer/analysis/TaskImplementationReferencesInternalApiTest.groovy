package org.gradlex.plugins.analyzer.analysis

class TaskImplementationReferencesInternalApiTest extends AbstractAnalysisSpec {
    def "does not signal public API references"() {
        compileJava("""
            class CustomTask extends org.gradle.api.DefaultTask {
                @org.gradle.api.tasks.TaskAction
                public void execute() {
                }
            }
        """)

        when:
        analyzer.analyze(new TaskImplementationReferencesInternalApi())

        then:
        reports == []
    }

    def "signals extending internal API"() {
        compileJava("""
            class CustomTask extends org.gradle.jvm.toolchain.internal.task.ShowToolchainsTask {
            }
        """)

        when:
        analyzer.analyze(new TaskImplementationReferencesInternalApi())

        then:
        reports == [
            "WARN: Method < Application, LCustomTask, <init>()V > in LCustomTask references internal Gradle type: Lorg/gradle/jvm/toolchain/internal/task/ShowToolchainsTask",
            "WARN: Type LCustomTask extends internal Gradle API Lorg/gradle/jvm/toolchain/internal/task/ShowToolchainsTask",
        ]
    }

    def "signals calling internal API"() {
        compileJava("""
            class CustomTask extends org.gradle.api.DefaultTask {
                @org.gradle.api.tasks.TaskAction
                public void execute() {
                    if (hasTaskActions()) {
                        System.out.println("We have actions, obviously!");
                    }
                }
            }
        """)

        when:
        analyzer.analyze(new TaskImplementationReferencesInternalApi())

        then:
        reports == []
    }
}
