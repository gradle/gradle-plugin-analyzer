package org.gradlex.plugins.analyzer;

import org.slf4j.event.Level;

public interface Reporter {
    void report(Level level, String message);
}
