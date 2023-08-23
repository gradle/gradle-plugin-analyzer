package org.gradlex.plugins.analyzer;

import org.gradlex.plugins.analyzer.TypeRepository.TypeSet;

public interface Analyzer {
    void analyze(TypeSet typeSet, Analysis analysis);
}
