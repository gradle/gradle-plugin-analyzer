package org.gradlex.plugins.analyzer.analysis

class TypeDoesNotOverrideSetterTest extends AbstractAnalysisSpec {
    def "can detect type overriding setter"() {
        classLoader.parseClass("""
            class BaseTask extends org.gradle.api.DefaultTask {
                void setValue(Object value) {
                }
            }
            
            class ExtendedTask extends BaseTask {
                @Override
                void setValue(Object value) {
                    System.out.println("Overriding")
                }
            }
        """)

        when:
        analyzer.analyze(new TypeDoesNotOverrideSetter())

        then:
        reports == [
        ]
    }
}
