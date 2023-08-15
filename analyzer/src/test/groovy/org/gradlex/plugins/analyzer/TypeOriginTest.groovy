package org.gradlex.plugins.analyzer

import com.ibm.wala.types.TypeName
import spock.lang.Specification

import static org.gradlex.plugins.analyzer.TypeOrigin.EXTERNAL
import static org.gradlex.plugins.analyzer.TypeOrigin.INTERNAL
import static org.gradlex.plugins.analyzer.TypeOrigin.PUBLIC

class TypeOriginTest extends Specification {
    def "identifies packages correctly"() {
        expect:
        originOf("org.gradle.BuildListener") == PUBLIC
        originOf("org.gradle.api.Action") == PUBLIC
        originOf("org.gradle.api.Task") == PUBLIC
        originOf("org.gradle.api.DefaultTask") == PUBLIC
        originOf("org.gradle.api.internal.AbstractTask") == INTERNAL
        originOf("org.gradle.caching.BuildCacheKey") == PUBLIC
        originOf("org.gradle.caching.internal.DefaultBuildCacheKey") == INTERNAL
        originOf("org.gradle.cache.Cache") == INTERNAL
        originOf("com.google.common.collect.ImmutableList") == EXTERNAL
    }

    private static TypeOrigin originOf(String type) {
        return TypeOrigin.of(TypeName.findOrCreate(type))
    }
}
