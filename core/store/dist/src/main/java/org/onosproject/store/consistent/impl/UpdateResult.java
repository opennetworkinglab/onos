/*
 * Copyright 2015 Open Networking Laboratory
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
package org.onosproject.store.consistent.impl;

/**
 * Result of a update operation.
 * <p>
 * Both old and new values are accessible along with a flag that indicates if the
 * the value was updated. If flag is false, oldValue and newValue both
 * point to the same unmodified value.
 * @param <V> result type
 */
public class UpdateResult<V> {

    private final boolean updated;
    private final V oldValue;
    private final V newValue;

    public UpdateResult(boolean updated, V oldValue, V newValue) {
        this.updated = updated;
        this.oldValue = oldValue;
        this.newValue = newValue;
    }

    public boolean updated() {
        return updated;
    }

    public V oldValue() {
        return oldValue;
    }

    public V newValue() {
        return newValue;
    }
}