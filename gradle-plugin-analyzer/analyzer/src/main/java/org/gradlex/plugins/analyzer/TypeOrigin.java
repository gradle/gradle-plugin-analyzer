package org.gradlex.plugins.analyzer;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableList;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.types.TypeName;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public enum TypeOrigin {
    PUBLIC(true),
    INTERNAL(true),
    RUNTIME(false),
    EXTERNAL(false);

    private static final List<Pattern> PUBLIC_PACKAGES = Stream.of(
            "org/gradle/*",
            "org/gradle/api/**",
            "org/gradle/authentication/**",
            "org/gradle/build/**",
            "org/gradle/buildinit/**",
            "org/gradle/caching/**",
            "org/gradle/concurrent/**",
            "org/gradle/deployment/**",
            "org/gradle/env/**",
            "org/gradle/external/javadoc/**",
            "org/gradle/ide/**",
            "org/gradle/includedbuild/**",
            "org/gradle/ivy/**",
            "org/gradle/jvm/**",
            "org/gradle/language/**",
            "org/gradle/maven/**",
            "org/gradle/nativeplatform/**",
            "org/gradle/normalization/**",
            "org/gradle/platform/**",
            "org/gradle/play/**",
            "org/gradle/plugin/devel/**",
            "org/gradle/plugin/repository/*",
            "org/gradle/plugin/use/*",
            "org/gradle/plugin/management/*",
            "org/gradle/plugins/**",
            "org/gradle/process/**",
            "org/gradle/testfixtures/**",
            "org/gradle/testing/jacoco/**",
            "org/gradle/tooling/**",
            "org/gradle/swiftpm/**",
            "org/gradle/model/**",
            "org/gradle/testkit/**",
            "org/gradle/testing/**",
            "org/gradle/vcs/**",
            "org/gradle/work/**",
            "org/gradle/workers/**",
            "org/gradle/util/**"
        ).map(TypeOrigin::toPackagePattern)
        .collect(ImmutableList.toImmutableList());

    private static final List<Pattern> INTERNAL_PACKAGES = Stream.of(
            "**/internal/**"
        ).map(TypeOrigin::toPackagePattern)
        .collect(ImmutableList.toImmutableList());
    private final boolean gradleApi;

    private static Pattern toPackagePattern(String packageGlob) {
        return Pattern.compile("L" + packageGlob
            .replaceAll("\\*\\*", "###")
            .replaceAll("/\\*", "/[A-Z][a-z_A-Z0-9]+")
            .replaceAll("###", ".*?")
        );
    }

    private static final LoadingCache<TypeName, TypeOrigin> CACHE = CacheBuilder.newBuilder()
        .build(new CacheLoader<>() {
            @Nonnull
            @Override
            public TypeOrigin load(TypeName type) {
                String className = type.toString();
                if (className.startsWith("Lorg/gradle/") || className.startsWith("Lnet/rubygrapefruit/")) {
                    if (INTERNAL_PACKAGES.stream().noneMatch(pattern -> matches(pattern, className))
                        && PUBLIC_PACKAGES.stream().anyMatch(pattern -> matches(pattern, className))) {
                        return PUBLIC;
                    } else {
                        return INTERNAL;
                    }
                } else if (
                    className.startsWith("Ljava/") || className.startsWith("Ljavax/") || className.startsWith("Ljdk/")
                    || className.startsWith("Lgroovy/") || className.startsWith("Lorg/codehaus/groovy/")
                    || className.startsWith("Lkotlin/")
                ) {
                    return RUNTIME;
                } else {
                    return EXTERNAL;
                }
            }
        });

    private static boolean matches(Pattern pattern, String packageName) {
        return pattern.matcher(packageName).matches();
    }

    static TypeOrigin of(TypeName type) {
        return CACHE.getUnchecked(type);
    }

    public static TypeOrigin of(IClass clazz) {
        return of(clazz.getName());
    }

    public boolean isGradleApi() {
        return gradleApi;
    }

    TypeOrigin(boolean gradleApi) {
        this.gradleApi = gradleApi;
    }
}
