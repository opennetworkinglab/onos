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
package org.onosproject.store;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Opaque version structure.
 * <p>
 * Classes implementing this interface must also implement
 * {@link #hashCode()} and {@link #equals(Object)}.
 */
public interface Timestamp extends Comparable<Timestamp> {

    @Override
    int hashCode();

    @Override
    boolean equals(Object obj);

    /**
     * Tests if this timestamp is newer than the specified timestamp.
     *
     * @param other timestamp to compare against
     * @return true if this instance is newer
     */
    default boolean isNewerThan(Timestamp other) {
        return this.compareTo(checkNotNull(other)) > 0;
    }

    /**
     * Tests if this timestamp is older than the specified timestamp.
     *
     * @param other timestamp to compare against
     * @return true if this instance is older
     */
    default boolean isOlderThan(Timestamp other) {
        return this.compareTo(checkNotNull(other)) < 0;
    }
}
