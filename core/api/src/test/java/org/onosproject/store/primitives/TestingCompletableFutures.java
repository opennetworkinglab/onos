/*
 * Copyright 2018-present Open Networking Foundation
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

package org.onosproject.store.primitives;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Completable Futures with known error states used for testing.
 */
public final class TestingCompletableFutures {

    private TestingCompletableFutures() {
    }

    /** Indicates which kind of error to produce. */
    public enum ErrorState { NONE, INTERRUPTED_EXCEPTION, TIMEOUT_EXCEPTION, EXECUTION_EXCEPTION }

    private static class TimeoutExceptionFuture<T> extends CompletableFuture<T> {
        @Override
        public T get(long timeout, TimeUnit unit)
                throws TimeoutException {
            throw new TimeoutException();
        }
    }

    private static class InterruptedFuture<T> extends CompletableFuture<T> {
        @Override
        public T get(long timeout, TimeUnit unit)
                throws InterruptedException {
            throw new InterruptedException();
        }
    }

    private static class ExecutionExceptionFuture<T> extends CompletableFuture<T> {
        @Override
        public T get(long timeout, TimeUnit unit)
                throws ExecutionException {
            throw new ExecutionException("", new Exception());
        }
    }

    /**
     * Creates a Long Future for a given error type.
     *
     * @param errorState wht kind of error to produce
     * @return new future that will generate the requested error
     */
    public static CompletableFuture<Long> createFuture(ErrorState errorState) {
        switch (errorState) {
            case TIMEOUT_EXCEPTION:
                return new TimeoutExceptionFuture<>();
            case INTERRUPTED_EXCEPTION:
                return new InterruptedFuture<>();
            case EXECUTION_EXCEPTION:
                return new ExecutionExceptionFuture<>();
            default:
                return new CompletableFuture<>();
        }
    }

    /**
     * Creates a Long Future for a given error type.
     *
     * @param errorState wht kind of error to produce
     * @return new future that will generate the requested error
     */
    public static CompletableFuture<String> createStringFuture(ErrorState errorState) {
        switch (errorState) {
            case TIMEOUT_EXCEPTION:
                return new TimeoutExceptionFuture<>();
            case INTERRUPTED_EXCEPTION:
                return new InterruptedFuture<>();
            case EXECUTION_EXCEPTION:
                return new ExecutionExceptionFuture<>();
            default:
                return new CompletableFuture<>();
        }
    }
}
