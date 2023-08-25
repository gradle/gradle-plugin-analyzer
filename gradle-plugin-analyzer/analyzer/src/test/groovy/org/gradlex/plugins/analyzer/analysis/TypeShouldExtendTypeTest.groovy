package org.gradlex.plugins.analyzer.analysis


import static org.gradlex.plugins.analyzer.TypeRepository.TypeSet.EXTERNAL_TASK_TYPES

class TypeShouldExtendTypeTest extends AbstractAnalysisSpec {
    def "can detect task extending SourceTask"() {
        compileGroovy("""
            class GoodTask extends org.gradle.api.DefaultTask {}
            class BadTask extends org.gradle.api.tasks.SourceTask {}
        """)

        when:
        analyze(EXTERNAL_TASK_TYPES, new TypeShouldExtendType("Lorg/gradle/api/DefaultTask"))

        then:
        reports == [
            "WARN: The type LBadTask should extend type Lorg/gradle/api/DefaultTask instead of type Lorg/gradle/api/tasks/SourceTask",
        ]
    }
}
