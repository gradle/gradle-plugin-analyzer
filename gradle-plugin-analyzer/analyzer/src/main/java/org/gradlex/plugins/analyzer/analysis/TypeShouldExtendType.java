package org.gradlex.plugins.analyzer.analysis;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.types.TypeReference;
import org.gradlex.plugins.analyzer.Analysis;
import org.gradlex.plugins.analyzer.TypeOrigin;

import java.util.Objects;
import java.util.stream.Stream;

import static org.slf4j.event.Level.DEBUG;
import static org.slf4j.event.Level.WARN;

/**
 * Task implementations should only extend {@code org.gradle.api.DefaultTask}, not other Gradle task types.
 */
public class TypeShouldExtendType implements Analysis {
    private final String expectedSuperType;

    public TypeShouldExtendType(String expectedSuperType) {
        this.expectedSuperType = expectedSuperType;
    }

    @Override
    public void analyzeType(IClass type, AnalysisContext context) {
        if (type.isInterface()) {
            context.report(DEBUG, String.format("Type %s is an interface", type.getName()));
            return;
        }
        TypeReference expected = context.getReference(expectedSuperType);
        // Walk type hierarchy
        Stream.iterate(type, Objects::nonNull, IClass::getSuperclass)
            // Look for the most immediate superclass from Gradle API
            .filter(TypeOrigin::isGradleApi)
            .findFirst()
            .ifPresent(superType -> {
                    if (!superType.getReference().equals(expected)) {
                        context.report(WARN, String.format("Type %s should extend %s instead of %s", type.getName(), expected.getName(), superType.getName()));
                    }
                }
            );
    }
}
