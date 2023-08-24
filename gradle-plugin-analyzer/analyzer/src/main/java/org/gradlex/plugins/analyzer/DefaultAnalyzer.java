package org.gradlex.plugins.analyzer;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.types.TypeReference;
import org.gradlex.plugins.analyzer.Analysis.AnalysisContext;
import org.gradlex.plugins.analyzer.TypeRepository.TypeSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;

public class DefaultAnalyzer implements Analyzer {
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultAnalyzer.class);

    private final Reporter reporter;
    private final TypeRepository typeRepository;

    public DefaultAnalyzer(Collection<Path> classpath) throws ClassHierarchyException, IOException {
        this(classpath, (level, message) -> LOGGER.atLevel(level).log(message));
    }

    public DefaultAnalyzer(Collection<Path> classpath, Reporter reporter) throws ClassHierarchyException, IOException {
        this.reporter = reporter;
        this.typeRepository = new TypeRepository(classpath);
    }

    @Override
    public void analyze(TypeSet typeSet, Analysis analysis) {
        TypeResolverImpl typeResolver = typeRepository.getTypeResolver();
        typeRepository.getTypeSet(typeSet).forEach(type -> {
            analysis.analyzeType(type, new AnalysisContext() {
                // TODO Expose TypeResolver directly
                @Nullable
                @Override
                public TypeReference findReference(String name) {
                    return typeResolver.findReference(name);
                }

                @Nullable
                @Override
                public IClass findClass(TypeReference reference) {
                    return typeResolver.findClass(reference);
                }

                @Nullable
                @Override
                public IClass findClass(String name) {
                    return typeResolver.findClass(name);
                }

                @Override
                public void report(Level level, String message) {
                    reporter.report(level, message);
                }
            });
        });
    }
}
