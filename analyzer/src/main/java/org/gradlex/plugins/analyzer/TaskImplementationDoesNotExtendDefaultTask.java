package org.gradlex.plugins.analyzer;

import com.ibm.wala.classLoader.IClass;

import static org.slf4j.event.Level.DEBUG;
import static org.slf4j.event.Level.ERROR;
import static org.slf4j.event.Level.WARN;

/**
 * Task implementations should only extend {@code org.gradle.api.DefaultTask}, not other Gradle task types.
 */
public class TaskImplementationDoesNotExtendDefaultTask extends ExternalSubtypeAnalysis {
    protected TaskImplementationDoesNotExtendDefaultTask() {
        super("Lorg/gradle/api/Task");
    }

    @Override
    protected void analyzeType(IClass type, AnalysisContext context) {
        if (type.isInterface()) {
            context.report(DEBUG, "Type {} is an interface", type.getName());
            return;
        }
        IClass defaultTaskType = context.lookup("Lorg/gradle/api/DefaultTask");
        IClass superType = type;
        while (true) {
            if (superType == null) {
                context.report(ERROR, "Type {} was not a task type after all", type.getName());
                break;
            }
            TypeOrigin origin = TypeOrigin.of(superType);
            if (origin.isGradleApi()) {
                if (!superType.equals(defaultTaskType)) {
                    context.report(WARN, "Type {} is a task but extends {} instead of DefaultTask", type.getName(), superType.getName());
                } else {
                    context.report(DEBUG, "Type {} is a task and it extends DefaultTask directly", type.getName());
                }
                break;
            }
            superType = superType.getSuperclass();
        }
    }
}
