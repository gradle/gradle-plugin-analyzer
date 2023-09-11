package org.gradlex.plugins.analyzer.analysis;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.types.TypeReference;
import org.gradlex.plugins.analyzer.Analysis;
import org.gradlex.plugins.analyzer.Reference;
import org.gradlex.plugins.analyzer.Reference.FieldDeclarationSource;
import org.gradlex.plugins.analyzer.Reference.MethodBodySource;
import org.gradlex.plugins.analyzer.Reference.MethodDeclarationSource;
import org.gradlex.plugins.analyzer.Reference.MethodInheritanceSource;
import org.gradlex.plugins.analyzer.Reference.MethodTarget;
import org.gradlex.plugins.analyzer.Reference.Source;
import org.gradlex.plugins.analyzer.Reference.Target;
import org.gradlex.plugins.analyzer.Reference.TypeDeclarationSource;
import org.gradlex.plugins.analyzer.Reference.TypeInheritanceSource;
import org.gradlex.plugins.analyzer.Reference.TypeTarget;
import org.gradlex.plugins.analyzer.Reporter.Message;
import org.gradlex.plugins.analyzer.TypeOrigin;
import org.gradlex.plugins.analyzer.TypeReferenceWalker;
import org.gradlex.plugins.analyzer.WalaUtil;

import javax.annotation.Nullable;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.slf4j.event.Level.WARN;

/**
 * Task implementations should not reference internal Gradle APIs.
 */
public class ShouldNotReferenceInternalApi implements Analysis {
    @Override
    public void analyzeType(IClass type, AnalysisContext context) {
        var references = new LinkedHashSet<Reference>();
        TypeReferenceWalker.walkReferences(type, context.getResolver(), references::add);

        references.stream()
            .map(ShouldNotReferenceInternalApi::formatMessage)
            .filter(Objects::nonNull)
            .forEach(message -> context.report(WARN, message));
    }

    @Nullable
    private static Message formatMessage(Reference reference) {
        Target refTarget = reference.target();
        Object target;
        if (refTarget instanceof TypeTarget) {
            TypeReference type = ((TypeTarget) refTarget).type();
            // Ignore references to non-internal types
            if (TypeOrigin.of(type) != TypeOrigin.INTERNAL) {
                return null;
            }
            target = type;
        } else if (refTarget instanceof MethodTarget) {
            IMethod method = ((MethodTarget) refTarget).method();

            IClass type = method.getDeclaringClass();
            // Ignore references to non-internal types
            if (TypeOrigin.of(type) != TypeOrigin.INTERNAL) {
                return null;
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
            if (hasPublicDefinition.get()) {
                return null;
            }
            target = method;
        } else {
            throw new AssertionError();
        }

        Source source = reference.source();
        if (source instanceof TypeDeclarationSource) {
            return new Message("The %s references internal Gradle %s",
                ((TypeDeclarationSource) source).type(), target);
        }
        if (source instanceof TypeInheritanceSource) {
            return new Message("The %s extends internal Gradle %s",
                ((TypeInheritanceSource) source).type(), target);
        }
        if (source instanceof FieldDeclarationSource) {
            return new Message("The %s references internal Gradle %s",
                ((FieldDeclarationSource) source).field(), target);
        }
        if (source instanceof MethodDeclarationSource) {
            return new Message("The declaration of %s references internal Gradle %s",
                ((MethodDeclarationSource) source).method(), target);
        }
        if (source instanceof MethodInheritanceSource) {
            return new Message("The %s overrides internal Gradle %s",
                ((MethodInheritanceSource) source).method(), target);
        }
        if (source instanceof MethodBodySource) {
            return new Message("The %s references internal Gradle %s",
                ((MethodBodySource) source).method(), target);
        }
        throw new AssertionError();
    }
}
