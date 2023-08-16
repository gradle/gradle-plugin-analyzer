package org.gradlex.plugins.analyzer.analysis;

import com.ibm.wala.classLoader.IClass;
import org.gradlex.plugins.analyzer.ExternalSubtypeAnalysis;
import org.gradlex.plugins.analyzer.TypeOrigin;

import java.util.Objects;
import java.util.stream.Stream;

import static org.slf4j.event.Level.DEBUG;
import static org.slf4j.event.Level.ERROR;
import static org.slf4j.event.Level.WARN;

/**
 * Task implementations should only extend {@code org.gradle.api.DefaultTask}, not other Gradle task types.
 */
public class TaskImplementationDoesNotExtendDefaultTask extends ExternalSubtypeAnalysis {
    public TaskImplementationDoesNotExtendDefaultTask() {
        super("Lorg/gradle/api/Task");
    }

    @Override
    protected void analyzeType(IClass type, AnalysisContext context) {
        if (type.isInterface()) {
            context.report(DEBUG, String.format("Type %s is an interface", type.getName()));
            return;
        }
        IClass defaultTaskType = context.lookup("Lorg/gradle/api/DefaultTask");
        // Walk type hierarchy
        Stream.iterate(type, Objects::nonNull, IClass::getSuperclass)
            // Look for the most immediate superclass from Gradle API
            .filter(TypeOrigin::isGradleApi)
            .findFirst()
            .ifPresentOrElse(
                superType -> {
                    if (!superType.equals(defaultTaskType)) {
                        context.report(WARN, String.format("Type %s is a task but extends %s instead of DefaultTask", type.getName(), superType.getName()));
                    } else {
                        context.report(DEBUG, String.format("Type %s is a task and it extends DefaultTask directly", type.getName()));
                    }
                },
                () -> context.report(ERROR, String.format("Type %s was not a task type after all", type.getName()))
            );
    }
}
