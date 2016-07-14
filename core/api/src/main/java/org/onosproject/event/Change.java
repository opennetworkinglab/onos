/*
 * Copyright 2016-present Open Networking Laboratory
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
package org.onosproject.event;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;

/**
 * Generic representation of an update.
 *
 * @param <T> type of value that was updated
 */
public class Change<T> {

    private final T oldValue;
    private final T newValue;

    public Change(T oldValue, T newValue) {
        this.oldValue = oldValue;
        this.newValue = newValue;
    }

    /**
     * Returns previous value.
     * @return previous value.
     */
    public T oldValue() {
        return oldValue;
    }

    /**
     * Returns new or current value.
     * @return new value.
     */
    public T newValue() {
        return newValue;
    }

    @Override
    public boolean equals(Object other) {
        if (other == null || !(other instanceof Change)) {
            return false;
        }
        Change<T> that = (Change<T>) other;
        return Objects.equal(this.oldValue, that.oldValue) &&
                Objects.equal(this.newValue, that.newValue);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(oldValue, newValue);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("oldValue", oldValue)
                .add("newValue", newValue)
                .toString();
    }
}
