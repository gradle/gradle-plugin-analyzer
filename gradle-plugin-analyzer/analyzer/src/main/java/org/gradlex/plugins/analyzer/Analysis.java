package org.gradlex.plugins.analyzer;

import com.ibm.wala.classLoader.IClass;

public interface Analysis {
    void analyzeType(IClass type, AnalysisContext context);

    interface AnalysisContext extends Reporter {
        TypeResolver getResolver();
    }
}
