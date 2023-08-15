package org.gradlex.plugins.analyzer

import spock.lang.Specification

import java.nio.file.FileSystem
import java.nio.file.FileSystems
import java.nio.file.Path
import java.nio.file.Paths
import java.util.regex.Pattern
import java.util.stream.Stream

import static org.gradlex.plugins.analyzer.Analysis.Severity.INFO
import static org.gradlex.plugins.analyzer.TypeSelector.externalSubtypesOf

class DefaultAnalyzerTest extends Specification {
    def "can instantiate analyzer"() {
        def files = []

        def pluginFiles = System.getProperty("plugin-files")
        explode(pluginFiles, FileSystems.default).forEach(files::add)

        def gradleApi = System.getProperty("gradle-api")
        files.add(Paths.get(gradleApi))

        def analyzer = new DefaultAnalyzer(files)

        expect:
        analyzer.analyze(externalSubtypesOf("Lorg/gradle/api/Task", (type, context) -> {
            context.report(INFO, "Type {} implements Task", type.getName())
        }))
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
