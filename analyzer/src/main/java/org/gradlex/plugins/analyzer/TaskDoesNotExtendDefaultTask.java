package org.gradlex.plugins.analyzer;

import com.ibm.wala.classLoader.IClass;
import org.gradlex.plugins.analyzer.Analysis.AnalysisContext;

import static org.slf4j.event.Level.DEBUG;
import static org.slf4j.event.Level.ERROR;
import static org.slf4j.event.Level.WARN;

/**
 * Tasks should only extend {@code org.gradle.api.DefaultTask}.
 */
public class TaskDoesNotExtendDefaultTask implements TypeAnalysis {
    @Override
    public void execute(IClass type, AnalysisContext context) {
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
