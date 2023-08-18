package org.gradlex.plugins.analyzer;

import com.ibm.wala.core.util.strings.Atom;

import java.util.regex.Pattern;

public class WalaUtil {
    public static boolean matches(Pattern pattern, Atom name) {
        return pattern.matcher(name.toString()).matches();
    }
}
