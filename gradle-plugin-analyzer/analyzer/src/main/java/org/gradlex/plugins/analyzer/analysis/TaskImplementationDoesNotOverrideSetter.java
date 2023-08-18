package org.gradlex.plugins.analyzer.analysis;

import java.util.regex.Pattern;

import static org.gradlex.plugins.analyzer.WalaUtil.matches;

/**
 * Task implementations should not override setters from the Gradle API.
 */
public class TaskImplementationDoesNotOverrideSetter extends AbstractTaskImplementationDoesNotOverrideMethod {
    private static final Pattern SETTER = Pattern.compile("set[A-Z].*");

    public TaskImplementationDoesNotOverrideSetter() {
        super("setter", method ->
            // Setters take two arguments: `this` and the value
            (method.getNumberOfParameters() == 2)
            // Setters have a name prefixed with `set`
            && matches(SETTER, method.getName())
        );
    }
}
