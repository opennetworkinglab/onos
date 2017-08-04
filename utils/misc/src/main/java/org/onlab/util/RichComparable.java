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

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Extends useful methods for comparison to {@link Comparable} interface.
 *
 * @param <T> type of instance to be compared
 */
public interface RichComparable<T> extends Comparable<T> {
    /**
     * Compares if this object is less than the specified object.
     *
     * @param other the object to be compared
     * @return true if this object is less than the specified object
     */
    default boolean isLessThan(T other) {
        return compareTo(checkNotNull(other)) < 0;
    }

    /**
     * Compares if this object is greater than the specified object.
     *
     * @param other the object to be compared
     * @return true if this object is less thant the specified object
     */
    default boolean isGreaterThan(T other) {
        return compareTo(checkNotNull(other)) > 0;
    }
}
