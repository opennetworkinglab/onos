/*
 * Copyright 2016-present Open Networking Foundation
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

import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

/**
 * A synchronization utility that defers invocation of a {@link Consumer consumer}
 * callback until a set number of actions tracked by a {@code long} counter complete.
 * <p>
 * Each completion is recorded by invoking the {@link CountDownCompleter#countDown countDown}
 * method. When the total number of completions is equal to the preset counter value,
 * this instance is marked as completed and the callback invoked by supplying the object
 * held by this instance.
 *
 * @param <T> object type
 */
public final class CountDownCompleter<T> {

    private final T object;
    private final Consumer<T> onCompleteCallback;
    private final AtomicLong counter;

    /**
     * Constructor.
     * @param object object
     * @param count total number of times countDown must be invoked for this completer to complete
     * @param onCompleteCallback callback to invoke when completer is completed
     */
    public CountDownCompleter(T object, long count, Consumer<T> onCompleteCallback) {
        checkState(count >= 0, "count must be non-negative");
        this.counter = new AtomicLong(count);
        this.object = checkNotNull(object);
        this.onCompleteCallback = checkNotNull(onCompleteCallback);
        if (count == 0) {
            onCompleteCallback.accept(object);
        }
    }

    /**
     * Returns the object.
     * @return object
     */
    public T object() {
        return object;
    }

    /**
     * Records a single completion.
     * <p>
     * If this instance has already completed, this method has no effect
     */
    public void countDown() {
        if (counter.decrementAndGet() == 0) {
            onCompleteCallback.accept(object);
        }
    }

    /**
     * Returns if this instance has completed.
     * @return {@code true} if completed
     */
    public boolean isComplete() {
        return counter.get() <= 0;
    }
}