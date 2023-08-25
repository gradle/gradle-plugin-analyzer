package org.gradlex.plugins.analyzer.analysis

import static org.gradlex.plugins.analyzer.TypeRepository.TypeSet.EXTERNAL_TASK_TYPES

class TypeShouldNotOverrideSetterTest extends AbstractAnalysisSpec {
    def "can detect type overriding setter"() {
        compileJava("""
            class CustomTask extends org.gradle.api.DefaultTask {
                @Override
                public void setEnabled(boolean enabled) {
                    super.setEnabled(enabled);
                }
            }
        """)

        when:
        analyze(EXTERNAL_TASK_TYPES, new TypeShouldNotOverrideSetter())

        then:
        reports == [
            "INFO: The setter method CustomTask.setEnabled(Z)V overrides Gradle API from type Lorg/gradle/api/DefaultTask, but calls only super()",
        ]
    }
}
