package org.gradlex.plugins.analyzer;

import com.ibm.wala.ipa.cha.ClassHierarchyException;
import org.gradlex.plugins.analyzer.Analysis.AnalysisContext;
import org.gradlex.plugins.analyzer.TypeRepository.TypeSet;
import org.slf4j.event.Level;

import java.io.IOException;

public class DefaultAnalyzer implements Analyzer {
    private final TypeRepository typeRepository;

    public DefaultAnalyzer(TypeRepository typeRepository) throws ClassHierarchyException, IOException {
        this.typeRepository = typeRepository;
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
                public void report(Level level, String message) {
                    reporter.report(level, message);
                }
            });
        });
    }
}
