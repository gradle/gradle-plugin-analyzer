package org.gradlex.plugins.analyzer.analysis

import com.ibm.wala.classLoader.IMethod

import static org.gradlex.plugins.analyzer.TypeRepository.TypeSet.EXTERNAL_TASK_TYPES

class AbstractMethodOverrideAnalysisTest extends AbstractAnalysisSpec {
    def analysis = new AbstractMethodOverrideAnalysis("method", { IMethod method -> !method.static && !method.init }) {}

    def "can detect super-only overriding method in plain Java code"() {
        compileJava("""
            class CustomTask extends org.gradle.api.tasks.SourceTask {
                @Override
                public boolean getEnabled() {
                    return super.getEnabled();
                }

                @Override
                public org.gradle.api.file.FileTree getSource() {
                    return super.getSource();
                }
            }
        """)

        when:
        analyze(EXTERNAL_TASK_TYPES, analysis)

        then:
        reports == [
            "INFO: The method getEnabled() in LCustomTask overrides Gradle API from Lorg/gradle/api/DefaultTask, but calls only super()",
            "INFO: The method getSource() in LCustomTask overrides Gradle API from Lorg/gradle/api/tasks/SourceTask, but calls only super()",
        ]
    }

    def "can detect overriding method with custom logic in plain Java code"() {
        compileJava("""
            class CustomTask extends org.gradle.api.DefaultTask {               
                @Override
                public void setEnabled(boolean value) {
                    super.setEnabled(value);
                    System.out.println("Additional logic");
                }
            }
        """)

        when:
        analyze(EXTERNAL_TASK_TYPES, analysis)

        then:
        reports == [
            "WARN: The method setEnabled() in LCustomTask overrides Gradle API from Lorg/gradle/api/DefaultTask with custom logic: Instruction #4 expected to be ReturnInstruction but it was Get(Ljava/io/PrintStream;,STATIC,Ljava/lang/System;,out)"
        ]
    }

    def "can detect super-only overriding method in static Groovy code"() {
        compileGroovy("""
            @groovy.transform.CompileStatic
            class CustomTask extends org.gradle.api.tasks.SourceTask {
                @Override
                boolean getEnabled() {
                    return super.getEnabled()
                }

                @Override
                org.gradle.api.file.FileTree getSource() {
                    return super.getSource()
                }
            }
        """)

        when:
        analyze(EXTERNAL_TASK_TYPES, analysis)

        then:
        reports == [
            "INFO: The method getEnabled() in LCustomTask overrides Gradle API from Lorg/gradle/api/DefaultTask, but calls only super()",
            "INFO: The method getSource() in LCustomTask overrides Gradle API from Lorg/gradle/api/tasks/SourceTask, but calls only super()",
        ]
    }

    def "can detect overriding method with custom logic in static Groovy code"() {
        compileGroovy("""
            @groovy.transform.CompileStatic
            class CustomTask extends org.gradle.api.DefaultTask {               
                @Override
                void setEnabled(boolean value) {
                    super.setEnabled(value)
                    println "Additional logic"
                }
            }
        """)

        when:
        analyze(EXTERNAL_TASK_TYPES, analysis)

        then:
        reports == [
            "WARN: The method setEnabled() in LCustomTask overrides Gradle API from Lorg/gradle/api/DefaultTask with custom logic: Instruction #4 expected to be ReturnInstruction but it was Constant(L;,null)"
        ]
    }

    def "can detect overriding method in dynamic Groovy code"() {
        compileGroovy("""
            class CustomTask extends org.gradle.api.tasks.SourceTask {
                @Override
                boolean getEnabled() {
                    return super.getEnabled()
                }

                @Override
                void setEnabled(boolean value) {
                    super.setEnabled(value)
                }

                @Override
                org.gradle.api.file.FileTree getSource() {
                    return super.getSource()
                }
            }
        """)

        when:
        analyze(EXTERNAL_TASK_TYPES, analysis)

        then:
        reports == [
            "WARN: The dynamic Groovy method getEnabled() in LCustomTask overrides Gradle API from Lorg/gradle/api/DefaultTask",
            "WARN: The dynamic Groovy method getSource() in LCustomTask overrides Gradle API from Lorg/gradle/api/tasks/SourceTask",
            "WARN: The dynamic Groovy method setEnabled() in LCustomTask overrides Gradle API from Lorg/gradle/api/DefaultTask",
        ]
    }

    def "can detect super-only overriding method in Kotlin code"() {
        compileKotlin("""
            class CustomTask : org.gradle.api.tasks.SourceTask() {
                override fun getEnabled() = super.getEnabled()

                override fun getSource() = super.getSource()
            }
        """)

        when:
        analyze(EXTERNAL_TASK_TYPES, analysis)

        then:
        reports == [
            "INFO: The method getEnabled() in LCustomTask overrides Gradle API from Lorg/gradle/api/DefaultTask, but calls only super()",
            "INFO: The method getSource() in LCustomTask overrides Gradle API from Lorg/gradle/api/tasks/SourceTask, but calls only super()",
        ]
    }

    def "can detect overriding method with custom logic in Kotlin code"() {
        compileKotlin("""
            class CustomTask : org.gradle.api.DefaultTask() {
                override fun setEnabled(value: Boolean): Unit {
                    super.setEnabled(value)
                    println("Additional logic")
                }
            }
        """)

        when:
        analyze(EXTERNAL_TASK_TYPES, analysis)

        then:
        reports == [
            "WARN: The method setEnabled() in LCustomTask overrides Gradle API from Lorg/gradle/api/DefaultTask with custom logic: Instruction #4 expected to be ReturnInstruction but it was Constant(Ljava/lang/String;,\"Additional logic\")"
        ]
    }

    def "can detect overriding method with additional null check on the return value in Kotlin code"() {
        compileKotlin("""
            class CustomTask : org.gradle.api.tasks.SourceTask() {
                // Forcing the return type from FileTree? to FileTree
                // causes an additional null check to be added in the byte code
                override fun getSource(): org.gradle.api.file.FileTree = super.getSource()
            }
        """)

        when:
        analyze(EXTERNAL_TASK_TYPES, analysis)

        then:
        reports == [
            "WARN: The method getSource() in LCustomTask overrides Gradle API from Lorg/gradle/api/tasks/SourceTask with custom logic: Instruction #3 expected to be ReturnInstruction but it was Dup(1,0)"
        ]
    }
}
