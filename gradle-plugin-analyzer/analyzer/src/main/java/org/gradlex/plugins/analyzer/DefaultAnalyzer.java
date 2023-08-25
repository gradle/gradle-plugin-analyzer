package org.gradlex.plugins.analyzer;

import com.ibm.wala.ipa.cha.ClassHierarchyException;
import org.gradlex.plugins.analyzer.Analysis.AnalysisContext;
import org.gradlex.plugins.analyzer.TypeRepository.TypeSet;
import org.slf4j.event.Level;

import java.io.IOException;
import java.util.function.Function;
import java.util.stream.Stream;

public class DefaultAnalyzer implements Analyzer {
    private final TypeRepository typeRepository;
    private final Function<Object, String> formatter;

    public DefaultAnalyzer(TypeRepository typeRepository, Function<Object, String> formatter) throws ClassHierarchyException, IOException {
        this.typeRepository = typeRepository;
        this.formatter = formatter;
    }

    @Override
    public void analyze(TypeSet typeSet, Analysis analysis, Reporter reporter) {
        TypeResolverImpl typeResolver = typeRepository.getTypeResolver();
        typeRepository.getTypeSet(typeSet).forEach(type -> {
            analysis.analyzeType(type, new AnalysisContext() {
                @Override
                public TypeResolver getResolver() {
                    return typeResolver;
                }

                @Override
                public void report(Level level, String message, Object... args) {
                    reporter.report(level, message, Stream.of(args).map(formatter).toArray(Object[]::new));
                }
            });
        });
    }
}
