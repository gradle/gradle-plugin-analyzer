package org.gradlex.plugins.analyzer.analysis;

import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableList;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMember;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.shrike.shrikeBT.IInstruction;
import com.ibm.wala.shrike.shrikeBT.IInvokeInstruction;
import com.ibm.wala.shrike.shrikeBT.IInvokeInstruction.Dispatch;
import com.ibm.wala.shrike.shrikeBT.ILoadInstruction;
import com.ibm.wala.shrike.shrikeBT.ReturnInstruction;
import com.ibm.wala.types.TypeReference;
import org.gradlex.plugins.analyzer.Analysis;
import org.gradlex.plugins.analyzer.TypeOrigin;
import org.gradlex.plugins.analyzer.WalaUtil;
import org.slf4j.event.Level;

import java.util.ArrayDeque;
import java.util.Objects;
import java.util.Optional;
import java.util.Queue;
import java.util.function.Predicate;
import java.util.stream.Stream;

public abstract class AbstractMethodOverrideAnalysis implements Analysis {
    private final String methodType;
    private final Predicate<? super IMethod> filter;

    public AbstractMethodOverrideAnalysis(String methodType, Predicate<? super IMethod> filter) {
        this.methodType = methodType;
        this.filter = filter;
    }

    @Override
    public void analyzeType(IClass type, AnalysisContext context) {
        type.getDeclaredMethods().stream()
            // Ignore bridge methods
            .filter(Predicate.not(IMethod::isBridge))
            // Looking for instance methods
            .filter(Predicate.not(IMember::isStatic))
            // Looking for public or protected methods
            .filter(method -> method.isPublic() || method.isProtected())
            // Matching our filter
            .filter(filter)
            // Walk ancestry
            .forEach(method -> Stream.iterate(type.getSuperclass(), Objects::nonNull, IClass::getSuperclass)
                // We only care about methods that come from the Gradle API
                .filter(TypeOrigin::isGradleApi)
                // Find the same method in the superclass
                .flatMap(clazz -> Stream.ofNullable(clazz.getMethod(method.getSelector())))
                .findFirst()
                .ifPresent(overriddenMethod -> reportOverriddenMethod(context, type, method, overriddenMethod))
            );
    }

    private void reportOverriddenMethod(AnalysisContext context, IClass type, IMethod method, IMethod overriddenMethod) {
        try {
            InstructionQueue queue = new InstructionQueue(WalaUtil.instructions(method));

            // Check if dynamic Groovi
            // Invoke(STATIC,<type>;,$getCallSiteArray,()[Lorg/codehaus/groovy/runtime/callsite/CallSite;)
            queue.takeNextIf(IInvokeInstruction.class, invokeCallSiteArray ->
                    invokeCallSiteArray.getInvocationCode() == Dispatch.STATIC
                    && invokeCallSiteArray.getMethodName().equals("$getCallSiteArray"))
                .ifPresentOrElse(
                    invokeInstruction -> context.report(Level.WARN, String.format("The dynamic Groovy %s %s() in %s overrides Gradle API from %s",
                        methodType, method.getName(), type.getName(), overriddenMethod.getDeclaringClass().getName())),
                    () -> {
                        checkJavaInstructions(method, queue);
                        context.report(Level.INFO, String.format("The %s %s() in %s overrides Gradle API from %s, but calls only super()",
                            methodType, method.getName(), type.getName(), overriddenMethod.getDeclaringClass().getName()));
                    }
                );
        } catch (AnalysisException ex) {
            // TODO Report the custom code in some form
            context.report(Level.WARN, String.format("The %s %s() in %s overrides Gradle API from %s with custom logic",
                methodType, method.getName(), type.getName(), overriddenMethod.getDeclaringClass().getName()));
        }
    }

    private static void checkJavaInstructions(IMethod method, InstructionQueue queue) throws AnalysisException {
        queue.expectNext(ILoadInstruction.class, iLoad ->
            WalaUtil.matchesType(iLoad::getType, TypeReference.JavaLangObject)
            && iLoad.getVarIndex() == 0);

        for (int paremNo = 0; paremNo < method.getNumberOfParameters() - 1; paremNo++) {
            queue.expectNext(ILoadInstruction.class);
        }

        queue.expectNext(IInvokeInstruction.class, iInvoke -> {
            // Remove type prefix + '.'
            String expectedMethodSignature = method.getSignature().substring(method.getDeclaringClass().getName().toString().length());
            String invokedMethodSignature = iInvoke.getMethodName() + iInvoke.getMethodSignature();
            return invokedMethodSignature.equals(expectedMethodSignature)
                && iInvoke.getInvocationCode().equals(Dispatch.SPECIAL);
        });

        queue.expectNext(ReturnInstruction.class);

        queue.expectNoMore();
    }

    private static class InstructionQueue {
        private final Queue<IInstruction> instructions;
        int counter = 0;

        public InstructionQueue(IInstruction... instructions) {
//            Arrays.stream(instructions)
//                .forEach(System.out::println);
            this.instructions = new ArrayDeque<>(ImmutableList.copyOf(instructions));
        }

        public <I extends IInstruction> Optional<I> takeNextIf(Class<I> type) throws AnalysisException {
            return takeNextIf(type, Predicates.alwaysTrue());
        }

        public <I extends IInstruction> Optional<I> takeNextIf(Class<I> type, Predicate<? super I> matcher) throws AnalysisException {
            IInstruction next = instructions.peek();
            if (next == null) {
                throw new AnalysisException("No more instruction after #%d", counter);
            }
            if (type.isInstance(next)) {
                I typedNext = type.cast(next);
                if (matcher.test(typedNext)) {
                    counter++;
                    instructions.poll();
                    return Optional.of(typedNext);
                }
            }
            return Optional.empty();
        }

        public <I extends IInstruction> I expectNext(Class<I> type) throws AnalysisException {
            return expectNext(type, Predicates.alwaysTrue());
        }

        public <I extends IInstruction> I expectNext(Class<I> type, Predicate<? super I> matcher) throws AnalysisException {
            IInstruction next = instructions.poll();
            if (next == null) {
                throw new AnalysisException("No more instruction after #%d", counter);
            }
            counter++;
            if (!type.isInstance(next)) {
                throw new AnalysisException("Instruction #%d expected to be %s but it was %s",
                    counter, type.getSimpleName(), next);
            }
            I typedNext = type.cast(next);
            if (!matcher.test(typedNext)) {
                throw new AnalysisException("Instruction #%d (%s) had unexpected parameters: %s", counter, type.getSimpleName(), next);
            }
            return typedNext;
        }

        public void expectNoMore() throws AnalysisException {
            if (!instructions.isEmpty()) {
                throw new AnalysisException("Expected no more instructions after #%d but there are %d more", counter, instructions.size());
            }
        }
    }

    private static class AnalysisException extends RuntimeException {
        public AnalysisException(String message) {
            super(message);
        }

        public AnalysisException(String message, Object... params) {
            this(String.format(message, params));
        }
    }
}
