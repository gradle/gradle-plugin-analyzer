package org.gradlex.plugins.analyzer.analysis


import static org.gradlex.plugins.analyzer.TypeRepository.TypeSet.EXTERNAL_TASK_TYPES

class TypeShouldExtendTypeTest extends AbstractAnalysisSpec {
    def "can detect task extending SourceTask"() {
        compileGroovy("""
            class GoodTask extends org.gradle.api.DefaultTask {}
            class BadTask extends org.gradle.api.tasks.SourceTask {}
        """)

        when:
        analyzer.analyze(EXTERNAL_TASK_TYPES, new TypeShouldExtendType("Lorg/gradle/api/DefaultTask"))

        then:
        reports == [
            "WARN: Type LBadTask should extend Lorg/gradle/api/DefaultTask instead of Lorg/gradle/api/tasks/SourceTask",
        ]
    }
}
