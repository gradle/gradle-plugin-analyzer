package org.gradlex.plugins.analyzer;

import com.ibm.wala.classLoader.IClass;
import org.gradlex.plugins.analyzer.Analysis.AnalysisContext;

import static org.slf4j.event.Level.INFO;
import static org.slf4j.event.Level.WARN;

public class TaskDoesNotExtendDefaultTask implements TypeAnalysis {
    @Override
    public void execute(IClass type, AnalysisContext context) {
        IClass taskType = context.lookup("Lorg/gradle/api/Task");
        IClass defaultTaskType = context.lookup("Lorg/gradle/api/DefaultTask");
        IClass superType = type;
        while (superType != null) {
            if (!context.getHierarchy().implementsInterface(superType, taskType)) {
                break;
            }
            TypeOrigin origin = TypeOrigin.of(superType);
            if (origin.isGradleApi()) {
                if (!superType.equals(defaultTaskType)) {
                    context.report(WARN, "Type {} is a task but extends {} instead of DefaultTask", type.getName(), superType.getName());
                } else {
                    context.report(INFO, "Type {} is a task and it extends DefaultTask directly", type.getName());
                }
                break;
            }
            superType = superType.getSuperclass();
        }
    }
}
