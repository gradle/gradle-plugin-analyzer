package org.gradlex.plugins.analyzer;

import com.google.common.collect.Streams;
import com.ibm.wala.classLoader.IClass;

import static org.gradlex.plugins.analyzer.TypeOrigin.EXTERNAL;

public abstract class AllExternalTypeAnalysis implements Analysis {
    @Override
    public void execute(AnalysisContext context) {
        Streams.stream(context.getHierarchy())
            .filter(clazz -> TypeOrigin.of(clazz) == EXTERNAL)
            .forEach(type -> analyzeType(type, context));
    }

    protected abstract void analyzeType(IClass type, AnalysisContext context);
}
