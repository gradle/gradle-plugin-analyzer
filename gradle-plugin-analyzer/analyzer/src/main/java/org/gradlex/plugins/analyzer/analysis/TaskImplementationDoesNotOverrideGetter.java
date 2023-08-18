package org.gradlex.plugins.analyzer.analysis;

import com.ibm.wala.types.TypeReference;

import java.util.regex.Pattern;

import static org.gradlex.plugins.analyzer.WalaUtil.matches;

/**
 * Task implementations should not override getters from the Gradle API.
 */
public class TaskImplementationDoesNotOverrideGetter extends AbstractTaskImplementationDoesNotOverrideMethod {
    private static final Pattern GET_GETTER = Pattern.compile("get[A-Z].*");
    private static final Pattern IS_GETTER = Pattern.compile("is[A-Z].*");

    public TaskImplementationDoesNotOverrideGetter() {
        super("getter", method ->
            // Getters one argument: `this`
            (method.getNumberOfParameters() == 1)
            // Getters have a name prefixed with `get` or `is` with a `boolean` return type
            && (matches(GET_GETTER, method.getName())
                || (matches(IS_GETTER, method.getName()) && method.getReturnType().equals(TypeReference.Boolean)))
        );
    }
}
