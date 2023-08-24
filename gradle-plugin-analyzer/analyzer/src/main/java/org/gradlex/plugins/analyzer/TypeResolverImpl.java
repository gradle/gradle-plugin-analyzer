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
        if (normalizedTypeName == null) {
            return null;
        }
        var reference = TypeReference.find(hierarchy.getScope().getApplicationLoader(), normalizedTypeName);
        if (reference == null) {
            return null;
        }
        // Unpack array types
        if (reference.isArrayType()) {
            reference = reference.getInnermostElementType();
        }
        return reference;
    }

    @Nullable
    private static TypeName normalizeTypeName(String typeName) {
        var normalizedName = typeName.endsWith(";")
            ? typeName.substring(0, typeName.length() - 1)
            : typeName;
        if (normalizedName.isEmpty()) {
            return null;
        }
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
        if (reference.isArrayType()) {
            reference = reference.getInnermostElementType();
        }
        if (reference.isPrimitiveType()) {
            return null;
        }

        return hierarchy.lookupClass(reference);
    }
}
