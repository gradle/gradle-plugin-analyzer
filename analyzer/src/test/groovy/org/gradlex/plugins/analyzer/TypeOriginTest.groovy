package org.gradlex.plugins.analyzer

import com.ibm.wala.types.TypeName
import spock.lang.Specification

import static org.gradlex.plugins.analyzer.TypeOrigin.EXTERNAL
import static org.gradlex.plugins.analyzer.TypeOrigin.INTERNAL
import static org.gradlex.plugins.analyzer.TypeOrigin.PUBLIC

class TypeOriginTest extends Specification {
    def "identifies packages correctly"() {
        expect:
        originOf("Lorg/gradle/BuildListener") == PUBLIC
        originOf("Lorg/gradle/api/Action") == PUBLIC
        originOf("Lorg/gradle/api/Task") == PUBLIC
        originOf("Lorg/gradle/api/DefaultTask") == PUBLIC
        originOf("Lorg/gradle/api/internal/AbstractTask") == INTERNAL
        originOf("Lorg/gradle/caching/BuildCacheKey") == PUBLIC
        originOf("Lorg/gradle/caching/internal/DefaultBuildCacheKey") == INTERNAL
        originOf("Lorg/gradle/cache/Cache") == INTERNAL
        originOf("Lcom/google/common/collect/ImmutableList") == EXTERNAL
    }

    private static TypeOrigin originOf(String type) {
        return TypeOrigin.of(TypeName.findOrCreate(type))
    }
}
