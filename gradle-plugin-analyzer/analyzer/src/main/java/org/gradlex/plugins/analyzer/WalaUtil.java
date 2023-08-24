package org.gradlex.plugins.analyzer;

import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.classLoader.ShrikeCTMethod;
import com.ibm.wala.core.util.strings.Atom;
import com.ibm.wala.shrike.shrikeBT.IInstruction;
import com.ibm.wala.shrike.shrikeCT.InvalidClassFileException;
import com.ibm.wala.types.TypeReference;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class WalaUtil {
    public static boolean matches(Pattern pattern, Atom name) {
        return pattern.matcher(name.toString()).matches();
    }

    public static IInstruction[] instructions(IMethod method) {
        try {
            IInstruction[] instructions = ((ShrikeCTMethod) method).getInstructions();
            if (instructions != null) {
                return instructions;
            } else {
                // TODO Why can't we sometimes get the instructions?
                return new IInstruction[0];
            }
        } catch (InvalidClassFileException e) {
            throw new RuntimeException(e);
        }
    }

    public static boolean matchesType(Supplier<String> typeName, TypeReference reference) {
        return matchesType(typeName.get(), reference);
    }

    public static boolean matchesType(String typeName, TypeReference reference) {
        return typeName.equals(reference.getName().toString() + ";");
    }

    public static <T> void visitHierarchy(T seed, Function<? super T, Stream<? extends T>> visitor) {
        var seen = new HashSet<>();
        visitHierarchy(seed, seen::add, visitor);
    }

    public static <T> void visitHierarchy(T seed, Predicate<? super T> seen, Function<? super T, Stream<? extends T>> visitor) {
        if (!seen.test(seed)) {
            return;
        }

        var queue = new ArrayDeque<T>();
        queue.add(seed);
        while (true) {
            T node = queue.poll();
            if (node == null) {
                break;
            }
            visitor.apply(node)
                .filter(seen)
                .forEach(queue::add);
        }
    }
}
