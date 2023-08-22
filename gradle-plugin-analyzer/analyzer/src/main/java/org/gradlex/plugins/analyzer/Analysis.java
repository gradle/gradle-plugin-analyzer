package org.gradlex.plugins.analyzer;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.types.TypeReference;
import org.slf4j.event.Level;

import javax.annotation.Nullable;
import java.util.Objects;

public interface Analysis {
    void execute(AnalysisContext context);

    interface AnalysisContext {
        ClassHierarchy getHierarchy();

        @Nullable
        TypeReference findReference(String name);

        /**
         * Returns {@code null} for primitives and `V`.
         */
        @Nullable
        IClass findClass(String name);

        default IClass getClass(String name) {
            return Objects.requireNonNull(findClass(name));
        }

        void report(Level level, String message);
    }
}
