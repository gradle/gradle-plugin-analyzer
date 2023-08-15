package org.gradlex.plugins.analyzer;

import com.ibm.wala.classLoader.IClass;
import org.gradlex.plugins.analyzer.Analysis.AnalysisContext;

public interface TypeAnalysis {
    void execute(IClass type, AnalysisContext context);
}
