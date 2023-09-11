package org.gradlex.plugins.analyzer;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IField;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.types.TypeReference;
import org.gradlex.plugins.analyzer.Reporter.Message;

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

    public static Message format(Reference reference) {
        Target refTarget = reference.target();
        Object target;
        if (refTarget instanceof TypeTarget) {
            target = ((TypeTarget) refTarget).type();
        } else if (refTarget instanceof MethodTarget) {
            target = ((MethodTarget) refTarget).method();
        } else {
            throw new AssertionError();
        }

        Source source = reference.source();
        if (source instanceof TypeDeclarationSource) {
            return new Message("The %s references %s",
                ((TypeDeclarationSource) source).type(), target);
        }
        if (source instanceof TypeInheritanceSource) {
            return new Message("The %s extends %s",
                ((TypeInheritanceSource) source).type(), target);
        }
        if (source instanceof FieldDeclarationSource) {
            return new Message("The %s references %s",
                ((FieldDeclarationSource) source).field(), target);
        }
        if (source instanceof MethodDeclarationSource) {
            return new Message("The declaration of %s references %s",
                ((MethodDeclarationSource) source).method(), target);
        }
        if (source instanceof MethodInheritanceSource) {
            return new Message("The %s overrides %s",
                ((MethodInheritanceSource) source).method(), target);
        }
        if (source instanceof MethodBodySource) {
            return new Message("The %s references %s",
                ((MethodBodySource) source).method(), target);
        }
        throw new AssertionError();
    }
}
