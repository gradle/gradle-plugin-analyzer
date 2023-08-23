package org.gradlex.plugins.analyzer.analysis

import static org.gradlex.plugins.analyzer.TypeRepository.TypeSet.EXTERNAL_TASK_TYPES

class ShouldNotReferenceInternalApiTest extends AbstractAnalysisSpec {
    def "does not signal public API references"() {
        compileJava("""
            class CustomTask extends org.gradle.api.DefaultTask {
                @org.gradle.api.tasks.TaskAction
                public void execute() {
                }
            }
        """)

        when:
        analyzer.analyze(EXTERNAL_TASK_TYPES, new ShouldNotReferenceInternalApi())

        then:
        reports == []
    }

    def "signals extending internal API"() {
        compileJava("""
            class CustomTask extends org.gradle.jvm.toolchain.internal.task.ShowToolchainsTask {
            }
        """)

        when:
        analyzer.analyze(EXTERNAL_TASK_TYPES, new ShouldNotReferenceInternalApi())

        then:
        reports == [
            "WARN: Method CustomTask.<init>()V references internal Gradle method Lorg/gradle/jvm/toolchain/internal/task/ShowToolchainsTask.<init>()V",
            "WARN: Type LCustomTask extends internal Gradle type Lorg/gradle/jvm/toolchain/internal/task/ShowToolchainsTask",
        ]
    }

    def "signals calling internal API"() {
        compileJava("""
            class CustomTask extends org.gradle.api.DefaultTask {
                static {
                    System.out.println(org.gradle.api.internal.TaskOutputsInternal.class.getName());
                }
            
                {
                    getOutputs().getUpToDateSpec();
                }
            
                @org.gradle.api.tasks.TaskAction
                public void execute() {
                    getOutputs().getUpToDateSpec();
                    org.gradle.api.internal.TaskOutputsInternal[] lajos = new org.gradle.api.internal.TaskOutputsInternal[10];
                }
            }
        """)

        when:
        analyzer.analyze(EXTERNAL_TASK_TYPES, new ShouldNotReferenceInternalApi())

        then:
        reports == [
            "WARN: Method CustomTask.<clinit>()V references internal Gradle type Lorg/gradle/api/internal/TaskOutputsInternal",
            "WARN: Method CustomTask.<init>()V references internal Gradle method Lorg/gradle/api/internal/TaskOutputsInternal.getUpToDateSpec()Lorg/gradle/api/specs/AndSpec;",
            "WARN: Method CustomTask.execute()V references internal Gradle method Lorg/gradle/api/internal/TaskOutputsInternal.getUpToDateSpec()Lorg/gradle/api/specs/AndSpec;",
            "WARN: Method CustomTask.execute()V references internal Gradle type Lorg/gradle/api/internal/TaskOutputsInternal",
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
        analyzer.analyze(EXTERNAL_TASK_TYPES, new ShouldNotReferenceInternalApi())

        then:
        reports == [
            "WARN: Method declaration CustomTask.<init>(Lorg/gradle/api/internal/BuildType;)V references internal Gradle type Lorg/gradle/api/internal/BuildType",
            "WARN: Method declaration CustomTask.internalApis(Lorg/gradle/api/internal/TaskInputsInternal;)Lorg/gradle/api/internal/TaskOutputsInternal; references internal Gradle type Lorg/gradle/api/internal/TaskInputsInternal",
            "WARN: Method declaration CustomTask.internalApis(Lorg/gradle/api/internal/TaskInputsInternal;)Lorg/gradle/api/internal/TaskOutputsInternal; references internal Gradle type Lorg/gradle/api/internal/TaskOutputsInternal",
            "WARN: Method declaration CustomTask.internalApis(Lorg/gradle/api/internal/TaskInputsInternal;)Lorg/gradle/api/internal/TaskOutputsInternal; references internal Gradle type Lorg/gradle/internal/exceptions/DefaultMultiCauseException",
        ]
    }

    def "signals internal types in field declarations"() {
        compileJava("""
            class CustomTask extends org.gradle.api.DefaultTask {
                private org.gradle.api.internal.BuildType buildType;
            }
        """)

        when:
        analyzer.analyze(EXTERNAL_TASK_TYPES, new ShouldNotReferenceInternalApi())

        then:
        reports == [
            "WARN: Field LCustomTask.buildType references internal Gradle type Lorg/gradle/api/internal/BuildType",
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
        analyzer.analyze(EXTERNAL_TASK_TYPES, new ShouldNotReferenceInternalApi())

        then:
        reports == [
            "WARN: Annotation on method CustomTask.doSomething()V references internal Gradle type Lorg/gradle/internal/instrumentation/api/annotations/InterceptInherited",
            "WARN: Annotation on type LCustomTask references internal Gradle type Lorg/gradle/api/internal/BuildType",
            "WARN: Annotation on type LCustomTask references internal Gradle type Lorg/gradle/internal/instrumentation/api/annotations/VisitForInstrumentation",
        ]
    }
}
