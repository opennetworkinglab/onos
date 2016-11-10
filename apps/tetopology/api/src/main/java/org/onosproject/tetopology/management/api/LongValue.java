/*
 * Copyright 2016 Open Networking Laboratory
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
package org.onosproject.tetopology.management.api;

import com.google.common.base.MoreObjects;

import java.util.Objects;

/**
 * Implementation of using a long integer to represent
 * an ElementType.
 */
public class LongValue {
    private final long value;

    /**
     * Creates an instance of LongValue.
     *
     * @param value long value
     */
    public LongValue(long value) {
        this.value = value;
    }

    /**
     * Returns the long integer representing the ElementType.
     *
     * @return long integer
     */
    public long value() {
        return value;
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof LongValue) {
            LongValue other = (LongValue) obj;
            return Objects.equals(value, other.value);
        }
        return false;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("value", value)
                .toString();
    }
}
