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
package org.onosproject.ovsdb.rfc.notation;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Objects;

import org.onosproject.ovsdb.rfc.notation.json.UuidConverter;
import org.onosproject.ovsdb.rfc.notation.json.UuidSerializer;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

/**
 * Handles both uuid and named-uuid.
 */
@JsonSerialize(using = UuidSerializer.class)
@JsonDeserialize(converter = UuidConverter.class)
public final class Uuid {
    private final String value;

    /**
     * UUID constructor.
     * @param value UUID value
     */
    private Uuid(String value) {
        checkNotNull(value, "value cannot be null");
        this.value = value;
    }

    /**
     * Get UUID.
     * @param value UUID value
     * @return UUID
     */
    public static Uuid uuid(String value) {
        return new Uuid(value);
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
        if (obj instanceof Uuid) {
            final Uuid other = (Uuid) obj;
            return Objects.equals(this.value, other.value);
        }
        return false;
    }

    @Override
    public String toString() {
        return toStringHelper(this).add("value", value).toString();
    }
}
