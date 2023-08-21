package org.gradlex.plugins.analyzer.analysis

import com.google.common.base.Predicates

class AbstractTaskImplementationDoesNotOverrideMethodTest extends AbstractAnalysisSpec {
    def analysis = new AbstractTaskImplementationDoesNotOverrideMethod("method", Predicates.alwaysTrue()) {}

    def "can detect super-only overriding method"() {
        compileJava("""
            class CustomTask extends org.gradle.api.DefaultTask {
                @Override
                public boolean getEnabled() {
                    return super.getEnabled();
                }
            }
        """)

        when:
        analyzer.analyze(analysis)

        then:
        reports == [
            "INFO: The method getEnabled() in LCustomTask overrides Gradle API from Lorg/gradle/api/DefaultTask, but calls only super()"
        ]
    }

    def "can detect overriding method with custom logic"() {
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
            "WARN: The method setEnabled() in LCustomTask overrides Gradle API from Lorg/gradle/api/DefaultTask with custom logic: Instruction #4 expected to be ReturnInstruction but found class com.ibm.wala.shrike.shrikeBT.GetInstruction\$Lazy"
        ]
    }
}
