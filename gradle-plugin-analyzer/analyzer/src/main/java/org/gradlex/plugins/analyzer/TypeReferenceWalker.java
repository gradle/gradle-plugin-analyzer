package org.gradlex.plugins.analyzer;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IField;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.shrike.shrikeBT.ArrayLengthInstruction;
import com.ibm.wala.shrike.shrikeBT.ConstantInstruction;
import com.ibm.wala.shrike.shrikeBT.ConstantInstruction.ClassToken;
import com.ibm.wala.shrike.shrikeBT.DupInstruction;
import com.ibm.wala.shrike.shrikeBT.GotoInstruction;
import com.ibm.wala.shrike.shrikeBT.IArrayLoadInstruction;
import com.ibm.wala.shrike.shrikeBT.IArrayStoreInstruction;
import com.ibm.wala.shrike.shrikeBT.IBinaryOpInstruction;
import com.ibm.wala.shrike.shrikeBT.IComparisonInstruction;
import com.ibm.wala.shrike.shrikeBT.IConditionalBranchInstruction;
import com.ibm.wala.shrike.shrikeBT.IConversionInstruction;
import com.ibm.wala.shrike.shrikeBT.IGetInstruction;
import com.ibm.wala.shrike.shrikeBT.IInstanceofInstruction;
import com.ibm.wala.shrike.shrikeBT.IInstruction;
import com.ibm.wala.shrike.shrikeBT.IInstruction.Visitor;
import com.ibm.wala.shrike.shrikeBT.IInvokeInstruction;
import com.ibm.wala.shrike.shrikeBT.ILoadIndirectInstruction;
import com.ibm.wala.shrike.shrikeBT.ILoadInstruction;
import com.ibm.wala.shrike.shrikeBT.IPutInstruction;
import com.ibm.wala.shrike.shrikeBT.IShiftInstruction;
import com.ibm.wala.shrike.shrikeBT.IStoreIndirectInstruction;
import com.ibm.wala.shrike.shrikeBT.IStoreInstruction;
import com.ibm.wala.shrike.shrikeBT.ITypeTestInstruction;
import com.ibm.wala.shrike.shrikeBT.IUnaryOpInstruction;
import com.ibm.wala.shrike.shrikeBT.MonitorInstruction;
import com.ibm.wala.shrike.shrikeBT.NewInstruction;
import com.ibm.wala.shrike.shrikeBT.PopInstruction;
import com.ibm.wala.shrike.shrikeBT.ReturnInstruction;
import com.ibm.wala.shrike.shrikeBT.SwapInstruction;
import com.ibm.wala.shrike.shrikeBT.SwitchInstruction;
import com.ibm.wala.shrike.shrikeBT.ThrowInstruction;
import com.ibm.wala.shrike.shrikeCT.AnnotationsReader.AnnotationAttribute;
import com.ibm.wala.shrike.shrikeCT.AnnotationsReader.ArrayElementValue;
import com.ibm.wala.shrike.shrikeCT.AnnotationsReader.ConstantElementValue;
import com.ibm.wala.shrike.shrikeCT.AnnotationsReader.EnumElementValue;
import com.ibm.wala.shrike.shrikeCT.InvalidClassFileException;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.types.annotations.Annotation;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Stream;

public class TypeReferenceWalker {

    public static void walkReferences(IClass type, ReferenceVisitorFactory visitorFactory) {
        WalaUtil.visitImmediateInternalSupertypes(type, visitorFactory.forTypeHierarchy(type)::visitType);

        visitAnnotations(type.getAnnotations(), visitorFactory.forTypeAnnotations(type));

        Stream.concat(
                type.getDeclaredStaticFields().stream(),
                type.getDeclaredInstanceFields().stream())
            .forEach(field -> {
                visitAnnotations(field.getAnnotations(), visitorFactory.forFieldAnnotations(field));
                visitorFactory.forFieldDeclaration(field).visitType(field.getFieldTypeReference());
            });

        Stream.concat(
                Stream.ofNullable(type.getClassInitializer()),
                type.getDeclaredMethods().stream())
            .forEach(method -> visitMethod(method, visitorFactory));
    }

    private static void visitAnnotations(@Nullable Collection<Annotation> annotations, ReferenceVisitor visitor) {
        if (annotations == null) {
            return;
        }
        annotations.forEach(annotation -> {
            visitor.visitType(annotation.getType());
            Stream.ofNullable(annotation.getUnnamedArguments())
                .flatMap(Arrays::stream)
                .map(arg -> arg.fst)
                .forEach(visitor::visitType);
            annotation.getNamedArguments().values()
                .forEach(rootValue -> WalaUtil.visitHierarchy(rootValue, value -> {
                    if (value instanceof ArrayElementValue vArray) {
                        return Arrays.stream(vArray.vals);
                    } else if (value instanceof AnnotationAttribute vAnnotation) {
                        visitor.visitType(vAnnotation.type);
                        return vAnnotation.elementValues.values().stream();
                    } else if (value instanceof ConstantElementValue vConst) {
                        if (vConst.val instanceof String typeName) {
                            try {
                                visitor.visitType(typeName);
                            } catch (IllegalArgumentException ignored) {
                                // Sometimes we end up here when the argument is not a Class, but a String with some random crap.
                                // We could look at the Annotation's parameter definitions, and figure if it was a Class.
                                // But it's probably also fine to have some very unlikely false positives here.
                            }
                        }
                    } else if (value instanceof EnumElementValue vEnum) {
                        visitor.visitType(vEnum.enumType);
                    }
                    return Stream.empty();
                }));
        });
    }

    private static void visitMethod(IMethod method, ReferenceVisitorFactory visitorFactory) {
        visitAnnotations(method.getAnnotations(), visitorFactory.forMethodAnnotations(method));

        ReferenceVisitor declarationVisitor = visitorFactory.forMethodDeclaration(method);
        declarationVisitor.visitType(method.getReturnType());
        for (int iParam = 0; iParam < method.getNumberOfParameters(); iParam++) {
            declarationVisitor.visitType(method.getParameterType(iParam));
        }
        getDeclaredExceptions(method).forEach(declarationVisitor::visitType);

        // Do not report inherited constructors as we already report extending internal types
        if (!method.isInit() && !method.isClinit()) {
            ReferenceVisitor inheritanceVisitor = visitorFactory.forMethodInheritance(method);
            WalaUtil.visitImmediateInternalSupertypes(method.getDeclaringClass(), superType -> {
                IMethod implementedMethod = superType.getMethod(method.getSelector());
                if (implementedMethod != null) {
                    inheritanceVisitor.visitMethod(implementedMethod);
                }
            });
        }

        ReferenceVisitor bodyVisitor = visitorFactory.forMethodBody(method);
        Arrays.stream(WalaUtil.instructions(method))
            .forEach(instruction -> visitReferencedTypes(instruction, bodyVisitor));
    }

    private static Stream<TypeReference> getDeclaredExceptions(IMethod method) {
        try {
            return Stream.ofNullable(method.getDeclaredExceptions()).flatMap(Arrays::stream);
        } catch (InvalidClassFileException e) {
            throw new RuntimeException(e);
        }
    }

    private static void visitReferencedTypes(IInstruction instruction, ReferenceVisitor visitor) {
        instruction.visit(new Visitor() {
            @Override
            public void visitConstant(ConstantInstruction instruction) {
                visitor.visitType(instruction.getType());
                if (instruction.getValue() instanceof ClassToken token) {
                    visitor.visitType(token.getTypeName());
                }
            }

            @Override
            public void visitLocalLoad(ILoadInstruction instruction) {
                visitor.visitType(instruction.getType());
            }

            @Override
            public void visitLocalStore(IStoreInstruction instruction) {
                visitor.visitType(instruction.getType());
            }

            @Override
            public void visitGoto(GotoInstruction instruction) {
                // Doesn't track type
            }

            @Override
            public void visitArrayLoad(IArrayLoadInstruction instruction) {
                visitor.visitType(instruction.getType());
            }

            @Override
            public void visitArrayStore(IArrayStoreInstruction instruction) {
                visitor.visitType(instruction.getType());
            }

            @Override
            public void visitPop(PopInstruction instruction) {
                // Doesn't track type
            }

            @Override
            public void visitDup(DupInstruction instruction) {
                // Doesn't track type
            }

            @Override
            public void visitSwap(SwapInstruction instruction) {
                // Doesn't track type
            }

            @Override
            public void visitBinaryOp(IBinaryOpInstruction instruction) {
                visitor.visitType(instruction.getType());
            }

            @Override
            public void visitUnaryOp(IUnaryOpInstruction instruction) {
                visitor.visitType(instruction.getType());
            }

            @Override
            public void visitShift(IShiftInstruction instruction) {
                visitor.visitType(instruction.getType());
            }

            @Override
            public void visitConversion(IConversionInstruction instruction) {
                visitor.visitType(instruction.getFromType());
                visitor.visitType(instruction.getToType());
            }

            @Override
            public void visitComparison(IComparisonInstruction instruction) {
                visitor.visitType(instruction.getType());
            }

            @Override
            public void visitConditionalBranch(IConditionalBranchInstruction instruction) {
                visitor.visitType(instruction.getType());
            }

            @Override
            public void visitSwitch(SwitchInstruction instruction) {
                // Doesn't track type
            }

            @Override
            public void visitReturn(ReturnInstruction instruction) {
                visitor.visitType(instruction.getType());
            }

            @Override
            public void visitGet(IGetInstruction instruction) {
                visitor.visitType(instruction.getClassType());
                visitor.visitType(instruction.getFieldType());
            }

            @Override
            public void visitPut(IPutInstruction instruction) {
                visitor.visitType(instruction.getClassType());
                visitor.visitType(instruction.getFieldType());
            }

            @Override
            public void visitInvoke(IInvokeInstruction instruction) {
                visitor.visitMethod(instruction.getClassType(), instruction.getMethodName() + instruction.getMethodSignature());
            }

            @Override
            public void visitNew(NewInstruction instruction) {
                visitor.visitType(instruction.getType());
            }

            @Override
            public void visitArrayLength(ArrayLengthInstruction instruction) {
                // Doesn't track type
            }

            @Override
            public void visitThrow(ThrowInstruction instruction) {
                // Doesn't track type
            }

            @Override
            public void visitMonitor(MonitorInstruction instruction) {
                // Doesn't track type
            }

            @Override
            public void visitCheckCast(ITypeTestInstruction instruction) {
                Arrays.stream(instruction.getTypes()).forEach(visitor::visitType);
            }

            @Override
            public void visitInstanceof(IInstanceofInstruction instruction) {
                visitor.visitType(instruction.getType());
            }

            @Override
            public void visitLoadIndirect(ILoadIndirectInstruction instruction) {
                // Doesn't track type
            }

            @Override
            public void visitStoreIndirect(IStoreIndirectInstruction instruction) {
                visitor.visitType(instruction.getType());
            }
        });
    }

    public interface ReferenceVisitorFactory {
        ReferenceVisitor forTypeHierarchy(IClass type);

        ReferenceVisitor forTypeAnnotations(IClass type);

        ReferenceVisitor forFieldDeclaration(IField field);

        ReferenceVisitor forFieldAnnotations(IField field);

        ReferenceVisitor forMethodDeclaration(IMethod originMethod);

        ReferenceVisitor forMethodInheritance(IMethod originMethod);

        ReferenceVisitor forMethodBody(IMethod originMethod);

        ReferenceVisitor forMethodAnnotations(IMethod method);

        static ReferenceVisitorFactory alwaysWith(ReferenceVisitor visitor) {
            return new ReferenceVisitorFactory() {
                @Override
                public ReferenceVisitor forTypeHierarchy(IClass type) {
                    return visitor;
                }

                @Override
                public ReferenceVisitor forTypeAnnotations(IClass type) {
                    return visitor;
                }

                @Override
                public ReferenceVisitor forFieldDeclaration(IField field) {
                    return visitor;
                }

                @Override
                public ReferenceVisitor forFieldAnnotations(IField field) {
                    return visitor;
                }

                @Override
                public ReferenceVisitor forMethodDeclaration(IMethod originMethod) {
                    return visitor;
                }

                @Override
                public ReferenceVisitor forMethodInheritance(IMethod originMethod) {
                    return visitor;
                }

                @Override
                public ReferenceVisitor forMethodBody(IMethod originMethod) {
                    return visitor;
                }

                @Override
                public ReferenceVisitor forMethodAnnotations(IMethod method) {
                    return visitor;
                }
            };
        }
    }

    public abstract static class ReferenceVisitor {
        private final TypeResolver typeResolver;

        protected ReferenceVisitor(TypeResolver typeResolver) {
            this.typeResolver = typeResolver;
        }

        public void visitType(String typeName) {
            TypeReference reference = typeResolver.findReference(typeName);
            if (reference != null) {
                visitType(reference);
            }
        }

        public abstract void visitType(TypeReference reference);

        public void visitType(IClass type) {
            visitType(type.getReference());
        }

        public abstract void visitMethod(IMethod method);

        public void visitMethod(String typeName, String methodSignature) {
            IMethod method = typeResolver.resolveMethod(typeName, methodSignature);
            if (method != null) {
                visitMethod(method);
            }
        }
    }
}
