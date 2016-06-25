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
package org.onosproject.ovsdb.rfc.schema.type;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Objects;

/**
 * The "atomic-type" specifies the type of data stored in this column. Refer
 * to RFC 7047 Section 3.2.
 */
public final class AtomicColumnType implements ColumnType {
    private final BaseType baseType;
    private final int min;
    private final int max;

    /**
     * Constructs a AtomicColumnType object.
     * @param baseType BaseType entity
     */
    public AtomicColumnType(BaseType baseType) {
        checkNotNull(baseType, "BaseType cannot be null");
        this.baseType = baseType;
        this.min = 1;
        this.max = 1;
    }

    /**
     * Constructs a AtomicColumnType object.
     * @param baseType BaseType entity
     * @param min min constraint
     * @param max max constraint
     */
    public AtomicColumnType(BaseType baseType, int min, int max) {
        checkNotNull(baseType, "BaseType cannot be null");
        this.baseType = baseType;
        this.min = min;
        this.max = max;
    }

    /**
     * Get baseType.
     * @return baseType
     */
    public BaseType baseType() {
        return baseType;
    }

    /**
     * Get min.
     * @return min
     */
    public int min() {
        return min;
    }

    /**
     * Get max.
     * @return max
     */
    public int max() {
        return max;
    }

    @Override
    public int hashCode() {
        return Objects.hash(baseType, min, max);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof AtomicColumnType) {
            final AtomicColumnType other = (AtomicColumnType) obj;
            return Objects.equals(this.baseType, other.baseType)
                    && Objects.equals(this.min, other.min)
                    && Objects.equals(this.max, other.max);
        }
        return false;
    }

    @Override
    public String toString() {
        return toStringHelper(this).add("baseType", baseType).add("min", min)
                .add("max", max).toString();
    }
}
