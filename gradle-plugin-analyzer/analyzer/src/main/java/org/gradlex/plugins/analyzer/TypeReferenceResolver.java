package org.gradlex.plugins.analyzer;

import com.ibm.wala.types.TypeReference;

public interface TypeReferenceResolver {
    TypeReference findReference(String typeName);
}
