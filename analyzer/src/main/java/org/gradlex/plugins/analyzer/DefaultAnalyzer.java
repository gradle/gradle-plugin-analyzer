package org.gradlex.plugins.analyzer;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.core.util.config.AnalysisScopeReader;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ipa.cha.ClassHierarchyFactory;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.config.FileOfClasses;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import static org.gradlex.plugins.analyzer.TypeOrigin.EXTERNAL;

public class DefaultAnalyzer implements Analyzer {

    public DefaultAnalyzer(Collection<Path> classpath) throws ClassHierarchyException, IOException {
        AnalysisScope scope = AnalysisScopeReader.instance.makeJavaBinaryAnalysisScope(toClasspath(classpath), null);
        scope.setExclusions(new FileOfClasses(new ByteArrayInputStream(EXCLUSIONS.getBytes(StandardCharsets.UTF_8))));

        ClassHierarchy cha = ClassHierarchyFactory.make(scope);

        TypeReference taskTypeRef = TypeReference.find(scope.getApplicationLoader(), "Lorg/gradle/api/Task");
        cha.getImplementors(taskTypeRef).stream()
            .filter(clazz -> TypeOrigin.of(clazz) == EXTERNAL)
            .forEach(taskType -> {
                System.out.println("Found external task type: " + taskType);
                Deque<IClass> queue = new ArrayDeque<>();
                queue.add(taskType);
                Set<IClass> seen = new HashSet<>();

                while (true) {
                    IClass type = queue.poll();
                    if (type == null) {
                        break;
                    }
                    if (!seen.add(type)) {
                        continue;
                    }

                    System.out.println(" - " + type.getName());
                    IClass superclass = type.getSuperclass();
                    if (superclass != null) {
                        queue.add(superclass);
                    }
                    queue.addAll(type.getDirectInterfaces());
                }
            });
    }

    private static String toClasspath(Collection<Path> classpath) {
        return classpath.stream()
            .map(Path::toString)
            .collect(Collectors.joining(File.pathSeparator));
    }

    @Override
    public void analyze() {
    }

    private static final String EXCLUSIONS = """
        java\\/awt\\/.*
        javax\\/swing\\/.*
        sun\\/awt\\/.*
        sun\\/swing\\/.*
        com\\/sun\\/.*
        sun\\/.*
        org\\/netbeans\\/.*
        org\\/openide\\/.*
        com\\/ibm\\/crypto\\/.*
        com\\/ibm\\/security\\/.*
        org\\/apache\\/xerces\\/.*
        java\\/security\\/.*
        """;
}
