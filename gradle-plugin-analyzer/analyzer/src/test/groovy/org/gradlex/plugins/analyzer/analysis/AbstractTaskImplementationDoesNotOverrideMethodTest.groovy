package org.gradlex.plugins.analyzer.analysis

import com.ibm.wala.classLoader.IMethod

class AbstractTaskImplementationDoesNotOverrideMethodTest extends AbstractAnalysisSpec {
    def analysis = new AbstractTaskImplementationDoesNotOverrideMethod("method", { IMethod method -> !method.static && !method.init }) {}

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
        analyzer.analyze(analysis)

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
        analyzer.analyze(analysis)

        then:
        reports == [
            ": The method setEnabled() in LCustomTask overrides Gradle API from Lorg/gradle/api/DefaultTask with custom logic: Instruction #5 expected to be ReturnInstruction but it was Get(Ljava/io/PrintStream;,STATIC,Ljava/lang/System;,out)"
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
        analyzer.analyze(analysis)

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
        analyzer.analyze(analysis)

        then:
        reports == [
            "WARN: The method setEnabled() in LCustomTask overrides Gradle API from Lorg/gradle/api/DefaultTask with custom logic: Instruction #5 expected to be ReturnInstruction but it was Constant(L;,null)"
        ]
    }

    def "can detect super-only overriding method in dynamic Groovy code"() {
        compileGroovy("""
            class CustomTask extends org.gradle.api.tasks.SourceTask {
                @Override
                boolean getEnabled() {
                    return super.getEnabled()
                }
//
//                @Override
//                org.gradle.api.file.FileTree getSource() {
//                    return super.getSource()
//                }
            }
        """)

        when:
        analyzer.analyze(analysis)

        then:
        reports == [
            "INFO: The method getEnabled() in LCustomTask overrides Gradle API from Lorg/gradle/api/DefaultTask, but calls only super()",
            "INFO: The method getSource() in LCustomTask overrides Gradle API from Lorg/gradle/api/tasks/SourceTask, but calls only super()",
        ]
    }
}
