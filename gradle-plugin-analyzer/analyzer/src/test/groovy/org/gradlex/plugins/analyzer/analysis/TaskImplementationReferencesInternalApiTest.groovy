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
            "WARN: Method CustomTask.<init>()V references internal Gradle type: Lorg/gradle/jvm/toolchain/internal/task/ShowToolchainsTask",
            "WARN: Type LCustomTask extends internal Gradle API Lorg/gradle/jvm/toolchain/internal/task/ShowToolchainsTask",
        ]
    }

    def "signals calling internal API"() {
        compileJava("""
            class CustomTask extends org.gradle.api.DefaultTask {
                static {
                    System.out.println(org.gradle.api.internal.TaskOutputsInternal.class.getName());
                }
            
                {
                    hasTaskActions();
                }
            
                @org.gradle.api.tasks.TaskAction
                public void execute() {
                    if (hasTaskActions()) {
                        System.out.println("We have actions, obviously!");
                    }
                    org.gradle.api.internal.TaskOutputsInternal[] lajos = new org.gradle.api.internal.TaskOutputsInternal[10];
                }
            }
        """)

        when:
        analyzer.analyze(new TaskImplementationReferencesInternalApi())

        then:
        reports == [
            "WARN: Method CustomTask.<clinit>()V references internal Gradle type: Lorg/gradle/api/internal/TaskOutputsInternal",
            "WARN: Method CustomTask.<init>()V references internal Gradle type: Lorg/gradle/api/internal/AbstractTask",
            "WARN: Method CustomTask.execute()V references internal Gradle type: Lorg/gradle/api/internal/AbstractTask",
            "WARN: Method CustomTask.execute()V references internal Gradle type: Lorg/gradle/api/internal/TaskOutputsInternal",
        ]
    }
}
