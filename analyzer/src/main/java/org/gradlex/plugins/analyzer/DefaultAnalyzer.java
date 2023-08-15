package org.gradlex.plugins.analyzer;

import sootup.core.IdentifierFactory;
import sootup.core.model.SootClassMember;
import sootup.core.model.SourceType;
import sootup.core.types.ClassType;
import sootup.java.bytecode.inputlocation.PathBasedAnalysisInputLocation;
import sootup.java.core.JavaProject;
import sootup.java.core.JavaProject.JavaProjectBuilder;
import sootup.java.core.language.JavaLanguage;
import sootup.java.core.views.JavaView;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.nio.file.Path;
import java.util.Collection;

import static org.gradlex.plugins.analyzer.TypeOrigin.EXTERNAL;

@Retention(RetentionPolicy.RUNTIME)
@interface Bela {
    String value();
}

@Retention(RetentionPolicy.RUNTIME)
@interface Alma {
    Bela value();
}

public class DefaultAnalyzer implements Analyzer {

    private final JavaView view;
    private final IdentifierFactory identifiers;

    public DefaultAnalyzer(Collection<Path> classpath) {
        JavaProjectBuilder builder = JavaProject
            .builder(new JavaLanguage(17));

        // TODO Commented out because with it TypeHierarchy.subtypesOf() does not return anything
        // builder.addInputLocation(new JrtFileSystemAnalysisInputLocation());

        classpath.stream()
            .map(entry -> new PathBasedAnalysisInputLocation(entry, SourceType.Application))
            .forEach(builder::addInputLocation);

        JavaProject project = builder.build();

        this.view = project.createView();
        this.identifiers = project.getIdentifierFactory();
    }

    @Override
    @Alma(@Bela("tibor"))
    public void analyze() {
//        ArrayDeque<ClassType> queue = new ArrayDeque<>();
//        Set<ClassType> seen = new HashSet<>();
//        queue.add(identifiers.getClassType("com.vaadin.gradle.VaadinCleanTask"));
//
//        while (!queue.isEmpty()) {
//            ClassType classType = queue.poll();
//            if (classType == null) {
//                break;
//            }
//            if (!seen.add(classType)) {
//                continue;
//            }
//
//            Optional<JavaSootClass> foundClass = view.getClass(classType);
//            if (foundClass.isEmpty()) {
//                System.out.println("Cannot find class " + classType);
//                continue;
//            }
//
//            JavaSootClass clazz = foundClass.get();
//            System.out.println("Found class: " + clazz);
//
//            queue.addAll(clazz.getInterfaces());
//            clazz.getSuperclass().ifPresent(queue::add);
//        }

//        view.getTypeHierarchy().subtypesOf(classType("org.gradle.api.Task")).stream()
        view.getClasses().stream()
            .filter(clazz -> TypeOrigin.of(clazz.getType()) == EXTERNAL)
            .forEach(clazz -> {
                System.out.println("Found external task type: " + clazz.getType());
                clazz.getMethods().stream()
                    .filter(DefaultAnalyzer::isVisible)
                    .forEach(method -> {
                        System.out.println("- method: " + method);
                    });
            });

        view.getTypeHierarchy().subtypesOf(classType("org.gradle.api.Plugin")).stream()
            .filter(type -> TypeOrigin.of(type) == EXTERNAL)
            .forEach(implementor -> {
                System.out.printf("Found external plugin type: %s%n", implementor);
            });

        System.out.println("Stored classes: " + view.getAmountOfStoredClasses());
    }

    private static boolean isVisible(SootClassMember<?> member) {
        return member.isPublic() || member.isProtected();
    }

    private ClassType classType(String fqcn) {
        return identifiers.getClassType(fqcn);
    }
}
