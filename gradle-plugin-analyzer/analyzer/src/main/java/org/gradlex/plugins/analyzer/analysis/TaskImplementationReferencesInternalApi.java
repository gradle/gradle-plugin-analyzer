package org.gradlex.plugins.analyzer.analysis;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.shrike.shrikeBT.IInstruction;
import org.gradlex.plugins.analyzer.ExternalSubtypeAnalysis;
import org.gradlex.plugins.analyzer.TypeOrigin;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.stream.Stream;

import static org.gradlex.plugins.analyzer.WalaUtil.instructions;
import static org.slf4j.event.Level.WARN;

/**
 * Task implementations should not reference internal Gradle APIs.
 */
public class TaskImplementationReferencesInternalApi extends ExternalSubtypeAnalysis {
    public TaskImplementationReferencesInternalApi() {
        super("Lorg/gradle/api/Task");
    }

    @Override
    protected void analyzeType(IClass type, AnalysisContext context) {
        checkHierarchy(type, context);
        type.getDeclaredMethods()
            .forEach(method -> instructions(method)
                .flatMap(TaskImplementationReferencesInternalApi::getReferencedTypes)
                .filter(TypeOrigin::isInternalGradleApi)
                .forEach(internalType -> context.report(WARN, String.format("Method %s in %s references internal Gradle type: %s",
                    method, type.getName(), internalType.getName()))));
    }

    private void checkHierarchy(IClass baseType, AnalysisContext context) {
        var queue = new ArrayDeque<IClass>();
        var seen = new HashSet<IClass>();
        queue.add(baseType);
        while (true) {
            IClass type = queue.poll();
            if (type == null) {
                break;
            }
            directSuperTypes(type)
                .forEach(superType -> {
                    switch (TypeOrigin.of(superType)) {
                        case PUBLIC:
                            // Ignore referenced public types
                            break;
                        case INTERNAL:
                            // Report referenced internal type
                            context.report(WARN, String.format("Type %s extends internal Gradle API %s", type.getName(), superType.getName()));
                            break;
                        default:
                            if (seen.add(superType)) {
                                queue.add(superType);
                            }
                            break;
                    }
                });
        }
    }

    private static Stream<IClass> directSuperTypes(IClass type) {
        return Stream.concat(
            Stream.ofNullable(type.getSuperclass()),
            type.getDirectInterfaces().stream());
    }

    public static Stream<IClass> getReferencedTypes(IInstruction instruction) {
        return Stream.of();
    }
}
