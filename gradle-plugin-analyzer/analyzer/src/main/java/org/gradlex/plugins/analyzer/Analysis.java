package org.gradlex.plugins.analyzer;

import com.ibm.wala.classLoader.IClass;
import org.slf4j.event.Level;

public interface Analysis {
    void analyzeType(IClass type, AnalysisContext context);

    interface AnalysisContext {
        TypeResolver getResolver();

        void report(Level level, String message);
    }
}
