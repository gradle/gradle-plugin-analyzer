package org.gradlex.plugins.analyzer;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IField;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.types.TypeReference;

public record Reference(Source source, Target target) {
    public sealed interface Source {
    }

    public sealed interface Target {
    }

    public record TypeDeclarationSource(IClass type) implements Source {
    }

    public record TypeInheritanceSource(IClass type) implements Source {
    }

    public record FieldDeclarationSource(IField field) implements Source {
    }

    public record MethodDeclarationSource(IMethod method) implements Source {
    }

    public record MethodInheritanceSource(IMethod method) implements Source {
    }

    public record MethodBodySource(IMethod method) implements Source {
    }

    public record TypeTarget(TypeReference type) implements Target {
    }

    public record MethodTarget(IMethod method) implements Target {
    }
}
