/*
 * Copyright 2014 Open Networking Laboratory
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
package org.onosproject.incubator.net.faultmanagement.alarm;

import com.google.common.annotations.Beta;

import java.util.Objects;

import static com.google.common.base.MoreObjects.toStringHelper;

/**
 * Alarm identifier suitable as an external key.
 * <p>
 * This class is immutable.</p>
 */
@Beta
public final class AlarmId {

    private final long id;

    /**
     * Instantiates a new Alarm id.
     *
     * @param id the id
     */
    public AlarmId(final long id) {
        this.id = id;
    }

    /**
     * Creates an alarm identifier from the specified long representation.
     *
     * @param value long value
     * @return intent identifier
     */
    public static AlarmId valueOf(final long value) {
        return new AlarmId(value);
    }

    /**
     * Returns the backing integer index.
     *
     * @return backing integer index
     */
    public long fingerprint() {
        return id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof AlarmId) {
            final AlarmId other = (AlarmId) obj;
            return Objects.equals(this.id, other.id);
        }
        return false;
    }

    @Override
    public String toString() {
        return toStringHelper(this).add("id", id).toString();
    }

}
