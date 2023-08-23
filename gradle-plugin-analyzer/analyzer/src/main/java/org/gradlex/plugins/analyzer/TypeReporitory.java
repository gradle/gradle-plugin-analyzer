package org.gradlex.plugins.analyzer;

import com.google.common.collect.ImmutableList;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IField;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.types.Selector;
import com.ibm.wala.types.TypeReference;
import org.gradlex.plugins.analyzer.analysis.TypeReferenceWalker;
import org.gradlex.plugins.analyzer.analysis.TypeReferenceWalker.ReferenceVisitor;
import org.gradlex.plugins.analyzer.analysis.TypeReferenceWalker.ReferenceVisitorFactory;

import javax.annotation.Nullable;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

public class TypeReporitory {
    private final ClassHierarchy hierarchy;
    private final ConcurrentHashMap<TypeSet, ImmutableList<IClass>> cache = new ConcurrentHashMap<>();

    public TypeReporitory(ClassHierarchy hierarchy) {
        this.hierarchy = hierarchy;
    }

    public ImmutableList<IClass> getTypeSet(TypeSet set) {
        return cache.computeIfAbsent(set, key -> key.load(this, new TypeResolverImpl(hierarchy))
            .collect(ImmutableList.toImmutableList()));
    }

    public enum TypeSet {
        TASK_TYPES {
            @Override
            Stream<IClass> load(TypeReporitory cache, TypeResolver typeResolver) {
                TypeReference taskType = typeResolver.findReference("Lorg/gradle/api/Task");
                return typeResolver.hierarchy().getImplementors(taskType).stream();
            }
        },
        PLUGIN_TYPES {
            @Override
            Stream<IClass> load(TypeReporitory cache, TypeResolver typeResolver) {
                TypeReference pluginType = typeResolver.findReference("Lorg/gradle/api/Plugin");
                return typeResolver.hierarchy().getImplementors(pluginType).stream();
            }
        },
        EXTERNAL_TASK_TYPES {
            @Override
            Stream<IClass> load(TypeReporitory cache, TypeResolver typeResolver) {
                return cache.getTypeSet(TASK_TYPES).stream()
                    .filter(TypeOrigin::isExternal);
            }
        },
        EXTERNAL_PLUGIN_TYPES {
            @Override
            Stream<IClass> load(TypeReporitory cache, TypeResolver typeResolver) {
                return cache.getTypeSet(PLUGIN_TYPES).stream()
                    .filter(TypeOrigin::isExternal);
            }
        },
        ALL_EXTERNAL_TYPES {
            @Override
            Stream<IClass> load(TypeReporitory cache, TypeResolver typeResolver) {
                return Stream.concat(
                    cache.getTypeSet(EXTERNAL_TASK_TYPES).stream(),
                    cache.getTypeSet(EXTERNAL_PLUGIN_TYPES).stream()
                );
            }
        },
        ALL_EXTERNAL_REFERENCED_TYPES {
            @Override
            Stream<IClass> load(TypeReporitory cache, TypeResolver typeResolver) {
                ImmutableList<IClass> allExternalTypes = cache.getTypeSet(ALL_EXTERNAL_TYPES);
                var queue = new ArrayDeque<>(allExternalTypes);
                var seenTypes = new HashSet<>(allExternalTypes);

                ReferenceVisitor visitor = new ReferenceVisitor() {
                    @Nullable
                    @Override
                    protected TypeReference findReference(String typeName) {
                        return typeResolver.findReference(typeName);
                    }

                    @Override
                    public void visitReference(TypeReference reference) {
                        IClass type = typeResolver.findClass(reference);
                        if (type != null) {
                            if (seenTypes.add(type)) {
                                queue.add(type);
                            }
                        }
                    }

                    @Override
                    public void visitMethodReference(String typeName, String methodName, String methodSignature) {
                        IClass type = typeResolver.findClass(typeName);
                        if (type != null) {
                            IMethod method = typeResolver.hierarchy().resolveMethod(type, Selector.make(methodName + methodSignature));
                            if (method != null) {
                                visitReference(type);
                                visitReference(method.getReturnType());
                                for (int iParam = 0; iParam < method.getNumberOfParameters(); iParam++) {
                                    visitReference(method.getParameterType(iParam));
                                }
                            }
                        }
                    }
                };
                ReferenceVisitorFactory visitorFactory = new ReferenceVisitorFactory() {
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
                    public ReferenceVisitor forMethodBody(IMethod originMethod) {
                        return visitor;
                    }

                    @Override
                    public ReferenceVisitor forMethodAnnotations(IMethod method) {
                        return visitor;
                    }
                };

                while (true) {
                    IClass type = queue.poll();
                    if (type == null) {
                        break;
                    }
                    TypeReferenceWalker.walkReferences(type, visitorFactory);
                }
                return seenTypes.stream();
            }
        };

        abstract Stream<IClass> load(TypeReporitory cache, TypeResolver typeResolver);
    }
}
