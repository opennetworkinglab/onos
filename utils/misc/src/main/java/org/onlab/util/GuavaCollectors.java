/*
 * Copyright 2015-present Open Networking Laboratory
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

import java.util.stream.Collector;
import java.util.stream.Collector.Characteristics;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

/**
 * Implementations of {@link Collector} that implement various useful reduction
 * operations, such as accumulating elements into Guava collections.
 */
public final class GuavaCollectors {

    /**
     * Returns a {@code Collector} that accumulates the input elements into a
     * new ImmutableSet.
     *
     * @param <T> type
     * @return a {@code Collector} which collects all the input elements into a
     * {@code ImmutableSet}
     *
     * @deprecated in 1.11.0 consider using {@link ImmutableSet#toImmutableSet()} instead.
     */
    @Deprecated
    public static <T> Collector<T, ImmutableSet.Builder<T>, ImmutableSet<T>> toImmutableSet() {
        return Collector.of(ImmutableSet.Builder<T>::new,
                            ImmutableSet.Builder<T>::add,
                            (s, r) -> s.addAll(r.build()),
                            ImmutableSet.Builder<T>::build,
                            Characteristics.UNORDERED);
    }

    /**
     * Returns a {@code Collector} that accumulates the input elements into a
     * new ImmutableList.
     *
     * @param <T> type
     * @return a {@code Collector} which collects all the input elements into a
     * {@code ImmutableList}, in encounter order
     *
     * @deprecated in 1.11.0 consider using {@link ImmutableList#toImmutableList()} instead.
     */
    @Deprecated
    public static <T> Collector<T, ImmutableList.Builder<T>, ImmutableList<T>> toImmutableList() {
        return Collector.of(ImmutableList.Builder<T>::new,
                            ImmutableList.Builder<T>::add,
                            (s, r) -> s.addAll(r.build()),
                            ImmutableList.Builder<T>::build);
    }

    private GuavaCollectors() {}
}
