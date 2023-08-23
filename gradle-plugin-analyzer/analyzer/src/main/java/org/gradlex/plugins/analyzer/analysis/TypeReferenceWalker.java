package org.gradlex.plugins.analyzer.analysis;

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
import org.gradlex.plugins.analyzer.TypeOrigin;
import org.gradlex.plugins.analyzer.WalaUtil;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Stream;

public class TypeReferenceWalker {

    public static void walkReferences(IClass type, ReferenceVisitorFactory visitorFactory) {
        visitHierarchy(type, visitorFactory.forTypeHierarchy(type));

        visitAnnotations(type.getAnnotations(), visitorFactory.forTypeAnnotations(type));

        Stream.concat(
                type.getDeclaredStaticFields().stream(),
                type.getDeclaredInstanceFields().stream())
            .forEach(field -> {
                visitAnnotations(field.getAnnotations(), visitorFactory.forFieldAnnotations(field));
                visitorFactory.forFieldDeclaration(field).visitReference(field.getFieldTypeReference());
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
            visitor.visitReference(annotation.getType());
            Stream.ofNullable(annotation.getUnnamedArguments())
                .flatMap(Arrays::stream)
                .map(arg -> arg.fst)
                .forEach(visitor::visitReference);
            annotation.getNamedArguments().values()
                .forEach(rootValue -> WalaUtil.visitHierarchy(rootValue, value -> {
                    if (value instanceof ArrayElementValue vArray) {
                        return Arrays.stream(vArray.vals);
                    } else if (value instanceof AnnotationAttribute vAnnotation) {
                        visitor.visitReference(vAnnotation.type);
                        return vAnnotation.elementValues.values().stream();
                    } else if (value instanceof ConstantElementValue vConst) {
                        if (vConst.val instanceof String typeName) {
                            visitor.visitReference(typeName);
                        }
                    } else if (value instanceof EnumElementValue vEnum) {
                        visitor.visitReference(vEnum.enumType);
                    }
                    return Stream.empty();
                }));
        });
    }

    private static void visitMethod(IMethod method, ReferenceVisitorFactory visitorFactory) {
        visitAnnotations(method.getAnnotations(), visitorFactory.forMethodAnnotations(method));

        ReferenceVisitor declarationVisitor = visitorFactory.forMethodDeclaration(method);
        declarationVisitor.visitReference(method.getReturnType());
        for (int iParam = 0; iParam < method.getNumberOfParameters(); iParam++) {
            declarationVisitor.visitReference(method.getParameterType(iParam));
        }
        getDeclaredExceptions(method).forEach(declarationVisitor::visitReference);

        ReferenceVisitor bodyVisitor = visitorFactory.forMethodBody(method);
        WalaUtil.instructions(method)
            .forEach(instruction -> visitReferencedTypes(instruction, bodyVisitor));
    }

    private static Stream<TypeReference> getDeclaredExceptions(IMethod method) {
        try {
            return Stream.ofNullable(method.getDeclaredExceptions()).flatMap(Arrays::stream);
        } catch (InvalidClassFileException e) {
            throw new RuntimeException(e);
        }
    }

    private static void visitHierarchy(IClass baseType, ReferenceVisitor visitor) {
        WalaUtil.visitHierarchy(baseType, type -> directSuperTypes(type)
            .flatMap(superType -> switch (TypeOrigin.of(superType)) {
                case PUBLIC ->
                    // Ignore referenced public types and their supertypes
                    Stream.<IClass>empty();
                case INTERNAL -> {
                    // Report referenced internal type
                    visitor.visitReference(superType);
                    yield Stream.<IClass>empty();
                }
                default ->
                    // Visit external supertype
                    Stream.of(superType);
            })
        );
    }

    private static Stream<IClass> directSuperTypes(IClass type) {
        return Stream.concat(
            Stream.ofNullable(type.getSuperclass()),
            type.getDirectInterfaces().stream());
    }

    private static void visitReferencedTypes(IInstruction instruction, ReferenceVisitor visitor) {
        instruction.visit(new Visitor() {
            @Override
            public void visitConstant(ConstantInstruction instruction) {
                visitor.visitReference(instruction.getType());
                if (instruction.getValue() instanceof ClassToken token) {
                    visitor.visitReference(token.getTypeName());
                }
            }

            @Override
            public void visitLocalLoad(ILoadInstruction instruction) {
                visitor.visitReference(instruction.getType());
            }

            @Override
            public void visitLocalStore(IStoreInstruction instruction) {
                visitor.visitReference(instruction.getType());
            }

            @Override
            public void visitGoto(GotoInstruction instruction) {
                // Doesn't track type
            }

            @Override
            public void visitArrayLoad(IArrayLoadInstruction instruction) {
                visitor.visitReference(instruction.getType());
            }

            @Override
            public void visitArrayStore(IArrayStoreInstruction instruction) {
                visitor.visitReference(instruction.getType());
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
                visitor.visitReference(instruction.getType());
            }

            @Override
            public void visitUnaryOp(IUnaryOpInstruction instruction) {
                visitor.visitReference(instruction.getType());
            }

            @Override
            public void visitShift(IShiftInstruction instruction) {
                visitor.visitReference(instruction.getType());
            }

            @Override
            public void visitConversion(IConversionInstruction instruction) {
                visitor.visitReference(instruction.getFromType());
                visitor.visitReference(instruction.getToType());
            }

            @Override
            public void visitComparison(IComparisonInstruction instruction) {
                visitor.visitReference(instruction.getType());
            }

            @Override
            public void visitConditionalBranch(IConditionalBranchInstruction instruction) {
                visitor.visitReference(instruction.getType());
            }

            @Override
            public void visitSwitch(SwitchInstruction instruction) {
                // Doesn't track type
            }

            @Override
            public void visitReturn(ReturnInstruction instruction) {
                visitor.visitReference(instruction.getType());
            }

            @Override
            public void visitGet(IGetInstruction instruction) {
                visitor.visitReference(instruction.getClassType());
                visitor.visitReference(instruction.getFieldType());
            }

            @Override
            public void visitPut(IPutInstruction instruction) {
                visitor.visitReference(instruction.getClassType());
                visitor.visitReference(instruction.getFieldType());
            }

            @Override
            public void visitInvoke(IInvokeInstruction instruction) {
                visitor.visitMethodReference(instruction.getClassType(), instruction.getMethodName(), instruction.getMethodSignature());
            }

            @Override
            public void visitNew(NewInstruction instruction) {
                visitor.visitReference(instruction.getType());
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
                Arrays.stream(instruction.getTypes()).forEach(visitor::visitReference);
            }

            @Override
            public void visitInstanceof(IInstanceofInstruction instruction) {
                visitor.visitReference(instruction.getType());
            }

            @Override
            public void visitLoadIndirect(ILoadIndirectInstruction instruction) {
                // Doesn't track type
            }

            @Override
            public void visitStoreIndirect(IStoreIndirectInstruction instruction) {
                visitor.visitReference(instruction.getType());
            }
        });
    }

    public interface ReferenceVisitorFactory {
        ReferenceVisitor forTypeHierarchy(IClass type);

        ReferenceVisitor forTypeAnnotations(IClass type);

        ReferenceVisitor forFieldDeclaration(IField field);

        ReferenceVisitor forFieldAnnotations(IField field);

        ReferenceVisitor forMethodDeclaration(IMethod originMethod);

        ReferenceVisitor forMethodBody(IMethod originMethod);

        ReferenceVisitor forMethodAnnotations(IMethod method);
    }

    public abstract static class ReferenceVisitor {
        @Nullable
        protected abstract TypeReference findReference(String typeName);

        public void visitReference(String typeName) {
            TypeReference reference = findReference(typeName);
            if (reference != null) {
                visitReference(reference);
            }
        }

        public abstract void visitMethodReference(String typeName, String methodName, String methodSignature);

        public abstract void visitReference(TypeReference reference);

        public void visitReference(IClass type) {
            visitReference(type.getReference());
        }
    }
}
