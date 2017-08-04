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
package org.onosproject.ovsdb.rfc.schema.type;

import static com.google.common.base.MoreObjects.toStringHelper;

import java.util.Objects;
import java.util.Set;

import com.google.common.collect.Sets;

/**
 * One of the strings "integer", "real", "boolean", "string", or "uuid",
 * representing the specified scalar type. Refer to RFC 7047 Section 3.2.
 */
public final class StringBaseType implements BaseType {
    private final int minLength;
    private final int maxLength;
    private final Set<String> enums;

    /**
     * Constructs a StringBaseType object.
     */
    public StringBaseType() {
        this.minLength = Integer.MIN_VALUE;
        this.maxLength = Integer.MAX_VALUE;
        this.enums = Sets.newHashSet();
    }

    /**
     * Constructs a StringBaseType object.
     * @param minLength minLength constraint
     * @param maxLength maxLength constraint
     * @param enums enums constraint
     */
    public StringBaseType(int minLength, int maxLength, Set<String> enums) {
        this.minLength = minLength;
        this.maxLength = maxLength;
        this.enums = enums;
    }

    /**
     * Get minLength.
     * @return minLength
     */
    public int getMinLength() {
        return minLength;
    }

    /**
     * Get maxLength.
     * @return maxLength
     */
    public int getMaxLength() {
        return maxLength;
    }

    /**
     * Get enums.
     * @return enums
     */
    public Set<String> getEnums() {
        return enums;
    }

    @Override
    public int hashCode() {
        return Objects.hash(minLength, maxLength, enums);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof StringBaseType) {
            final StringBaseType other = (StringBaseType) obj;
            return Objects.equals(this.enums, other.enums)
                    && Objects.equals(this.minLength, other.minLength)
                    && Objects.equals(this.maxLength, other.maxLength);
        }
        return false;
    }

    @Override
    public String toString() {
        return toStringHelper(this).add("minLength", minLength)
                .add("maxLength", maxLength).add("enums", enums).toString();
    }
}
