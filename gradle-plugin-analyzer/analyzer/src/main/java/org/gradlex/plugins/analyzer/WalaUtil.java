package org.gradlex.plugins.analyzer;

import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.classLoader.ShrikeCTMethod;
import com.ibm.wala.core.util.strings.Atom;
import com.ibm.wala.shrike.shrikeBT.IInstruction;
import com.ibm.wala.shrike.shrikeCT.InvalidClassFileException;

import java.util.Arrays;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class WalaUtil {
    public static boolean matches(Pattern pattern, Atom name) {
        return pattern.matcher(name.toString()).matches();
    }

    public static Stream<IInstruction> instructions(IMethod method) {
        try {
            return Arrays.stream(((ShrikeCTMethod) method).getInstructions());
        } catch (InvalidClassFileException e) {
            throw new RuntimeException(e);
        }
    }
}
