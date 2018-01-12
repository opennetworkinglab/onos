/*
 *  Copyright 2015-present Open Networking Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.onlab.util;

import java.util.HashMap;

/**
 * HashMap that returns a default value for unmapped keys.
 */
public final class DefaultHashMap<K, V> extends HashMap<K, V> {

    /** Default value to return when no key binding exists. */
    private final V defaultValue;

    /**
     * Constructs an empty map with the given default value.
     *
     * @param defaultValue the default value
     */
    public DefaultHashMap(V defaultValue) {
        this.defaultValue = defaultValue;
    }

    @Override
    public V get(Object k) {
        return containsKey(k) ? super.get(k) : defaultValue;
    }
}