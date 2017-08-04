/*
 * Copyright 2015-present Open Networking Foundation
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

/**
 * A consumer that accepts three arguments.
 *
 * @param <U> type of first argument
 * @param <V> type of second argument
 * @param <W> type of third argument
 */
public interface TriConsumer<U, V, W> {

    /**
     * Applies the given arguments to the function.
     * @param arg1 first argument
     * @param arg2 second argument
     * @param arg3 third argument
     */
    void accept(U arg1, V arg2, W arg3);

}