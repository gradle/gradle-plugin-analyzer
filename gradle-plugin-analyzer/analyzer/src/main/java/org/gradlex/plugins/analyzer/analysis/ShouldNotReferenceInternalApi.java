package org.gradlex.plugins.analyzer.analysis;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.types.TypeReference;
import org.gradlex.plugins.analyzer.Analysis;
import org.gradlex.plugins.analyzer.Reference;
import org.gradlex.plugins.analyzer.Reference.MethodTarget;
import org.gradlex.plugins.analyzer.Reference.Target;
import org.gradlex.plugins.analyzer.Reference.TypeTarget;
import org.gradlex.plugins.analyzer.TypeOrigin;
import org.gradlex.plugins.analyzer.TypeReferenceWalker;
import org.gradlex.plugins.analyzer.WalaUtil;

import java.util.LinkedHashSet;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.slf4j.event.Level.WARN;

/**
 * Task implementations should not reference APIs.
 */
public class ShouldNotReferenceInternalApi implements Analysis {
    @Override
    public void analyzeType(IClass type, AnalysisContext context) {
        var references = new LinkedHashSet<Reference>();
        TypeReferenceWalker.walkReferences(type, context.getResolver(), references::add);

        references.stream()
            .filter(ShouldNotReferenceInternalApi::isInternalReference)
            .map(Reference::format)
            .forEach(message -> context.report(WARN, message));
    }

    private static boolean isInternalReference(Reference reference) {
        Target refTarget = reference.target();
        if (refTarget instanceof TypeTarget) {
            TypeReference type = ((TypeTarget) refTarget).type();
            return TypeOrigin.of(type) == TypeOrigin.INTERNAL;
        } else if (refTarget instanceof MethodTarget) {
            IMethod method = ((MethodTarget) refTarget).method();

            IClass type = method.getDeclaringClass();
            // Ignore references to non-internal types
            if (TypeOrigin.of(type) != TypeOrigin.INTERNAL) {
                return false;
            }

            // Try to find method in a public supertype
            var hasPublicDefinition = new AtomicBoolean(false);
            WalaUtil.visitTypeHierarchy(type, superType -> {
                if (TypeOrigin.isPublicGradleApi(superType)) {
                    if (superType.getMethod(method.getSelector()) != null) {
                        hasPublicDefinition.set(true);
                        return false;
                    }
                }
                return true;
            });

            // Skip method if it's defined in a public API type
            return !hasPublicDefinition.get();
        } else {
            throw new AssertionError();
        }
    }
}
