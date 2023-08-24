package org.gradlex.plugins.analyzer

import com.ibm.wala.classLoader.IClass
import org.gradlex.plugins.analyzer.analysis.AbstractAnalysisSpec
import org.gradlex.plugins.analyzer.analysis.ShouldNotReferenceInternalApi
import org.gradlex.plugins.analyzer.analysis.TypeShouldExtendType
import org.gradlex.plugins.analyzer.analysis.TypeShouldNotOverrideSetter

import java.nio.file.FileSystem
import java.nio.file.FileSystems
import java.nio.file.Path
import java.util.regex.Pattern
import java.util.stream.Stream

import static org.gradlex.plugins.analyzer.TypeRepository.TypeSet.ALL_EXTERNAL_REFERENCED_TYPES
import static org.slf4j.event.Level.INFO

class DefaultAnalyzerTest extends AbstractAnalysisSpec {
    def "can detect implemented types"() {
        def pluginTypes = []

        when:
        analyze(TypeRepository.TypeSet.PLUGIN_TYPES, new Analysis() {
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
        expect:
        analyze(ALL_EXTERNAL_REFERENCED_TYPES, new TypeShouldExtendType("Lorg/gradle/api/DefaultTask"))
    }

    def "can show types that override setters"() {
        expect:
        analyze(ALL_EXTERNAL_REFERENCED_TYPES, new TypeShouldNotOverrideSetter())
    }

    def "can show references to internal Gradle types"() {
        expect:
        analyze(ALL_EXTERNAL_REFERENCED_TYPES, new ShouldNotReferenceInternalApi())
    }

    @Override
    protected Analyzer getAnalyzer() {
        def pluginFiles = explode(System.getProperty("plugin-files"), FileSystems.default).toList()
        new DefaultAnalyzer(files + pluginFiles)
    }

    @Override
    protected Reporter getReporter() {
        { level, message -> println("$level: $message") }
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
