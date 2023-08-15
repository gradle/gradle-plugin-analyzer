package org.gradlex.plugins.analyzer;

import com.google.common.collect.Streams;

import static org.gradlex.plugins.analyzer.TypeOrigin.EXTERNAL;

public class TypeSelector {
    public static Analysis externalSubtypesOf(String typeName, TypeAnalysis analysis) {
        return new Analysis() {
            @Override
            public void execute(AnalysisContext context) {
                context.getHierarchy().getImplementors(context.typeReference(typeName)).stream()
                    .filter(clazz -> TypeOrigin.of(clazz) == EXTERNAL)
                    .forEach(type -> analysis.execute(type, context));
            }
        };
    }

    public static Analysis allExternalTypes(TypeAnalysis analysis) {
        return new Analysis() {
            @Override
            public void execute(AnalysisContext context) {
                Streams.stream(context.getHierarchy())
                    .filter(clazz -> TypeOrigin.of(clazz) == EXTERNAL)
                    .forEach(type -> analysis.execute(type, context));
            }
        };
    }
}
