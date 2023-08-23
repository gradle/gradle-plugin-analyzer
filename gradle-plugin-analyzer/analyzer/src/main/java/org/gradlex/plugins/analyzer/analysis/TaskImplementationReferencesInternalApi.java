package org.gradlex.plugins.analyzer.analysis;

import com.ibm.wala.classLoader.IClass;
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
import com.ibm.wala.shrike.shrikeCT.InvalidClassFileException;
import com.ibm.wala.types.Selector;
import com.ibm.wala.types.TypeReference;
import org.gradlex.plugins.analyzer.ExternalSubtypeAnalysis;
import org.gradlex.plugins.analyzer.TypeOrigin;
import org.gradlex.plugins.analyzer.WalaUtil;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.HashSet;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Stream;

import static org.slf4j.event.Level.DEBUG;
import static org.slf4j.event.Level.WARN;

/**
 * Task implementations should not reference internal Gradle APIs.
 */
public class TaskImplementationReferencesInternalApi extends ExternalSubtypeAnalysis {
    public TaskImplementationReferencesInternalApi() {
        super("Lorg/gradle/api/Task");
    }

    @Override
    protected void analyzeType(IClass type, AnalysisContext context) {
        ReferenceCollector referenceCollector = new ReferenceCollector(context);
        checkHierarchy(type, referenceCollector.forTypeHierarchy(type));

        if (type.getClassInitializer() != null) {
            analyzeMethod(context, type.getClassInitializer(), referenceCollector);
        }
        type.getDeclaredMethods()
            .forEach(method -> {
                analyzeMethod(context, method, referenceCollector);
            });

        referenceCollector.references.forEach(reference -> context.report(WARN, reference));
    }

    private static void analyzeMethod(AnalysisContext context, IMethod method, ReferenceCollector referenceCollector) {
        ReferenceCollector.Recorder declarationRecorder = referenceCollector.forMethodDeclaration(method);
        declarationRecorder.recordReference(method.getReturnType());
        for (int iParam = 0; iParam < method.getNumberOfParameters(); iParam++) {
            declarationRecorder.recordReference(method.getParameterType(iParam));
        }
        getDeclaredExceptions(method).forEach(declarationRecorder::recordReference);

        ReferenceCollector.Recorder bodyRecorder = referenceCollector.forMethodBody(method);
        WalaUtil.instructions(method)
            .forEach(instruction -> recordReferencedTypes(context, instruction, bodyRecorder));
    }

    private static Stream<TypeReference> getDeclaredExceptions(IMethod method) {
        try {
            return Stream.ofNullable(method.getDeclaredExceptions()).flatMap(Arrays::stream);
        } catch (InvalidClassFileException e) {
            throw new RuntimeException(e);
        }
    }

    private void checkHierarchy(IClass baseType, ReferenceCollector.Recorder recorder) {
        var queue = new ArrayDeque<IClass>();
        var seen = new HashSet<IClass>();
        queue.add(baseType);
        while (true) {
            IClass type = queue.poll();
            if (type == null) {
                break;
            }
            directSuperTypes(type)
                .forEach(superType -> {
                    switch (TypeOrigin.of(superType)) {
                        case PUBLIC:
                            // Ignore referenced public types and their supertypes
                            break;
                        case INTERNAL:
                            // Report referenced internal type
                            recorder.recordReference(superType);
                            break;
                        default:
                            // Visit external supertype
                            if (seen.add(superType)) {
                                queue.add(superType);
                            }
                            break;
                    }
                });
        }
    }

    private static Stream<IClass> directSuperTypes(IClass type) {
        return Stream.concat(
            Stream.ofNullable(type.getSuperclass()),
            type.getDirectInterfaces().stream());
    }

    private static void recordReferencedTypes(AnalysisContext context, IInstruction instruction, ReferenceCollector.Recorder recorder) {
        instruction.visit(new Visitor() {
            @Override
            public void visitConstant(ConstantInstruction instruction) {
                recorder.recordReference(instruction.getType());
                if (instruction.getValue() instanceof ClassToken token) {
                    recorder.recordReference(token.getTypeName());
                }
            }

            @Override
            public void visitLocalLoad(ILoadInstruction instruction) {
                recorder.recordReference(instruction.getType());
            }

            @Override
            public void visitLocalStore(IStoreInstruction instruction) {
                recorder.recordReference(instruction.getType());
            }

            @Override
            public void visitGoto(GotoInstruction instruction) {
                // Doesn't track type
            }

            @Override
            public void visitArrayLoad(IArrayLoadInstruction instruction) {
                recorder.recordReference(instruction.getType());
            }

            @Override
            public void visitArrayStore(IArrayStoreInstruction instruction) {
                recorder.recordReference(instruction.getType());
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
                recorder.recordReference(instruction.getType());
            }

            @Override
            public void visitUnaryOp(IUnaryOpInstruction instruction) {
                recorder.recordReference(instruction.getType());
            }

            @Override
            public void visitShift(IShiftInstruction instruction) {
                recorder.recordReference(instruction.getType());
            }

            @Override
            public void visitConversion(IConversionInstruction instruction) {
                recorder.recordReference(instruction.getFromType());
                recorder.recordReference(instruction.getToType());
            }

            @Override
            public void visitComparison(IComparisonInstruction instruction) {
                recorder.recordReference(instruction.getType());
            }

            @Override
            public void visitConditionalBranch(IConditionalBranchInstruction instruction) {
                recorder.recordReference(instruction.getType());
            }

            @Override
            public void visitSwitch(SwitchInstruction instruction) {
                // Doesn't track type
            }

            @Override
            public void visitReturn(ReturnInstruction instruction) {
                recorder.recordReference(instruction.getType());
            }

            @Override
            public void visitGet(IGetInstruction instruction) {
                recorder.recordReference(instruction.getClassType());
                recorder.recordReference(instruction.getFieldType());
            }

            @Override
            public void visitPut(IPutInstruction instruction) {
                recorder.recordReference(instruction.getClassType());
                recorder.recordReference(instruction.getFieldType());
            }

            @Override
            public void visitInvoke(IInvokeInstruction instruction) {
                String invokedTypeName = instruction.getClassType();
                IClass invokedType = context.findClass(invokedTypeName);
                if (invokedType == null) {
                    return;
                }
                recorder.recordReference(invokedTypeName);

                var invokedMethod = context.getHierarchy().resolveMethod(invokedType, Selector.make(instruction.getMethodName() + instruction.getMethodSignature()));
                if (invokedMethod == null) {
                    context.report(DEBUG, "Cannot find method: %s.%s%s".formatted(invokedTypeName, instruction.getMethodName(), instruction.getMethodSignature()));
                } else {
                    // Add the type the method is declared in
                    recorder.recordReference(invokedMethod.getDeclaringClass().getName().toString());

                    // Add return type
                    recorder.recordReference(invokedMethod.getReturnType().getName().toString());

                    // Add parameter types
                    for (int iParam = 0; iParam < invokedMethod.getNumberOfParameters(); iParam++) {
                        recorder.recordReference(invokedMethod.getParameterType(iParam).getName().toString());
                    }
                }
            }

            @Override
            public void visitNew(NewInstruction instruction) {
                recorder.recordReference(instruction.getType());
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
                Arrays.stream(instruction.getTypes()).forEach(recorder::recordReference);
            }

            @Override
            public void visitInstanceof(IInstanceofInstruction instruction) {
                recorder.recordReference(instruction.getType());
            }

            @Override
            public void visitLoadIndirect(ILoadIndirectInstruction instruction) {
                // Doesn't track type
            }

            @Override
            public void visitStoreIndirect(IStoreIndirectInstruction instruction) {
                recorder.recordReference(instruction.getType());
            }
        });
    }

    private static class ReferenceCollector {
        private final AnalysisContext context;
        private final SortedSet<String> references = new TreeSet<>();

        public ReferenceCollector(AnalysisContext context) {
            this.context = context;
        }

        public Recorder forMethodDeclaration(IMethod originMethod) {
            return new Recorder() {
                @Override
                protected String formatReference(TypeReference reference) {
                    return "Method declaration %s references internal Gradle type: %s".formatted(originMethod.getSignature(), reference.getName());
                }
            };
        }

        public Recorder forMethodBody(IMethod originMethod) {
            return new Recorder() {
                @Override
                protected String formatReference(TypeReference reference) {
                    return "Method %s references internal Gradle type: %s".formatted(originMethod.getSignature(), reference.getName());
                }
            };
        }

        public Recorder forTypeHierarchy(IClass baseType) {
            return new Recorder() {
                @Override
                protected String formatReference(TypeReference reference) {
                    return "Type %s extends internal Gradle type: %s".formatted(baseType.getName(), reference.getName());
                }
            };
        }

        public abstract class Recorder {
            public void recordReference(String typeName) {
                TypeReference reference = context.findReference(typeName);
                if (reference != null) {
                    recordReference(reference);
                }
            }

            public void recordReference(TypeReference reference) {
                if (TypeOrigin.of(reference) == TypeOrigin.INTERNAL) {
                    references.add(formatReference(reference));
                }
            }

            public void recordReference(IClass type) {
                recordReference(type.getReference());
            }

            protected abstract String formatReference(TypeReference reference);
        }
    }
}
