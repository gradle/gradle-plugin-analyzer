package org.gradlex.plugins.analyzer;

import sootup.core.Project;
import sootup.core.inputlocation.AnalysisInputLocation;
import sootup.core.types.ClassType;
import sootup.java.bytecode.inputlocation.JavaClassPathAnalysisInputLocation;
import sootup.java.core.JavaIdentifierFactory;
import sootup.java.core.JavaProject;
import sootup.java.core.JavaSootClass;
import sootup.java.core.language.JavaLanguage;
import sootup.java.core.views.JavaView;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.stream.Collectors;

public class Analyzer {
    void analyze(Path directory) throws IOException {
        String classpath = Files.list(directory)
            .filter(path -> path.getFileName().toString().endsWith(".jar"))
            .map(Path::toString)
            .collect(Collectors.joining(File.pathSeparator));
        AnalysisInputLocation<JavaSootClass> inputLocation =
            new JavaClassPathAnalysisInputLocation(classpath);

        JavaLanguage language = new JavaLanguage(17);

        Project<JavaSootClass, JavaView> project = JavaProject.builder(language).addInputLocation(inputLocation).build();
        JavaView view = project.createView();

        ClassType classType = JavaIdentifierFactory.getInstance().getClassType("com.vaadin.gradle.VaadinCleanTask");
        while (true) {
            Optional<JavaSootClass> foundClass = view.getClass(classType);
            if (!foundClass.isPresent()) {
                System.out.println("Cannot find class " + classType);
                break;
            }
            JavaSootClass clazz = foundClass.get();
            System.out.println("Found class: " + clazz);
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
