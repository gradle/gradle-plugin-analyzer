package org.gradlex.plugins.analyzer.analysis;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IField;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.types.TypeReference;
import org.gradlex.plugins.analyzer.ExternalSubtypeAnalysis;
import org.gradlex.plugins.analyzer.TypeOrigin;
import org.gradlex.plugins.analyzer.analysis.TypeReferenceWalker.ReferenceVisitor;
import org.gradlex.plugins.analyzer.analysis.TypeReferenceWalker.ReferenceVisitorFactory;

import javax.annotation.Nullable;
import java.util.SortedSet;
import java.util.TreeSet;

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
        ReferenceCollector referenceCollector = new ReferenceCollector(context);
        
        TypeReferenceWalker.walkReferences(type, context, referenceCollector);

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
            return new Recorder() {
                @Override
                protected String formatReference(TypeReference reference) {
                    return "Type %s extends internal Gradle type: %s".formatted(type.getName(), reference.getName());
                }
            };
        }

        @Override
        public ReferenceVisitor forTypeAnnotations(IClass type) {
            return forAnnotations("type " + type.getName());
        }

        @Override
        public ReferenceVisitor forFieldDeclaration(IField field) {
            return new Recorder() {
                @Override
                protected String formatReference(TypeReference reference) {
                    return "Field %s references internal Gradle type: %s".formatted(field.getName(), reference.getName());
                }
            };
        }

        @Override
        public ReferenceVisitor forFieldAnnotations(IField field) {
            return forAnnotations("field " + field.getName());
        }

        @Override
        public ReferenceVisitor forMethodDeclaration(IMethod originMethod) {
            return new Recorder() {
                @Override
                protected String formatReference(TypeReference reference) {
                    return "Method declaration %s references internal Gradle type: %s".formatted(originMethod.getSignature(), reference.getName());
                }
            };
        }

        @Override
        public ReferenceVisitor forMethodBody(IMethod originMethod) {
            return new Recorder() {
                @Override
                protected String formatReference(TypeReference reference) {
                    return "Method %s references internal Gradle type: %s".formatted(originMethod.getSignature(), reference.getName());
                }
            };
        }

        @Override
        public ReferenceVisitor forMethodAnnotations(IMethod method) {
            return forAnnotations("method " + method.getSignature());
        }

        private ReferenceVisitor forAnnotations(String subject) {
            return new Recorder() {
                @Override
                protected String formatReference(TypeReference reference) {
                    return "Annotation on %s references internal Gradle APIs: %s".formatted(subject, reference.getName());
                }
            };
        }

        public abstract class Recorder extends ReferenceVisitor {
            @Nullable
            @Override
            protected TypeReference findReference(String typeName) {
                return context.findReference(typeName);
            }

            @Override
            public void visitReference(TypeReference reference) {
                if (TypeOrigin.of(reference) == TypeOrigin.INTERNAL) {
                    references.add(formatReference(reference));
                }
            }

            protected abstract String formatReference(TypeReference reference);
        }
    }
}
