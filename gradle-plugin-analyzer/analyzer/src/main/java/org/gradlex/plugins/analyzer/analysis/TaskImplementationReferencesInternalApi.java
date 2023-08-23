package org.gradlex.plugins.analyzer.analysis;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.AnalysisCache;
import com.ibm.wala.ipa.callgraph.AnalysisCacheImpl;
import com.ibm.wala.shrike.shrikeBT.ArrayLengthInstruction;
import com.ibm.wala.shrike.shrikeBT.ConstantInstruction;
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
import com.ibm.wala.ssa.IRFactory;
import com.ibm.wala.types.Selector;
import org.gradlex.plugins.analyzer.ExternalSubtypeAnalysis;
import org.gradlex.plugins.analyzer.TypeOrigin;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Objects;
import java.util.stream.Stream;

import static org.gradlex.plugins.analyzer.WalaUtil.instructions;
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
        checkHierarchy(type, context);

        AnalysisCache cache = new AnalysisCacheImpl();
        IRFactory<IMethod> factory = cache.getIRFactory();

        type.getDeclaredMethods()
            .forEach(method -> instructions(method)
                .flatMap(instruction -> getReferencedTypeNames(context, instruction))
                .map(context::findClass)
                .filter(Objects::nonNull)
                .filter(TypeOrigin::isInternalGradleApi)
                .distinct()
                .sorted(Comparator.comparing(internalType -> internalType.getName().toString()))
                .forEach(internalType -> context.report(WARN, String.format("Method %s references internal Gradle type: %s",
                    method.getSignature(), internalType.getName()))));
    }

    private void checkHierarchy(IClass baseType, AnalysisContext context) {
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
                            // Ignore referenced public types
                            break;
                        case INTERNAL:
                            // Report referenced internal type
                            context.report(WARN, String.format("Type %s extends internal Gradle API %s", type.getName(), superType.getName()));
                            break;
                        default:
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

    private static Stream<String> getReferencedTypeNames(AnalysisContext context, IInstruction instruction) {
        var types = Stream.<String>builder();
        instruction.visit(new Visitor() {
            @Override
            public void visitConstant(ConstantInstruction instruction) {
                types.add(instruction.getType());
            }

            @Override
            public void visitLocalLoad(ILoadInstruction instruction) {
                types.add(instruction.getType());
            }

            @Override
            public void visitLocalStore(IStoreInstruction instruction) {
                types.add(instruction.getType());
            }

            @Override
            public void visitGoto(GotoInstruction instruction) {
                // Doesn't track type
            }

            @Override
            public void visitArrayLoad(IArrayLoadInstruction instruction) {
                types.add(instruction.getType());
            }

            @Override
            public void visitArrayStore(IArrayStoreInstruction instruction) {
                types.add(instruction.getType());
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
                types.add(instruction.getType());
            }

            @Override
            public void visitUnaryOp(IUnaryOpInstruction instruction) {
                types.add(instruction.getType());
            }

            @Override
            public void visitShift(IShiftInstruction instruction) {
                types.add(instruction.getType());
            }

            @Override
            public void visitConversion(IConversionInstruction instruction) {
                types.add(instruction.getFromType());
                types.add(instruction.getToType());
            }

            @Override
            public void visitComparison(IComparisonInstruction instruction) {
                types.add(instruction.getType());
            }

            @Override
            public void visitConditionalBranch(IConditionalBranchInstruction instruction) {
                types.add(instruction.getType());
            }

            @Override
            public void visitSwitch(SwitchInstruction instruction) {
                // Doesn't track type
            }

            @Override
            public void visitReturn(ReturnInstruction instruction) {
                types.add(instruction.getType());
            }

            @Override
            public void visitGet(IGetInstruction instruction) {
                types.add(instruction.getClassType());
                types.add(instruction.getFieldType());
            }

            @Override
            public void visitPut(IPutInstruction instruction) {
                types.add(instruction.getClassType());
                types.add(instruction.getFieldType());
            }

            @Override
            public void visitInvoke(IInvokeInstruction instruction) {
                String invokedTypeName = instruction.getClassType();
                IClass invokedType = context.findClass(invokedTypeName);
                if (invokedType == null) {
                    return;
                }
                types.add(invokedTypeName);

                var invokedMethod = context.getHierarchy().resolveMethod(invokedType, Selector.make(instruction.getMethodName() + instruction.getMethodSignature()));
                if (invokedMethod == null) {
                    context.report(DEBUG, "Cannot find method: %s.%s%s".formatted(invokedTypeName, instruction.getMethodName(), instruction.getMethodSignature()));
                } else {
                    // Add the type the method is declared in
                    types.add(invokedMethod.getDeclaringClass().getName().toString());

                    // Add return type
                    types.add(invokedMethod.getReturnType().getName().toString());

                    // Add parameter types
                    for (int iParam = 0; iParam < invokedMethod.getNumberOfParameters(); iParam++) {
                        types.add(invokedMethod.getParameterType(iParam).getName().toString());
                    }
                }
            }

            @Override
            public void visitNew(NewInstruction instruction) {
                types.add(instruction.getType());
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
                Arrays.stream(instruction.getTypes()).forEach(types::add);
            }

            @Override
            public void visitInstanceof(IInstanceofInstruction instruction) {
                types.add(instruction.getType());
            }

            @Override
            public void visitLoadIndirect(ILoadIndirectInstruction instruction) {
                // Doesn't track type
            }

            @Override
            public void visitStoreIndirect(IStoreIndirectInstruction instruction) {
                types.add(instruction.getType());
            }
        });
        return types.build();
    }
}
