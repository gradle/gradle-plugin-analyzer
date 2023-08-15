package org.gradlex.plugins.analyzer;

import sootup.core.IdentifierFactory;
import sootup.core.model.SourceType;
import sootup.core.types.ClassType;
import sootup.java.bytecode.inputlocation.JrtFileSystemAnalysisInputLocation;
import sootup.java.bytecode.inputlocation.PathBasedAnalysisInputLocation;
import sootup.java.core.JavaProject;
import sootup.java.core.JavaProject.JavaProjectBuilder;
import sootup.java.core.JavaSootClass;
import sootup.java.core.language.JavaLanguage;
import sootup.java.core.views.JavaView;

import java.nio.file.Path;
import java.util.Collection;
import java.util.Optional;

public class Analyzer {

    private final JavaView view;
    private final IdentifierFactory identifiers;

    public Analyzer(Collection<Path> classpath) {
        JavaProjectBuilder builder = JavaProject
            .builder(new JavaLanguage(17))
            .addInputLocation(new JrtFileSystemAnalysisInputLocation());

        classpath.forEach(entry -> builder.addInputLocation(new PathBasedAnalysisInputLocation(entry, SourceType.Application)));

        JavaProject project = builder.build();

        this.view = project.createView();
        this.identifiers = project.getIdentifierFactory();
    }

    void analyze() {
        ClassType classType = identifiers.getClassType("com.vaadin.gradle.VaadinCleanTask");

        while (true) {
            Optional<JavaSootClass> foundClass = view.getClass(classType);
            if (foundClass.isEmpty()) {
                System.out.println("Cannot find class " + classType);
                break;
            }
            JavaSootClass clazz = foundClass.get();
            System.out.println("Found class: " + clazz);

            clazz.getMethods().forEach(method -> System.out.printf(" - %s%n", method));

            Optional<? extends ClassType> superclass = clazz.getSuperclass();
            if (superclass.isPresent()) {
                classType = superclass.get();
            } else {
                System.out.println("No superclass present");
                break;
            }
        }

        System.out.println("Stored classes: " + view.getAmountOfStoredClasses());
    }
}
