package org.gradlex.plugins.analyzer.analysis;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMember;
import com.ibm.wala.classLoader.IMethod;
import org.gradlex.plugins.analyzer.ExternalSubtypeAnalysis;
import org.gradlex.plugins.analyzer.TypeOrigin;
import org.slf4j.event.Level;

import java.util.Objects;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Task implementations should only extend {@code org.gradle.api.DefaultTask}, not other Gradle task types.
 */
public class TaskImplementationDoesNotOverrideSetter extends ExternalSubtypeAnalysis {
    private static final Pattern SETTER = Pattern.compile("set[A-Z].*");

    public TaskImplementationDoesNotOverrideSetter() {
        super("Lorg/gradle/api/Task");
    }

    @Override
    protected void analyzeType(IClass type, AnalysisContext context) {
        type.getDeclaredMethods().stream()
            // Setters are instance methods
            .filter(Predicate.not(IMember::isStatic))
            // Setters are public or protected
            .filter(method -> method.isPublic() || method.isProtected())
            // Setters take two arguments: `this` and the value
            .filter(method -> method.getNumberOfParameters() == 2)
            // Setters have a name prefixed with `set`
            .filter(method -> SETTER.matcher(method.getName().toString()).matches())
            // Ignore bridge methods
            .filter(Predicate.not(IMethod::isBridge))
            // Walk ancestry
            .forEach(setter -> Stream.iterate(type.getSuperclass(), Objects::nonNull, IClass::getSuperclass)
                // We only care about methods that come from the Gradle API
                .filter(TypeOrigin::isGradleApi)
                // Find the same method in the superclass
                .flatMap(clazz -> Stream.ofNullable(clazz.getMethod(setter.getSelector())))
                .findFirst()
                .ifPresent(overriddenMethod -> context.report(Level.WARN, String.format("Setter %s() in %s overrides Gradle API from %s",
                    setter.getName(), type.getName(), overriddenMethod.getDeclaringClass().getName())))
            );
    }
}
