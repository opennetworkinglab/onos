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
package org.onosproject.ovsdb.rfc.notation;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Objects;

import org.onosproject.ovsdb.rfc.notation.json.UUIDConverter;
import org.onosproject.ovsdb.rfc.notation.json.UUIDSerializer;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

/**
 * Handles both uuid and named-uuid.
 */
@JsonSerialize(using = UUIDSerializer.class)
@JsonDeserialize(converter = UUIDConverter.class)
public final class UUID {
    private final String value;

    /**
     * UUID constructor.
     * @param value UUID value
     */
    private UUID(String value) {
        checkNotNull(value, "value cannot be null");
        this.value = value;
    }

    /**
     * Get UUID.
     * @param value UUID value
     * @return UUID
     */
    public static UUID uuid(String value) {
        return new UUID(value);
    }

    /**
     * Returns value.
     * @return value
     */
    public String value() {
        return value;
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof UUID) {
            final UUID other = (UUID) obj;
            return Objects.equals(this.value, other.value);
        }
        return false;
    }

    @Override
    public String toString() {
        return toStringHelper(this).add("value", value).toString();
    }
}
