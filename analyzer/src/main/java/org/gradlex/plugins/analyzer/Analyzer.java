package org.gradlex.plugins.analyzer;

import sootup.core.Project;
import sootup.core.inputlocation.AnalysisInputLocation;
import sootup.java.bytecode.inputlocation.JavaClassPathAnalysisInputLocation;
import sootup.java.core.JavaProject;
import sootup.java.core.JavaSootClass;
import sootup.java.core.language.JavaLanguage;
import sootup.java.core.views.JavaView;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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
        view.getClasses().forEach(clazz -> {
            System.out.println(clazz);
        });
        System.out.println("Stored classes: " + view.getAmountOfStoredClasses());
    }
}
