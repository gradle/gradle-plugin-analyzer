package org.gradlex.plugins.analyzer;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.types.TypeReference;
import org.slf4j.event.Level;

public interface Analysis {
    void execute(AnalysisContext context);

    interface AnalysisContext {
        ClassHierarchy getHierarchy();

        TypeReference reference(String name);

        IClass lookup(String name);

        void report(Level level, String message, Object... args);
    }
}
