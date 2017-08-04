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
public final class RealBaseType implements BaseType {
    private final double min;
    private final double max;
    private final Set<Double> enums;

    /**
     * Constructs a RealBaseType object.
     */
    public RealBaseType() {
        this.min = Double.MIN_VALUE;
        this.max = Double.MAX_VALUE;
        this.enums = Sets.newHashSet();
    }

    /**
     * Constructs a RealBaseType object.
     * @param min min constraint
     * @param max max constraint
     * @param enums enums constraint
     */
    public RealBaseType(double min, double max, Set<Double> enums) {
        this.min = min;
        this.max = max;
        this.enums = enums;
    }

    /**
     * Get min.
     * @return min
     */
    public double getMin() {
        return min;
    }

    /**
     * Get max.
     * @return max
     */
    public double getMax() {
        return max;
    }

    /**
     * Get enums.
     * @return enums
     */
    public Set<Double> getEnums() {
        return enums;
    }

    @Override
    public int hashCode() {
        return Objects.hash(min, max, enums);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof RealBaseType) {
            final RealBaseType other = (RealBaseType) obj;
            return Objects.equals(this.enums, other.enums)
                    && Objects.equals(this.min, other.min)
                    && Objects.equals(this.max, other.max);
        }
        return false;
    }

    @Override
    public String toString() {
        return toStringHelper(this).add("min", min).add("max", max)
                .add("enums", enums).toString();
    }
}
