package org.gradlex.plugins.analyzer.analysis;

import com.ibm.wala.classLoader.IClass;
import org.gradlex.plugins.analyzer.ExternalSubtypeAnalysis;

import java.util.regex.Pattern;

/**
 * Task implementations should only extend {@code org.gradle.api.DefaultTask}, not other Gradle task types.
 */
public class TypeDoesNotOverrideSetter extends ExternalSubtypeAnalysis {
    private static final Pattern SETTER = Pattern.compile("set[A-Z].*");

    public TypeDoesNotOverrideSetter() {
        super("Lorg/gradle/api/Task");
    }

    @Override
    protected void analyzeType(IClass type, AnalysisContext context) {
        type.getDeclaredMethods().forEach(method -> {
            if (method.isStatic()) {
                return;
            }
            if (!method.isPublic()) {
                return;
            }
            if (!SETTER.matcher(method.getName().toString()).matches()) {
                return;
            }
            System.out.println(" - " + method);
        });
    }
}
