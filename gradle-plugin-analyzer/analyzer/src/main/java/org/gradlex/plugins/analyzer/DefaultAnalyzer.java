package org.gradlex.plugins.analyzer;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.core.util.config.AnalysisScopeReader;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ipa.cha.ClassHierarchyFactory;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.config.FileOfClasses;
import org.gradlex.plugins.analyzer.Analysis.AnalysisContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Collection;
import java.util.stream.Collectors;

public class DefaultAnalyzer implements Analyzer {
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultAnalyzer.class);

    private final AnalysisScope scope;
    private final ClassHierarchy hierarchy;

    public DefaultAnalyzer(Collection<Path> classpath) throws ClassHierarchyException, IOException {
        this.scope = AnalysisScopeReader.instance.makeJavaBinaryAnalysisScope(toClasspath(classpath), null);
        scope.setExclusions(new FileOfClasses(new ByteArrayInputStream(EXCLUSIONS.getBytes(StandardCharsets.UTF_8))));

        this.hierarchy = ClassHierarchyFactory.make(scope);
    }

    private static String toClasspath(Collection<Path> classpath) {
        return classpath.stream()
            .map(Path::normalize)
            .distinct()
            .map(Path::toString)
            .collect(Collectors.joining(File.pathSeparator));
    }

    @Override
    public void analyze(Analysis analysis) {
        analysis.execute(new AnalysisContext() {
            @Override
            public ClassHierarchy getHierarchy() {
                return hierarchy;
            }

            @Override
            public TypeReference reference(String name) {
                TypeReference result = TypeReference.find(scope.getApplicationLoader(), name);
                if (result == null) {
                    throw new IllegalStateException("Missing type: " + name);
                }
                return result;
            }

            @Override
            public IClass lookup(String name) {
                return hierarchy.lookupClass(reference(name));
            }

            @Override
            public void report(Level level, String message, Object... args) {
                LOGGER.atLevel(level).log(message, args);
            }
        });
    }

    private static final String EXCLUSIONS = """
        java\\/awt\\/.*
        javax\\/swing\\/.*
        sun\\/awt\\/.*
        sun\\/swing\\/.*
        com\\/sun\\/.*
        sun\\/.*
        org\\/netbeans\\/.*
        org\\/openide\\/.*
        com\\/ibm\\/crypto\\/.*
        com\\/ibm\\/security\\/.*
        org\\/apache\\/xerces\\/.*
        java\\/security\\/.*
        """;

    @Override
    public String toString() {
        // To avoid printing the whole classpath in Spock error messages
        return "Analyzer";
    }

}
