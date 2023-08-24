package org.gradlex.plugins.analyzer.analysis

import static org.gradlex.plugins.analyzer.TypeRepository.TypeSet.EXTERNAL_TASK_TYPES

class TypeShouldNotOverrideGetterTest extends AbstractAnalysisSpec {
    def "can detect type overriding getter"() {
        compileJava("""
            class CustomTask extends org.gradle.api.tasks.SourceTask {
                @Override
                public org.gradle.api.file.FileTree getSource() {
                    return super.getSource();
                }
            }
        """)

        when:
        analyze(EXTERNAL_TASK_TYPES, new TypeShouldNotOverrideGetter())

        then:
        reports == [
            "INFO: The getter getSource() in LCustomTask overrides Gradle API from Lorg/gradle/api/tasks/SourceTask, but calls only super()"
        ]
    }
}
