package org.gradlex.plugins.analyzer;

import com.google.common.collect.ImmutableList;
import sootup.core.IdentifierFactory;
import sootup.core.types.ClassType;
import sootup.java.bytecode.inputlocation.JavaClassPathAnalysisInputLocation;
import sootup.java.core.JavaProject;
import sootup.java.core.JavaSootClass;
import sootup.java.core.language.JavaLanguage;
import sootup.java.core.views.JavaView;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public class Analyzer {

    private final JavaView view;
    private final IdentifierFactory identifiers;

    public Analyzer(Path directory) throws IOException {
        List<String> classpath;
        try (Stream<Path> files = Files.list(directory)) {
            classpath = files
                .filter(path -> path.getFileName().toString().endsWith(".jar"))
                .map(Path::toString)
                .collect(ImmutableList.toImmutableList());
        }

        System.out.println("Classpath:");
        classpath.forEach(element -> System.out.printf(" - %s%n", element));

        JavaProject project = JavaProject
            .builder(new JavaLanguage(17))
            .addInputLocation(new JavaClassPathAnalysisInputLocation(String.join(File.pathSeparator, classpath)))
            .build();

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
