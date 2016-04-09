/*
 * Copyright 2014-present Open Networking Laboratory
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
package org.onlab.junit;

import com.google.common.collect.ImmutableList;
import com.google.common.io.Files;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.List;
import java.util.Random;

import static com.google.common.base.Preconditions.checkArgument;
import static org.junit.Assert.fail;

/**
 * Utilities to aid in producing JUnit tests.
 */
public final class TestTools {

    private static final Random RANDOM = new Random();

    // Prohibit construction
    private TestTools() {
    }

    public static void print(String msg) {
        System.out.print(msg);
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
            fail("test interrupted");
        }
    }

    /**
     * Returns the current time in millis since epoch.
     *
     * @return current time
     */
    public static long now() {
        return System.currentTimeMillis();
    }

    /**
     * Runs the specified runnable until it completes successfully or until the
     * specified time expires. If the latter occurs, the first encountered
     * assertion on the last attempt will be re-thrown. Errors other than
     * assertion errors will be propagated immediately.
     * <p>
     * Assertions attempts will not be closer than 10 millis apart and no
     * further than 50 millis.
     * </p>
     *
     * @param delay      number of millis to delay before the first attempt
     * @param duration   number of milliseconds beyond the current time
     * @param assertions test assertions runnable
     */
    public static void assertAfter(int delay, int duration, Runnable assertions) {
        checkArgument(delay < duration, "delay >= duration");
        long start = now();
        int step = Math.max(Math.min((duration - delay) / 100, 50), 10);

        // Is there an initial delay?
        if (delay > 0) {
            delay(delay);
        }

        // Keep going until the assertions succeed or until time runs-out.
        while (true) {
            try {
                assertions.run();
                break;
            } catch (AssertionError e) {
                // If there was an error and time ran out, re-throw it.
                if (now() - start > duration) {
                    throw e;
                }
            }
            delay(step);
        }
    }

    /**
     * Runs the specified runnable until it completes successfully or until the
     * specified time expires. If the latter occurs, the first encountered
     * assertion on the last attempt will be re-thrown. Errors other than
     * assertion errors will be propagated immediately.
     * <p>
     * Assertions attempts will not be closer than 10 millis apart and no
     * further than 50 millis.
     * </p>
     *
     * @param duration   number of milliseconds beyond the current time
     * @param assertions test assertions runnable
     */
    public static void assertAfter(int duration, Runnable assertions) {
        assertAfter(0, duration, assertions);
    }


    /**
     * Creates a directory tree of test files. To signify creating a directory
     * file path should end with '/'.
     *
     * @param paths list of file paths
     * @return list of created files
     * @throws java.io.IOException if there is an issue
     */
    public static List<File> createTestFiles(List<String> paths) throws IOException {
        return createTestFiles(paths, 32, 1024);
    }

    /**
     * Creates a directory tree of test files. To signify creating a directory
     * file path should end with '/'.
     *
     * @param paths   list of file paths
     * @param minSize minimum file size in bytes
     * @param maxSize maximum file size in bytes
     * @return list of created files
     * @throws java.io.IOException if there is an issue
     */
    public static List<File> createTestFiles(List<String> paths,
                                             int minSize, int maxSize) throws IOException {
        ImmutableList.Builder<File> files = ImmutableList.builder();
        for (String p : paths) {
            File f = new File(p);
            if (p.endsWith("/")) {
                if (f.mkdirs()) {
                    files.add(f);
                }
            } else {
                Files.createParentDirs(f);
                if (f.createNewFile()) {
                    writeRandomFile(f, minSize, maxSize);
                    files.add(f);
                }
            }
        }
        return files.build();
    }

    /**
     * Writes random binary content into the specified file. The number of
     * bytes will be random between the given minimum and maximum.
     *
     * @param file    file to write data to
     * @param minSize minimum number of bytes to write
     * @param maxSize maximum number of bytes to write
     * @throws IOException if there is an issue
     */
    public static void writeRandomFile(File file, int minSize, int maxSize) throws IOException {
        int size = minSize + (minSize == maxSize ? 0 : RANDOM.nextInt(maxSize - minSize));
        byte[] data = new byte[size];
        tweakBytes(RANDOM, data, size / 4);
        Files.write(data, file);
    }


    /**
     * Tweaks the given number of bytes in a byte array.
     *
     * @param random random number generator
     * @param data   byte array to be tweaked
     * @param count  number of bytes to tweak
     */
    public static void tweakBytes(Random random, byte[] data, int count) {
        tweakBytes(random, data, count, 0, data.length);
    }

    /**
     * Tweaks the given number of bytes in the specified range of a byte array.
     *
     * @param random random number generator
     * @param data   byte array to be tweaked
     * @param count  number of bytes to tweak
     * @param start  index at beginning of range (inclusive)
     * @param end    index at end of range (exclusive)
     */
    public static void tweakBytes(Random random, byte[] data, int count,
                                  int start, int end) {
        int len = end - start;
        for (int i = 0; i < count; i++) {
            data[start + random.nextInt(len)] = (byte) random.nextInt();
        }
    }

    /*
     * Finds an available port that a test can bind to.
     */
    public static int findAvailablePort(int defaultPort) {
        try {
            ServerSocket socket = new ServerSocket(0);
            socket.setReuseAddress(true);
            int port = socket.getLocalPort();
            socket.close();
            return port;
        } catch (IOException ex) {
            return defaultPort;
        }
    }


}
