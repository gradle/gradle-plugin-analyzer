package org.gradlex.plugins.analyzer;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.types.TypeReference;

import javax.annotation.Nullable;
import java.util.Objects;

public interface TypeResolver {

    @Nullable
    TypeReference findReference(String name);

    default TypeReference getReference(String name) {
        return Objects.requireNonNull(findReference(name));
    }

    @Nullable
    IClass findClass(TypeReference reference);

    /**
     * Returns {@code null} for primitives and `V`.
     */
    @Nullable
    IClass findClass(String name);

    default IClass getClass(String name) {
        return Objects.requireNonNull(findClass(name));
    }

    @Nullable
    IMethod resolveMethod(String typeName, String methodSignature);
}
