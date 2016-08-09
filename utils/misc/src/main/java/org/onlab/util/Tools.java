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
package org.onlab.util;

import com.google.common.base.Charsets;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.primitives.UnsignedLongs;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.Collection;
import java.util.Dictionary;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static java.nio.file.Files.delete;
import static java.nio.file.Files.walkFileTree;
import static org.onlab.util.GroupedThreadFactory.groupedThreadFactory;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Miscellaneous utility methods.
 */
public abstract class Tools {

    private Tools() {
    }

    private static final Logger log = getLogger(Tools.class);

    private static Random random = new Random();

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
                .setUncaughtExceptionHandler((t, e) -> log.error("Uncaught exception on " + t.getName(), e))
                .build();
    }

    /**
     * Returns a thread factory that produces threads named according to the
     * supplied name pattern and from the specified thread-group. The thread
     * group name is expected to be specified in slash-delimited format, e.g.
     * {@code onos/intent}. The thread names will be produced by converting
     * the thread group name into dash-delimited format and pre-pended to the
     * specified pattern.
     *
     * @param groupName group name in slash-delimited format to indicate hierarchy
     * @param pattern   name pattern
     * @return thread factory
     */
    public static ThreadFactory groupedThreads(String groupName, String pattern) {
        return groupedThreads(groupName, pattern, log);
    }

    /**
     * Returns a thread factory that produces threads named according to the
     * supplied name pattern and from the specified thread-group. The thread
     * group name is expected to be specified in slash-delimited format, e.g.
     * {@code onos/intent}. The thread names will be produced by converting
     * the thread group name into dash-delimited format and pre-pended to the
     * specified pattern. If a logger is specified, it will use the logger to
     * print out the exception if it has any.
     *
     * @param groupName group name in slash-delimited format to indicate hierarchy
     * @param pattern   name pattern
     * @param logger    logger
     * @return thread factory
     */
    public static ThreadFactory groupedThreads(String groupName, String pattern, Logger logger) {
        if (logger == null) {
            return groupedThreads(groupName, pattern);
        }
        return new ThreadFactoryBuilder()
                .setThreadFactory(groupedThreadFactory(groupName))
                .setNameFormat(groupName.replace(GroupedThreadFactory.DELIMITER, "-") + "-" + pattern)
                .setUncaughtExceptionHandler((t, e) -> logger.error("Uncaught exception on " + t.getName(), e))
                .build();
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
     * Returns a thread factory that produces threads with MAX_PRIORITY.
     *
     * @param factory backing ThreadFactory
     * @return thread factory
     */
    public static ThreadFactory maxPriority(ThreadFactory factory) {
        return new ThreadFactoryBuilder()
                .setThreadFactory(factory)
                .setPriority(Thread.MAX_PRIORITY)
                .build();
    }

    /**
     * Returns true if the collection is null or is empty.
     *
     * @param collection collection to test
     * @return true if null or empty; false otherwise
     */
    public static boolean isNullOrEmpty(Collection collection) {
        return collection == null || collection.isEmpty();
    }

    /**
     * Returns the specified item if that item is not null; otherwise throws
     * not found exception.
     *
     * @param item    item to check
     * @param message not found message
     * @param <T>     item type
     * @return item if not null
     * @throws org.onlab.util.ItemNotFoundException if item is null
     */
    public static <T> T nullIsNotFound(T item, String message) {
        if (item == null) {
            throw new ItemNotFoundException(message);
        }
        return item;
    }

    /**
     * Returns the specified set if the set is not null and not empty;
     * otherwise throws a not found exception.
     *
     * @param item set to check
     * @param message not found message
     * @param <T> Set item type
     * @return item if not null and not empty
     * @throws org.onlab.util.ItemNotFoundException if set is null or empty
     */
    public static <T> Set<T> emptyIsNotFound(Set<T> item, String message) {
        if (item == null || item.isEmpty()) {
            throw new ItemNotFoundException(message);
        }
        return item;
    }

    /**
     * Returns the specified item if that item is not null; otherwise throws
     * bad argument exception.
     *
     * @param item    item to check
     * @param message not found message
     * @param <T>     item type
     * @return item if not null
     * @throws IllegalArgumentException if item is null
     */
    public static <T> T nullIsIllegal(T item, String message) {
        if (item == null) {
            throw new IllegalArgumentException(message);
        }
        return item;
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
     * Returns a string encoding in hex of the given long value with prefix
     * '0x'.
     *
     * @param value long value to encode as hex string
     * @return hex string
     */
    public static String toHexWithPrefix(long value) {
        return "0x" + Long.toHexString(value);
    }

    /**
     * Returns the UTF-8 encoded byte[] representation of a String.
     * @param input input string
     * @return UTF-8 encoded byte array
     */
    public static byte[] getBytesUtf8(String input) {
        return input.getBytes(Charsets.UTF_8);
    }

    /**
     * Returns the String representation of UTF-8 encoded byte[].
     * @param input input byte array
     * @return UTF-8 encoded string
     */
    public static String toStringUtf8(byte[] input) {
        return new String(input, Charsets.UTF_8);
    }

    /**
     * Returns a copy of the input byte array.
     *
     * @param original input
     * @return copy of original
     */
    public static byte[] copyOf(byte[] original) {
        return Arrays.copyOf(original, original.length);
    }

    /**
     * Get property as a string value.
     *
     * @param properties   properties to be looked up
     * @param propertyName the name of the property to look up
     * @return value when the propertyName is defined or return null
     */
    public static String get(Dictionary<?, ?> properties, String propertyName) {
        Object v = properties.get(propertyName);
        String s = (v instanceof String) ? (String) v :
                v != null ? v.toString() : null;
        return Strings.isNullOrEmpty(s) ? null : s.trim();
    }

    /**
     * Get Integer property from the propertyName
     * Return null if propertyName is not found.
     *
     * @param properties   properties to be looked up
     * @param propertyName the name of the property to look up
     * @return value when the propertyName is defined or return null
     */
    public static Integer getIntegerProperty(Dictionary<?, ?> properties,
                                             String propertyName) {
        Integer value;
        try {
            String s = get(properties, propertyName);
            value = Strings.isNullOrEmpty(s) ? null : Integer.valueOf(s);
        } catch (NumberFormatException | ClassCastException e) {
            value = null;
        }
        return value;
    }

    /**
     * Get Integer property from the propertyName
     * Return default value if propertyName is not found.
     *
     * @param properties   properties to be looked up
     * @param propertyName the name of the property to look up
     * @param defaultValue the default value that to be assigned
     * @return value when the propertyName is defined or return default value
     */
    public static int getIntegerProperty(Dictionary<?, ?> properties,
                                         String propertyName,
                                         int defaultValue) {
        try {
            String s = get(properties, propertyName);
            return Strings.isNullOrEmpty(s) ? defaultValue : Integer.valueOf(s);
        } catch (NumberFormatException | ClassCastException e) {
            return defaultValue;
        }
    }

    /**
     * Check property name is defined and set to true.
     *
     * @param properties   properties to be looked up
     * @param propertyName the name of the property to look up
     * @return value when the propertyName is defined or return null
     */
    public static Boolean isPropertyEnabled(Dictionary<?, ?> properties,
                                             String propertyName) {
        Boolean value;
        try {
            String s = get(properties, propertyName);
            value = Strings.isNullOrEmpty(s) ? null : Boolean.valueOf(s);
        } catch (ClassCastException e) {
            value = null;
        }
        return value;
    }

    /**
     * Check property name is defined as set to true.
     *
     * @param properties   properties to be looked up
     * @param propertyName the name of the property to look up
     * @param defaultValue the default value that to be assigned
     * @return value when the propertyName is defined or return the default value
     */
    public static boolean isPropertyEnabled(Dictionary<?, ?> properties,
                                            String propertyName,
                                            boolean defaultValue) {
        try {
            String s = get(properties, propertyName);
            return Strings.isNullOrEmpty(s) ? defaultValue : Boolean.valueOf(s);
        } catch (ClassCastException e) {
            return defaultValue;
        }
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
     * Get Long property from the propertyName
     * Return null if propertyName is not found.
     *
     * @param properties   properties to be looked up
     * @param propertyName the name of the property to look up
     * @return value when the propertyName is defined or return null
     */
    public static Long getLongProperty(Dictionary<?, ?> properties,
                                             String propertyName) {
        Long value;
        try {
            String s = get(properties, propertyName);
            value = Strings.isNullOrEmpty(s) ? null : Long.valueOf(s);
        } catch (NumberFormatException | ClassCastException e) {
            value = null;
        }
        return value;
    }

    /**
     * Returns a function that retries execution on failure.
     * @param base base function
     * @param exceptionClass type of exception for which to retry
     * @param maxRetries max number of retries before giving up
     * @param maxDelayBetweenRetries max delay between successive retries. The actual delay is randomly picked from
     * the interval (0, maxDelayBetweenRetries]
     * @return function
     * @param <U> type of function input
     * @param <V> type of function output
     */
    public static <U, V> Function<U, V> retryable(Function<U, V> base,
            Class<? extends Throwable> exceptionClass,
            int maxRetries,
            int maxDelayBetweenRetries) {
        return new RetryingFunction<>(base, exceptionClass, maxRetries, maxDelayBetweenRetries);
    }

    /**
     * Returns a Supplier that retries execution on failure.
     * @param base base supplier
     * @param exceptionClass type of exception for which to retry
     * @param maxRetries max number of retries before giving up
     * @param maxDelayBetweenRetries max delay between successive retries. The actual delay is randomly picked from
     * the interval (0, maxDelayBetweenRetries]
     * @return supplier
     * @param <V> type of supplied result
     */
    public static <V> Supplier<V> retryable(Supplier<V> base,
            Class<? extends Throwable> exceptionClass,
            int maxRetries,
            int maxDelayBetweenRetries) {
        return () -> new RetryingFunction<>(v -> base.get(),
                exceptionClass,
                maxRetries,
                maxDelayBetweenRetries).apply(null);
    }

    /**
     * Suspends the current thread for a random number of millis between 0 and
     * the indicated limit.
     *
     * @param ms max number of millis
     */
    public static void randomDelay(int ms) {
        try {
            Thread.sleep(random.nextInt(ms));
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted", e);
        }
    }

    /**
     * Suspends the current thread for a specified number of millis and nanos.
     *
     * @param ms    number of millis
     * @param nanos number of nanos
     */
    public static void delay(int ms, int nanos) {
        try {
            Thread.sleep(ms, nanos);
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted", e);
        }
    }

    /**
     * Purges the specified directory path.&nbsp;Use with great caution since
     * no attempt is made to check for symbolic links, which could result in
     * deletion of unintended files.
     *
     * @param path directory to be removed
     * @throws java.io.IOException if unable to remove contents
     */
    public static void removeDirectory(String path) throws IOException {
        DirectoryDeleter visitor = new DirectoryDeleter();
        File dir = new File(path);
        if (dir.exists() && dir.isDirectory()) {
            walkFileTree(Paths.get(path), visitor);
            if (visitor.exception != null) {
                throw visitor.exception;
            }
        }
    }

    /**
     * Purges the specified directory path.&nbsp;Use with great caution since
     * no attempt is made to check for symbolic links, which could result in
     * deletion of unintended files.
     *
     * @param dir directory to be removed
     * @throws java.io.IOException if unable to remove contents
     */
    public static void removeDirectory(File dir) throws IOException {
        DirectoryDeleter visitor = new DirectoryDeleter();
        if (dir.exists() && dir.isDirectory()) {
            walkFileTree(Paths.get(dir.getAbsolutePath()), visitor);
            if (visitor.exception != null) {
                throw visitor.exception;
            }
        }
    }

    // Auxiliary path visitor for recursive directory structure removal.
    private static class DirectoryDeleter extends SimpleFileVisitor<Path> {

        private IOException exception;

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attributes)
                throws IOException {
            if (attributes.isRegularFile()) {
                delete(file);
            }
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult postVisitDirectory(Path directory, IOException ioe)
                throws IOException {
            delete(directory);
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFileFailed(Path file, IOException ioe)
                throws IOException {
            this.exception = ioe;
            return FileVisitResult.TERMINATE;
        }
    }

    /**
     * Returns a human friendly time ago string for a specified system time.
     *
     * @param unixTime system time in millis
     * @return human friendly time ago
     */
    public static String timeAgo(long unixTime) {
        long deltaMillis = System.currentTimeMillis() - unixTime;
        long secondsSince = (long) (deltaMillis / 1000.0);
        long minsSince = (long) (deltaMillis / (1000.0 * 60));
        long hoursSince = (long) (deltaMillis / (1000.0 * 60 * 60));
        long daysSince = (long) (deltaMillis / (1000.0 * 60 * 60 * 24));
        if (daysSince > 0) {
            return String.format("%dd%dh ago", daysSince, hoursSince - daysSince * 24);
        } else if (hoursSince > 0) {
            return String.format("%dh%dm ago", hoursSince, minsSince - hoursSince * 60);
        } else if (minsSince > 0) {
            return String.format("%dm%ds ago", minsSince, secondsSince - minsSince * 60);
        } else if (secondsSince > 0) {
            return String.format("%ds ago", secondsSince);
        } else {
            return "just now";
        }
    }

    /**
     * Copies the specified directory path.&nbsp;Use with great caution since
     * no attempt is made to check for symbolic links, which could result in
     * copy of unintended files.
     *
     * @param src directory to be copied
     * @param dst destination directory to be removed
     * @throws java.io.IOException if unable to remove contents
     */
    public static void copyDirectory(String src, String dst) throws IOException {
        walkFileTree(Paths.get(src), new DirectoryCopier(src, dst));
    }

    /**
     * Copies the specified directory path.&nbsp;Use with great caution since
     * no attempt is made to check for symbolic links, which could result in
     * copy of unintended files.
     *
     * @param src directory to be copied
     * @param dst destination directory to be removed
     * @throws java.io.IOException if unable to remove contents
     */
    public static void copyDirectory(File src, File dst) throws IOException {
        walkFileTree(Paths.get(src.getAbsolutePath()),
                     new DirectoryCopier(src.getAbsolutePath(),
                                         dst.getAbsolutePath()));
    }

    /**
     * Returns the future value when complete or if future
     * completes exceptionally returns the defaultValue.
     *
     * @param future future
     * @param defaultValue default value
     * @param <T> future value type
     * @return future value when complete or if future
     * completes exceptionally returns the defaultValue.
     */
    public static <T> T futureGetOrElse(Future<T> future, T defaultValue) {
        try {
            return future.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return defaultValue;
        } catch (ExecutionException e) {
            return defaultValue;
        }
    }

    /**
     * Returns the future value when complete or if future
     * completes exceptionally returns the defaultValue.
     *
     * @param future future
     * @param timeout time to wait for successful completion
     * @param timeUnit time unit
     * @param defaultValue default value
     * @param <T> future value type
     * @return future value when complete or if future
     * completes exceptionally returns the defaultValue.
     */
    public static <T> T futureGetOrElse(Future<T> future,
                                        long timeout,
                                        TimeUnit timeUnit,
                                        T defaultValue) {
        try {
            return future.get(timeout, timeUnit);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return defaultValue;
        } catch (ExecutionException | TimeoutException e) {
            return defaultValue;
        }
    }

    /**
     * Returns a future that is completed exceptionally.
     *
     * @param t exception
     * @param <T> future value type
     * @return future
     */
    public static <T> CompletableFuture<T> exceptionalFuture(Throwable t) {
        CompletableFuture<T> future = new CompletableFuture<>();
        future.completeExceptionally(t);
        return future;
    }

    /**
     * Returns a future that's completed using the given {@code orderedExecutor} if the future is not blocked or the
     * given {@code threadPoolExecutor} if the future is blocked.
     * <p>
     * This method allows futures to maintain single-thread semantics via the provided {@code orderedExecutor} while
     * ensuring user code can block without blocking completion of futures. When the returned future or any of its
     * descendants is blocked on a {@link CompletableFuture#get()} or {@link CompletableFuture#join()} call, completion
     * of the returned future will be done using the provided {@code threadPoolExecutor}.
     *
     * @param future the future to convert into an asynchronous future
     * @param orderedExecutor the ordered executor with which to attempt to complete the future
     * @param threadPoolExecutor the backup executor with which to complete blocked futures
     * @param <T> future value type
     * @return a new completable future to be completed using the provided {@code executor} once the provided
     * {@code future} is complete
     */
    public static <T> CompletableFuture<T> orderedFuture(
            CompletableFuture<T> future,
            Executor orderedExecutor,
            Executor threadPoolExecutor) {
        if (future.isDone()) {
            return future;
        }

        BlockingAwareFuture<T> newFuture = new BlockingAwareFuture<T>();
        future.whenComplete((result, error) -> {
            Runnable completer = () -> {
                if (future.isCompletedExceptionally()) {
                    newFuture.completeExceptionally(error);
                } else {
                    newFuture.complete(result);
                }
            };

            if (newFuture.isBlocked()) {
                threadPoolExecutor.execute(completer);
            } else {
                orderedExecutor.execute(completer);
            }
        });
        return newFuture;
    }

    /**
     * Returns a new CompletableFuture completed with a list of computed values
     * when all of the given CompletableFuture complete.
     *
     * @param futures the CompletableFutures
     * @param <T> value type of CompletableFuture
     * @return a new CompletableFuture that is completed when all of the given CompletableFutures complete
     */
    public static <T> CompletableFuture<List<T>> allOf(List<CompletableFuture<T>> futures) {
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[futures.size()]))
                .thenApply(v -> futures.stream()
                                .map(CompletableFuture::join)
                                .collect(Collectors.toList())
                );
    }

    /**
     * Returns a new CompletableFuture completed by reducing a list of computed values
     * when all of the given CompletableFuture complete.
     *
     * @param futures the CompletableFutures
     * @param reducer reducer for computing the result
     * @param emptyValue zero value to be returned if the input future list is empty
     * @param <T> value type of CompletableFuture
     * @return a new CompletableFuture that is completed when all of the given CompletableFutures complete
     */
    public static <T> CompletableFuture<T> allOf(List<CompletableFuture<T>> futures,
                                                 BinaryOperator<T> reducer,
                                                 T emptyValue) {
        return Tools.allOf(futures)
                    .thenApply(resultList -> resultList.stream().reduce(reducer).orElse(emptyValue));
    }

    /**
     * Returns a new CompletableFuture completed by with the first positive result from a list of
     * input CompletableFutures.
     *
     * @param futures the input list of CompletableFutures
     * @param positiveResultMatcher matcher to identify a positive result
     * @param negativeResult value to complete with if none of the futures complete with a positive result
     * @param <T> value type of CompletableFuture
     * @return a new CompletableFuture
     */
    public static <T> CompletableFuture<T> firstOf(List<CompletableFuture<T>> futures,
                                                   Match<T> positiveResultMatcher,
                                                   T negativeResult) {
        CompletableFuture<T> responseFuture = new CompletableFuture<>();
        Tools.allOf(Lists.transform(futures, future -> future.thenAccept(r -> {
            if (positiveResultMatcher.matches(r)) {
                responseFuture.complete(r);
            }
        }))).whenComplete((r, e) -> {
            if (!responseFuture.isDone()) {
                if (e != null) {
                    responseFuture.completeExceptionally(e);
                } else {
                    responseFuture.complete(negativeResult);
                }
            }
        });
        return responseFuture;
    }

    /**
     * Returns the contents of {@code ByteBuffer} as byte array.
     * <p>
     * WARNING: There is a performance cost due to array copy
     * when using this method.
     *
     * @param buffer byte buffer
     * @return byte array containing the byte buffer contents
     */
    public static byte[] byteBuffertoArray(ByteBuffer buffer) {
        int length = buffer.remaining();
        if (buffer.hasArray()) {
            int offset = buffer.arrayOffset() + buffer.position();
            return Arrays.copyOfRange(buffer.array(), offset, offset + length);
        }
        byte[] bytes = new byte[length];
        buffer.duplicate().get(bytes);
        return bytes;
    }

    /**
     * Converts an iterable to a stream.
     *
     * @param it iterable to convert
     * @param <T> type if item
     * @return iterable as a stream
     */
    public static <T> Stream<T> stream(Iterable<T> it) {
        return StreamSupport.stream(it.spliterator(), false);
    }

    /**
     * Converts an optional to a stream.
     *
     * @param optional optional to convert
     * @param <T> type of enclosed value
     * @return optional as a stream
     */
    public static <T> Stream<T> stream(Optional<? extends T> optional) {
        return optional.map(x -> Stream.<T>of(x)).orElse(Stream.empty());
    }

    // Auxiliary path visitor for recursive directory structure copying.
    private static class DirectoryCopier extends SimpleFileVisitor<Path> {
        private Path src;
        private Path dst;
        private StandardCopyOption copyOption = StandardCopyOption.REPLACE_EXISTING;

        DirectoryCopier(String src, String dst) {
            this.src = Paths.get(src);
            this.dst = Paths.get(dst);
        }

        @Override
        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
            Path targetPath = dst.resolve(src.relativize(dir));
            if (!Files.exists(targetPath)) {
                Files.createDirectory(targetPath);
            }
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            Files.copy(file, dst.resolve(src.relativize(file)), copyOption);
            return FileVisitResult.CONTINUE;
        }
    }

}
