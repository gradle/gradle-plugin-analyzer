package org.gradlex.plugins.analyzer

import org.gradlex.plugins.analyzer.TypeRepository.TypeSet
import org.gradlex.plugins.analyzer.analysis.AbstractAnalysisSpec

import static org.gradlex.plugins.analyzer.TypeRepository.TypeSet.*

class TypeRepositoryTest extends AbstractAnalysisSpec {
    def "types are properly recognized"() {
        compileJava """
            abstract class CustomTask extends org.gradle.api.DefaultTask {
            }
            
            abstract class CustomPlugin implements org.gradle.api.Plugin {
            }
        """

        def repository = new TypeRepository(files)

        expect:
        typeNames(repository, TASK_TYPES).contains("LCustomTask")
        typeNames(repository, TASK_TYPES).contains("Lorg/gradle/api/DefaultTask")
        typeNames(repository, EXTERNAL_TASK_TYPES) == ["LCustomTask"]

        typeNames(repository, PLUGIN_TYPES).contains("LCustomPlugin")
        typeNames(repository, PLUGIN_TYPES).contains("Lorg/gradle/api/plugins/JavaPlugin")
        typeNames(repository, EXTERNAL_PLUGIN_TYPES) == ["LCustomPlugin"]

        typeNames(repository, ALL_EXTERNAL_TYPES) == ["LCustomPlugin", "LCustomTask"]

        typeNames(repository, ALL_EXTERNAL_REFERENCED_TYPES) == ["LCustomPlugin", "LCustomTask"]
    }

    private static List<String> typeNames(TypeRepository repository, TypeSet set) {
        repository.getTypeSet(set)*.name*.toString().sort()
    }
}
