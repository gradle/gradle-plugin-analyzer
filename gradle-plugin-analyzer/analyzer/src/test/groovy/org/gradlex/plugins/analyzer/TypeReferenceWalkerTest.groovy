package org.gradlex.plugins.analyzer

import com.ibm.wala.types.TypeReference
import org.gradlex.plugins.analyzer.analysis.AbstractAnalysisSpec

class TypeReferenceWalkerTest extends AbstractAnalysisSpec {
    def "annotation references are properly tracked"() {
        compileJava """
            import java.lang.annotation.*;

            @Retention(RetentionPolicy.CLASS)
            @interface ClassRef {
                Class<?> value();
            }
            
            @Retention(RetentionPolicy.CLASS)
            @interface StringRef {
                String value();
            }

            @ClassRef(org.gradle.api.internal.TaskOutputsInternal.class)
            @StringRef("org.gradle.api.internal.TaskInputsInternal")
            interface CustomType {
            }
        """

        def repository = new TypeRepository(files)
        def types = new TreeSet<String>()
        def methods = new TreeSet<String>()

        def visitor = new TypeReferenceWalker.ReferenceVisitor(repository.typeResolver) {
            @Override
            void visitReference(TypeReference reference) {
                types += reference.getName().toString()
            }

            @Override
            void visitMethodReference(String typeName, String methodName, String methodSignature) {
                methods += "${typeName}.${methodName}${methodSignature}"
            }
        }

        def clazz = repository.typeResolver.findClass("LCustomType")
        TypeReferenceWalker.walkReferences(clazz, TypeReferenceWalker.ReferenceVisitorFactory.alwaysWith(visitor))

        expect:
        types.toList() == [
            "LClassRef",
            "LStringRef",
            "Lorg/gradle/api/internal/TaskOutputsInternal",
        ]
        methods.toList() == []
    }
}
