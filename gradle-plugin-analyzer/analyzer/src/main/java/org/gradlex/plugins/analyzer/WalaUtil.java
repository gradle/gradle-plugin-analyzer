package org.gradlex.plugins.analyzer;

import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.classLoader.ShrikeCTMethod;
import com.ibm.wala.core.util.strings.Atom;
import com.ibm.wala.shrike.shrikeBT.IInstruction;
import com.ibm.wala.shrike.shrikeCT.InvalidClassFileException;
import com.ibm.wala.types.TypeReference;

import java.util.Arrays;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class WalaUtil {
    public static boolean matches(Pattern pattern, Atom name) {
        return pattern.matcher(name.toString()).matches();
    }

    public static String normalizeTypeName(String typeName) {
        return typeName.endsWith(";")
            ? typeName.substring(0, typeName.length() - 1)
            : typeName;
    }

    public static Stream<IInstruction> instructions(IMethod method) {
        try {
            IInstruction[] instructions = ((ShrikeCTMethod) method).getInstructions();
            if (instructions != null) {
                return Arrays.stream(instructions);
            } else {
                // TODO Why can't we sometimes get the instructions?
                return Stream.of();
            }
        } catch (InvalidClassFileException e) {
            throw new RuntimeException(e);
        }
    }

    private static TypeReference[] getDeclaredExceptions(IMethod method) {
        try {
            return method.getDeclaredExceptions();
        } catch (InvalidClassFileException e) {
            throw new RuntimeException(e);
        }
    }
}
