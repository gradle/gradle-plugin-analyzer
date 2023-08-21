package org.gradlex.plugins.analyzer.analysis;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMember;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.classLoader.ShrikeCTMethod;
import com.ibm.wala.shrike.shrikeCT.InvalidClassFileException;
import org.gradlex.plugins.analyzer.ExternalSubtypeAnalysis;
import org.gradlex.plugins.analyzer.TypeOrigin;
import org.slf4j.event.Level;

import java.util.Arrays;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Stream;

public abstract class AbstractTaskImplementationDoesNotOverrideMethod extends ExternalSubtypeAnalysis {
    private final String methodType;
    private final Predicate<? super IMethod> filter;

    public AbstractTaskImplementationDoesNotOverrideMethod(String methodType, Predicate<? super IMethod> filter) {
        super("Lorg/gradle/api/Task");
        this.methodType = methodType;
        this.filter = filter;
    }

    @Override
    protected void analyzeType(IClass type, AnalysisContext context) {
        type.getDeclaredMethods().stream()
            // Ignore bridge methods
            .filter(Predicate.not(IMethod::isBridge))
            // Looking for instance methods
            .filter(Predicate.not(IMember::isStatic))
            // Looking for public or protected methods
            .filter(method -> method.isPublic() || method.isProtected())
            // Matching our filter
            .filter(filter)
            // Walk ancestry
            .forEach(method -> Stream.iterate(type.getSuperclass(), Objects::nonNull, IClass::getSuperclass)
                // We only care about methods that come from the Gradle API
                .filter(TypeOrigin::isGradleApi)
                // Find the same method in the superclass
                .flatMap(clazz -> Stream.ofNullable(clazz.getMethod(method.getSelector())))
                .findFirst()
                .ifPresent(overriddenMethod -> analyzeOverriddenMethod(type, context, method, overriddenMethod))
            );
    }

    private void analyzeOverriddenMethod(IClass type, AnalysisContext context, IMethod method, IMethod overriddenMethod) {
        context.report(Level.WARN, String.format("The %s %s() in %s overrides Gradle API from %s",
            methodType, method.getName(), type.getName(), overriddenMethod.getDeclaringClass().getName()));
        try {
            System.out.println("--------------------");
            System.out.println("Method: " + method.getSignature());
            System.out.println();
            Arrays.stream(((ShrikeCTMethod) method).getInstructions())
                .forEach(iInstruction -> {
                    System.out.println("Instruction: " + iInstruction);
                });
        } catch (InvalidClassFileException e) {
            throw new RuntimeException(e);
        }
    }
}
