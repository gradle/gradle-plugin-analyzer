package org.gradlex.plugins.analyzer

import com.ibm.wala.classLoader.IClass
import org.gradlex.plugins.analyzer.analysis.TypeShouldExtendType
import org.gradlex.plugins.analyzer.analysis.TypeShouldNotOverrideSetter
import spock.lang.Specification

import java.nio.file.FileSystem
import java.nio.file.FileSystems
import java.nio.file.Path
import java.nio.file.Paths
import java.util.regex.Pattern
import java.util.stream.Stream

import static org.gradlex.plugins.analyzer.TypeRepository.TypeSet.ALL_EXTERNAL_REFERENCED_TYPES
import static org.slf4j.event.Level.INFO

class DefaultAnalyzerTest extends Specification {
    def "can detect implemented types"() {
        def files = []

        def pluginFiles = System.getProperty("plugin-files")
        explode(pluginFiles, FileSystems.default).forEach(files::add)

        def gradleApi = System.getProperty("gradle-api")
        files.add(Paths.get(gradleApi))

        def analyzer = new DefaultAnalyzer(files)
        def pluginTypes = []

        when:
        analyzer.analyze(TypeRepository.TypeSet.PLUGIN_TYPES, new Analysis() {
            @Override
            void analyzeType(IClass type, Analysis.AnalysisContext context) {
                context.report(INFO, "Found plugin: " + type.name)
                pluginTypes += type.name.toString()
            }
        })

        then:
        pluginTypes.size() > 0
    }

    def "can show types that extend something other than DefaultTask"() {
        def files = []

        def pluginFiles = System.getProperty("plugin-files")
        explode(pluginFiles, FileSystems.default).forEach(files::add)

        def gradleApi = System.getProperty("gradle-api")
        files.add(Paths.get(gradleApi))

        def analyzer = new DefaultAnalyzer(files)

        expect:
        analyzer.analyze(ALL_EXTERNAL_REFERENCED_TYPES, new TypeShouldExtendType("Lorg/gradle/api/DefaultTask"))
    }

    def "can show types that override setters"() {
        def files = []

        def pluginFiles = System.getProperty("plugin-files")
        explode(pluginFiles, FileSystems.default).forEach(files::add)

        def gradleApi = System.getProperty("gradle-api")
        files.add(Paths.get(gradleApi))

        def analyzer = new DefaultAnalyzer(files)

        expect:
        analyzer.analyze(ALL_EXTERNAL_REFERENCED_TYPES, new TypeShouldNotOverrideSetter())
    }

    private static Stream<Path> explode(String paths, FileSystem fileSystem) {
        // the classpath is split at every path separator which is not escaped
        String regex = "(?<!\\\\)" + Pattern.quote(File.pathSeparator);
        // we need to filter out duplicates of the same files to not generate duplicate input locations
        return Stream.of(paths.split(regex))
                .map(fileSystem::getPath)
                .map(Path::normalize)
                .distinct();
    }
}
