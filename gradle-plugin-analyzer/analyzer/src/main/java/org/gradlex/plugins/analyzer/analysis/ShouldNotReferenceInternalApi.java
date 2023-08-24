package org.gradlex.plugins.analyzer.analysis;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IField;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.types.TypeReference;
import org.gradlex.plugins.analyzer.Analysis;
import org.gradlex.plugins.analyzer.TypeOrigin;
import org.gradlex.plugins.analyzer.TypeResolver;
import org.gradlex.plugins.analyzer.analysis.TypeReferenceWalker.ReferenceVisitor;
import org.gradlex.plugins.analyzer.analysis.TypeReferenceWalker.ReferenceVisitorFactory;

import java.util.SortedSet;
import java.util.TreeSet;

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
        private final SortedSet<String> references = new TreeSet<>();

        public ReferenceCollector(AnalysisContext context) {
            this.context = context;
        }

        @Override
        public ReferenceVisitor forTypeHierarchy(IClass type) {
            return new Recorder(context.getResolver()) {
                @Override
                protected String formatReference(String reference) {
                    return "Type %s extends %s".formatted(type.getName(), reference);
                }
            };
        }

        @Override
        public ReferenceVisitor forTypeAnnotations(IClass type) {
            return forAnnotations("type " + type.getName());
        }

        @Override
        public ReferenceVisitor forFieldDeclaration(IField field) {
            return new Recorder(context.getResolver()) {
                @Override
                protected String formatReference(String reference) {
                    return "Field %s references %s".formatted(field.getName(), reference);
                }
            };
        }

        @Override
        public ReferenceVisitor forFieldAnnotations(IField field) {
            return forAnnotations("field " + field.getName());
        }

        @Override
        public ReferenceVisitor forMethodDeclaration(IMethod originMethod) {
            return new Recorder(context.getResolver()) {
                @Override
                protected String formatReference(String reference) {
                    return "Method declaration %s references %s".formatted(originMethod.getSignature(), reference);
                }
            };
        }

        @Override
        public ReferenceVisitor forMethodBody(IMethod originMethod) {
            return new Recorder(context.getResolver()) {
                @Override
                protected String formatReference(String reference) {
                    return "Method %s references %s".formatted(originMethod.getSignature(), reference);
                }
            };
        }

        @Override
        public ReferenceVisitor forMethodAnnotations(IMethod method) {
            return forAnnotations("method " + method.getSignature());
        }

        private ReferenceVisitor forAnnotations(String subject) {
            return new Recorder(context.getResolver()) {
                @Override
                protected String formatReference(String reference) {
                    return "Annotation on %s references %s".formatted(subject, reference);
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
                    references.add(formatTypeReference(reference));
                }
            }

            @Override
            public void visitMethodReference(String typeName, String methodName, String methodSignature) {
                TypeReference reference = context.getResolver().findReference(typeName);
                if (reference != null && TypeOrigin.of(reference) == TypeOrigin.INTERNAL) {
                    references.add(formatMethodReference(reference, methodName, methodSignature));
                }
            }

            private String formatTypeReference(TypeReference reference) {
                return formatReference("internal Gradle type " + reference.getName());
            }
            private String formatMethodReference(TypeReference type, String methodName, String methodSignature) {
                return formatReference("internal Gradle method " + type.getName() + "." + methodName + methodSignature);
            }
            protected abstract String formatReference(String reference);
        }
    }
}
