package org.gradlex.plugins.analyzer.analysis;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IField;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.types.TypeReference;
import org.gradlex.plugins.analyzer.Analysis;
import org.gradlex.plugins.analyzer.Reporter.Message;
import org.gradlex.plugins.analyzer.TypeOrigin;
import org.gradlex.plugins.analyzer.TypeReferenceWalker;
import org.gradlex.plugins.analyzer.TypeReferenceWalker.ReferenceVisitor;
import org.gradlex.plugins.analyzer.TypeReferenceWalker.ReferenceVisitorFactory;
import org.gradlex.plugins.analyzer.TypeResolver;
import org.gradlex.plugins.analyzer.WalaUtil;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.slf4j.event.Level.WARN;

/**
 * Task implementations should not reference internal Gradle APIs.
 */
public class ShouldNotReferenceInternalApi implements Analysis {
    @Override
    public void analyzeType(IClass type, AnalysisContext context) {
        ReferenceCollector referenceCollector = new ReferenceCollector(context);

        TypeReferenceWalker.walkReferences(type, referenceCollector);

        referenceCollector.references.forEach(reference -> context.report(WARN, reference));
    }

    private static class ReferenceCollector implements ReferenceVisitorFactory {
        private final AnalysisContext context;
        private final Set<Message> references = new HashSet<>();

        public ReferenceCollector(AnalysisContext context) {
            this.context = context;
        }

        @Override
        public ReferenceVisitor forTypeHierarchy(IClass type) {
            return new Recorder(context.getResolver()) {
                @Override
                protected Message formatReference(Object reference) {
                    return new Message("The %s extends internal Gradle %s", type, reference);
                }
            };
        }

        @Override
        public ReferenceVisitor forTypeAnnotations(IClass type) {
            return forAnnotations(type);
        }

        @Override
        public ReferenceVisitor forFieldDeclaration(IField field) {
            return new Recorder(context.getResolver()) {
                @Override
                protected Message formatReference(Object reference) {
                    return new Message("The %s references internal Gradle %s", field, reference);
                }
            };
        }

        @Override
        public ReferenceVisitor forFieldAnnotations(IField field) {
            return forAnnotations(field);
        }

        @Override
        public ReferenceVisitor forMethodDeclaration(IMethod originMethod) {
            return new Recorder(context.getResolver()) {
                @Override
                protected Message formatReference(Object reference) {
                    return new Message("The declaration of %s references internal Gradle %s", originMethod, reference);
                }
            };
        }

        @Override
        public ReferenceVisitor forMethodInheritance(IMethod originMethod) {
            return new Recorder(context.getResolver()) {
                @Override
                protected Message formatReference(Object reference) {
                    return new Message("The %s overrides internal Gradle %s", originMethod, reference);
                }
            };
        }

        @Override
        public ReferenceVisitor forMethodBody(IMethod originMethod) {
            return new Recorder(context.getResolver()) {
                @Override
                protected Message formatReference(Object reference) {
                    return new Message("The %s references internal Gradle %s", originMethod, reference);
                }
            };
        }

        @Override
        public ReferenceVisitor forMethodAnnotations(IMethod method) {
            return forAnnotations(method);
        }

        private ReferenceVisitor forAnnotations(Object subject) {
            return new Recorder(context.getResolver()) {
                @Override
                protected Message formatReference(Object reference) {
                    return new Message("Annotation on %s references internal Gradle %s", subject, reference);
                }
            };
        }

        public abstract class Recorder extends ReferenceVisitor {
            public Recorder(TypeResolver typeResolver) {
                super(typeResolver);
            }

            @Override
            public void visitReference(TypeReference reference) {
                if (TypeOrigin.of(reference) == TypeOrigin.INTERNAL) {
                    references.add(formatReference(reference));
                }
            }

            @Override
            public void visitMethodReference(IMethod method) {
                IClass type = method.getDeclaringClass();
                if (TypeOrigin.of(type) == TypeOrigin.INTERNAL) {
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
                    if (!hasPublicDefinition.get()) {
                        references.add(formatReference(method));
                    }
                }
            }

            protected abstract Message formatReference(Object reference);
        }
    }
}
