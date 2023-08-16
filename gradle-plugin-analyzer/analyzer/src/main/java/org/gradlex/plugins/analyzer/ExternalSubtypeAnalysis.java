package org.gradlex.plugins.analyzer;

import com.ibm.wala.classLoader.IClass;

public abstract class ExternalSubtypeAnalysis implements Analysis {
    private final String typeName;

    protected ExternalSubtypeAnalysis(String typeName) {
        this.typeName = typeName;
    }

    @Override
    public void execute(AnalysisContext context) {
        context.getHierarchy().getImplementors(context.reference(typeName)).stream()
            .filter(TypeOrigin::isExternal)
            .forEach(type -> analyzeType(type, context));
    }

    protected abstract void analyzeType(IClass type, AnalysisContext context);
}
