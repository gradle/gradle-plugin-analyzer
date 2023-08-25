package org.gradlex.plugins.analyzer;

import org.slf4j.event.Level;

import java.util.Arrays;

public interface Reporter {
    void report(Level level, String message, Object... args);

    record Message(String fmt, Object... args) {
        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            Message message = (Message) o;

            if (!fmt.equals(message.fmt)) {
                return false;
            }
            return Arrays.equals(args, message.args);
        }

        @Override
        public int hashCode() {
            int result = fmt.hashCode();
            result = 31 * result + Arrays.hashCode(args);
            return result;
        }
    }

    default void report(Level level, Message message) {
        report(level, message.fmt, message.args);
    }
}
