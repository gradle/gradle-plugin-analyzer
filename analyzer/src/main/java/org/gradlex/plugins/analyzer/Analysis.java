package org.gradlex.plugins.analyzer;

import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.types.TypeReference;
import org.slf4j.Logger;
import org.slf4j.spi.LoggingEventBuilder;

public interface Analysis {
    void execute(AnalysisContext context);

    enum Severity {
        INFO {
            @Override
            LoggingEventBuilder log(Logger logger) {
                return logger.atInfo();
            }
        },
        WARNING {
            @Override
            LoggingEventBuilder log(Logger logger) {
                return logger.atWarn();
            }
        },
        ERROR {
            @Override
            LoggingEventBuilder log(Logger logger) {
                return logger.atError();
            }
        };

        abstract LoggingEventBuilder log(Logger logger);
    }

    interface AnalysisContext {
        ClassHierarchy getHierarchy();

        TypeReference typeReference(String name);

        void report(Severity severity, String message, Object... args);
    }
}
