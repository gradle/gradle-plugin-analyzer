package org.gradlex.plugins.analyzer.analysis;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.shrike.shrikeBT.ConstantInstruction;
import com.ibm.wala.shrike.shrikeBT.ConstantInstruction.ClassToken;
import com.ibm.wala.shrike.shrikeBT.IArrayLoadInstruction;
import com.ibm.wala.shrike.shrikeBT.IArrayStoreInstruction;
import com.ibm.wala.shrike.shrikeBT.IBinaryOpInstruction;
import com.ibm.wala.shrike.shrikeBT.IComparisonInstruction;
import com.ibm.wala.shrike.shrikeBT.IConditionalBranchInstruction;
import com.ibm.wala.shrike.shrikeBT.IConversionInstruction;
import com.ibm.wala.shrike.shrikeBT.IGetInstruction;
import com.ibm.wala.shrike.shrikeBT.IInstanceofInstruction;
import com.ibm.wala.shrike.shrikeBT.IInstruction;
import com.ibm.wala.shrike.shrikeBT.IInvokeInstruction;
import com.ibm.wala.shrike.shrikeBT.ILoadInstruction;
import com.ibm.wala.shrike.shrikeBT.IPutInstruction;
import com.ibm.wala.shrike.shrikeBT.IShiftInstruction;
import com.ibm.wala.shrike.shrikeBT.IStoreInstruction;
import com.ibm.wala.shrike.shrikeBT.ITypeTestInstruction;
import com.ibm.wala.shrike.shrikeBT.IUnaryOpInstruction;
import com.ibm.wala.shrike.shrikeBT.NewInstruction;
import com.ibm.wala.shrike.shrikeBT.ReturnInstruction;
import org.gradlex.plugins.analyzer.ExternalSubtypeAnalysis;
import org.gradlex.plugins.analyzer.TypeOrigin;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Objects;
import java.util.stream.Stream;

import static org.gradlex.plugins.analyzer.WalaUtil.instructions;
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
        type.getDeclaredMethods()
            .forEach(method -> instructions(method)
                .flatMap(TaskImplementationReferencesInternalApi::getReferencedTypeNames)
                .map(TaskImplementationReferencesInternalApi::normalizeTypeName)
                .map(context::lookup)
                .filter(Objects::nonNull)
                .filter(TypeOrigin::isInternalGradleApi)
                .forEach(internalType -> context.report(WARN, String.format("Method %s in %s references internal Gradle type: %s",
                    method, type.getName(), internalType.getName()))));
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

    private static Stream<String> getReferencedTypeNames(IInstruction instruction) {
        System.out.println("> " + instruction);
        if (instruction instanceof IArrayLoadInstruction iArrayLoad) {
            return Stream.of(iArrayLoad.getType());
        } else if (instruction instanceof IArrayStoreInstruction iArrayStore) {
            return Stream.of(iArrayStore.getType());
        } else if (instruction instanceof IBinaryOpInstruction iOp) {
            return Stream.of(iOp.getType());
        } else if (instruction instanceof IComparisonInstruction iCmp) {
            return Stream.of(iCmp.getType());
        } else if (instruction instanceof IConditionalBranchInstruction iCond) {
            return Stream.of(iCond.getType());
        } else if (instruction instanceof IConversionInstruction iConv) {
            return Stream.of(iConv.getFromType(), iConv.getToType());
        } else if (instruction instanceof IGetInstruction iGet) {
            return Stream.of(iGet.getClassType(), iGet.getFieldType());
        } else if (instruction instanceof IInstanceofInstruction iInst) {
            return Stream.of(iInst.getType());
        } else if (instruction instanceof IInvokeInstruction iInvoke) {
            return Stream.of(iInvoke.getClassType());
        } else if (instruction instanceof ILoadInstruction iLoad) {
            return Stream.of(iLoad.getType());
        } else if (instruction instanceof IPutInstruction iPut) {
            return Stream.of(iPut.getClassType(), iPut.getFieldType());
        } else if (instruction instanceof IShiftInstruction iShift) {
            return Stream.of(iShift.getType());
        } else if (instruction instanceof IStoreInstruction iStore) {
            return Stream.of(iStore.getType());
        } else if (instruction instanceof ITypeTestInstruction iTest) {
            return Stream.of(iTest.getTypes());
        } else if (instruction instanceof IUnaryOpInstruction iOp) {
            return Stream.of(iOp.getType());
        } else if (instruction instanceof ConstantInstruction iConst) {
            Object value = iConst.getValue();
            if (value instanceof ClassToken iToken) {
                return Stream.of(iToken.getTypeName());
            } else {
                return Stream.empty();
            }
        } else if (instruction instanceof NewInstruction iNew) {
            return Stream.of(iNew.getType());
        } else if (instruction instanceof ReturnInstruction iRet) {
            return Stream.of(iRet.getType());
        } else {
            return Stream.empty();
        }
    }

    private static String normalizeTypeName(String typeName) {
        return typeName.endsWith(";")
            ? typeName.substring(0, typeName.length() - 1)
            : typeName;
    }
}
