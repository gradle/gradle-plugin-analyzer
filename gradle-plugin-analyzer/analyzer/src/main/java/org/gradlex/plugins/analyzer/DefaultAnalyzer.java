package org.gradlex.plugins.analyzer;

import com.ibm.wala.classLoader.BinaryDirectoryTreeModule;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.core.util.config.AnalysisScopeReader;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ipa.cha.ClassHierarchyFactory;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.TypeName;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.config.FileOfClasses;
import org.gradlex.plugins.analyzer.Analysis.AnalysisContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;

import javax.annotation.Nonnull;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.jar.JarFile;

public class DefaultAnalyzer implements Analyzer {
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultAnalyzer.class);

    private final AnalysisScope scope;
    private final Reporter reporter;
    private final ClassHierarchy hierarchy;

    public DefaultAnalyzer(Collection<Path> classpath) throws ClassHierarchyException, IOException {
        this(classpath, (level, message) -> LOGGER.atLevel(level).log(message));
    }

    public DefaultAnalyzer(Collection<Path> classpath, Reporter reporter) throws ClassHierarchyException, IOException {
        this.reporter = reporter;
        this.scope = createScope(classpath);
        this.hierarchy = ClassHierarchyFactory.make(createScope(classpath));
    }

    @Nonnull
    private static AnalysisScope createScope(Collection<Path> classpath) throws IOException {
        AnalysisScope scope = AnalysisScopeReader.instance.makePrimordialScope(null);
        classpath.forEach(path -> addToScope(scope, path));
        scope.setExclusions(new FileOfClasses(new ByteArrayInputStream(EXCLUSIONS.getBytes(StandardCharsets.UTF_8))));
        return scope;
    }

    private static void addToScope(AnalysisScope scope, Path path) {
        ClassLoaderReference loader = scope.getLoader(AnalysisScope.APPLICATION);
        if (Files.isRegularFile(path)) {
            try {
                JarFile jar = new JarFile(path.toFile(), false);
                scope.addToScope(loader, jar);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        } else {
            scope.addToScope(loader, new BinaryDirectoryTreeModule(path.toFile()));
        }
    }

    @Override
    public void analyze(Analysis analysis) {
        analysis.execute(new AnalysisContext() {
            @Override
            public ClassHierarchy getHierarchy() {
                return hierarchy;
            }

            @Override
            public TypeReference findReference(String name) {
                var normalizedTypeName = normalizeTypeName(name);
                var reference = TypeReference.find(scope.getApplicationLoader(), normalizedTypeName);
                if (reference == null) {
                    return null;
                }
                // Unpack array types
                while (reference.isArrayType()) {
                    reference = reference.getArrayElementType();
                }
                return reference;
            }

            private static TypeName normalizeTypeName(String typeName) {
                var normalizedName = typeName.endsWith(";")
                    ? typeName.substring(0, typeName.length() - 1)
                    : typeName;
                return TypeName.findOrCreate(normalizedName);
            }

            @Override
            public IClass findClass(String name) {
                TypeReference reference = findReference(name);
                if (reference == null) {
                    return null;
                }
                if (reference.isPrimitiveType()) {
                    return null;
                }

                return hierarchy.lookupClass(reference);
            }

            @Override
            public void report(Level level, String message) {
                reporter.report(level, message);
            }
        });
    }

    private static final String EXCLUSIONS =
        "java\\/awt\\/.*\n" +
        "javax\\/swing\\/.*\n" +
        "sun\\/awt\\/.*\n" +
        "sun\\/swing\\/.*\n" +
        "com\\/sun\\/.*\n" +
        "sun\\/.*\n" +
        "org\\/netbeans\\/.*\n" +
        "org\\/openide\\/.*\n" +
        "com\\/ibm\\/crypto\\/.*\n" +
        "com\\/ibm\\/security\\/.*\n" +
        "org\\/apache\\/xerces\\/.*\n" +
        "java\\/security\\/.*\n";

    @Override
    public String toString() {
        // To avoid printing the whole classpath in Spock error messages
        return "Analyzer";
    }

}
