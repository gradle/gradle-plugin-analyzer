package org.gradlex.plugins.analyzer;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.types.TypeName;
import com.ibm.wala.types.TypeReference;

import javax.annotation.Nullable;

public record TypeResolverImpl(ClassHierarchy hierarchy) implements TypeResolver {

    @Override
    public TypeReference findReference(String name) {
        var normalizedTypeName = normalizeTypeName(name);
        var reference = TypeReference.find(hierarchy.getScope().getApplicationLoader(), normalizedTypeName);
        if (reference == null) {
            return null;
        }
        // Unpack array types
        while (reference.isArrayType()) {
            reference = reference.getArrayElementType();
        }
        return reference;
    }

    private static TypeName normalizeTypeName(String typeName) {
        var normalizedName = typeName.endsWith(";")
            ? typeName.substring(0, typeName.length() - 1)
            : typeName;
        return TypeName.findOrCreate(normalizedName);
    }

    @Override
    public IClass findClass(String name) {
        TypeReference reference = findReference(name);
        if (reference == null) {
            return null;
        }
        return findClass(reference);
    }

    @Override
    @Nullable
    public IClass findClass(TypeReference reference) {
        if (reference.isPrimitiveType()) {
            return null;
        }

        return hierarchy.lookupClass(reference);
    }
}
