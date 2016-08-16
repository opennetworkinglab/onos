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

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.commons.lang3.concurrent.ConcurrentInitializer;

/**
 * Creates an instance of new ConcurrentHashMap on each {@link #get()} call.
 * <p>
 * To be used with
 * {@link org.apache.commons.lang3.concurrent.ConcurrentUtils#createIfAbsent}
 * </p>
 *
 * @param <K> ConcurrentHashMap key type
 * @param <V> ConcurrentHashMap value type
 *
 * @deprecated in Hummingbird (1.7.0)
 */
@Deprecated
public final class NewConcurrentHashMap<K, V>
    implements ConcurrentInitializer<ConcurrentMap<K, V>> {

    public static final NewConcurrentHashMap<?, ?> INSTANCE = new NewConcurrentHashMap<>();

    @SuppressWarnings("unchecked")
    public static <K, V> NewConcurrentHashMap<K, V> ifNeeded() {
        return (NewConcurrentHashMap<K, V>) INSTANCE;
    }

    @Override
    public ConcurrentMap<K, V> get() {
        return new ConcurrentHashMap<>();
    }
}
