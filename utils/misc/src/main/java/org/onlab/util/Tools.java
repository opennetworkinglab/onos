/*
 * Copyright 2014 Open Networking Laboratory
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onlab.util;

import static org.slf4j.LoggerFactory.getLogger;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.Thread.UncaughtExceptionHandler;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadFactory;

import org.slf4j.Logger;

import com.google.common.base.Strings;
import com.google.common.primitives.UnsignedLongs;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

public abstract class Tools {

    private Tools() {
    }

    private static final Logger TOOLS_LOG = getLogger(Tools.class);

    /**
     * Returns a thread factory that produces threads named according to the
     * supplied name pattern.
     *
     * @param pattern name pattern
     * @return thread factory
     */
    public static ThreadFactory namedThreads(String pattern) {
        return new ThreadFactoryBuilder()
                .setNameFormat(pattern)
                // FIXME remove UncaughtExceptionHandler before release
                .setUncaughtExceptionHandler(new UncaughtExceptionHandler() {

                    @Override
                    public void uncaughtException(Thread t, Throwable e) {
                        TOOLS_LOG.error("Uncaught exception on {}", t.getName(), e);
                    }
                }).build();
    }

    /**
     * Returns a thread factory that produces threads with MIN_PRIORITY.
     *
     * @param factory backing ThreadFactory
     * @return thread factory
     */
    public static ThreadFactory minPriority(ThreadFactory factory) {
        return new ThreadFactoryBuilder()
                    .setThreadFactory(factory)
                    .setPriority(Thread.MIN_PRIORITY)
                    .build();
    }

    /**
     * Converts a string from hex to long.
     *
     * @param string hex number in string form; sans 0x
     * @return long value
     */
    public static long fromHex(String string) {
        return UnsignedLongs.parseUnsignedLong(string, 16);
    }

    /**
     * Converts a long value to hex string; 16 wide and sans 0x.
     *
     * @param value long value
     * @return hex string
     */
    public static String toHex(long value) {
        return Strings.padStart(UnsignedLongs.toString(value, 16), 16, '0');
    }

    /**
     * Converts a long value to hex string; 16 wide and sans 0x.
     *
     * @param value long value
     * @param width string width; zero padded
     * @return hex string
     */
    public static String toHex(long value, int width) {
        return Strings.padStart(UnsignedLongs.toString(value, 16), width, '0');
    }

    /**
     * Suspends the current thread for a specified number of millis.
     *
     * @param ms number of millis
     */
    public static void delay(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted", e);
        }
    }

    /**
     * Slurps the contents of a file into a list of strings, one per line.
     *
     * @param path file path
     * @return file contents
     */
    public static List<String> slurp(File path) {
        try {
            BufferedReader br = new BufferedReader(
                new InputStreamReader(new FileInputStream(path), StandardCharsets.UTF_8));

            List<String> lines = new ArrayList<>();
            String line;
            while ((line = br.readLine()) != null) {
                lines.add(line);
            }
            return lines;

        } catch (IOException e) {
            return null;
        }
    }

}
