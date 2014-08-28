package org.onlab.util;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import java.util.concurrent.ThreadFactory;

public abstract class Tools {

    private Tools() {
    }

    /**
     * Returns a thread factory that produces threads named according to the
     * supplied name pattern.
     *
     * @param pattern name pattern
     * @return thread factory
     */
    public static ThreadFactory namedThreads(String pattern) {
        return new ThreadFactoryBuilder().setNameFormat(pattern).build();
    }

}
