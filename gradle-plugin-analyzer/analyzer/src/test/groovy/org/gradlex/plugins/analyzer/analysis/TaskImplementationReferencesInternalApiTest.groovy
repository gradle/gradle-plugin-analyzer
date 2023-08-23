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
            "WARN: Type LCustomTask extends internal Gradle type: Lorg/gradle/jvm/toolchain/internal/task/ShowToolchainsTask",
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

    def "signals internal API references in method declarations"() {
        compileJava("""
            class CustomTask extends org.gradle.api.DefaultTask {
                CustomTask(org.gradle.api.internal.BuildType buildType) {}
                            
                protected org.gradle.api.internal.TaskOutputsInternal internalApis(org.gradle.api.internal.TaskInputsInternal inputs) throws org.gradle.internal.exceptions.DefaultMultiCauseException {
                    return null;
                } 
            }
        """)

        when:
        analyzer.analyze(new TaskImplementationReferencesInternalApi())

        then:
        reports == [
            "WARN: Method declaration CustomTask.<init>(Lorg/gradle/api/internal/BuildType;)V references internal Gradle type: Lorg/gradle/api/internal/BuildType",
            "WARN: Method declaration CustomTask.internalApis(Lorg/gradle/api/internal/TaskInputsInternal;)Lorg/gradle/api/internal/TaskOutputsInternal; references internal Gradle type: Lorg/gradle/api/internal/TaskInputsInternal",
            "WARN: Method declaration CustomTask.internalApis(Lorg/gradle/api/internal/TaskInputsInternal;)Lorg/gradle/api/internal/TaskOutputsInternal; references internal Gradle type: Lorg/gradle/api/internal/TaskOutputsInternal",
            "WARN: Method declaration CustomTask.internalApis(Lorg/gradle/api/internal/TaskInputsInternal;)Lorg/gradle/api/internal/TaskOutputsInternal; references internal Gradle type: Lorg/gradle/internal/exceptions/DefaultMultiCauseException",
        ]
    }

    def "signals use of internal types in annotations"() {
        // org.gradle.internal.instrumentation.api.annotations.InterceptInherited
        compileJava("""
            @org.gradle.internal.instrumentation.api.annotations.VisitForInstrumentation({org.gradle.api.internal.BuildType.class})
            class CustomTask extends org.gradle.api.DefaultTask {
                @org.gradle.internal.instrumentation.api.annotations.InterceptInherited
                void doSomething() {}
            }
        """)

        when:
        analyzer.analyze(new TaskImplementationReferencesInternalApi())

        then:
        reports == [
            "WARN: Annotation on method CustomTask.doSomething()V references internal Gradle APIs: Lorg/gradle/internal/instrumentation/api/annotations/InterceptInherited",
            "WARN: Annotation on type LCustomTask references internal Gradle APIs: Lorg/gradle/api/internal/BuildType",
            "WARN: Annotation on type LCustomTask references internal Gradle APIs: Lorg/gradle/internal/instrumentation/api/annotations/VisitForInstrumentation",
        ]
    }
}
