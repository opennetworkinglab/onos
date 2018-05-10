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
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Objects;

/**
 * One of the strings "integer", "real", "boolean", "string", or "uuid",
 * representing the specified scalar type. Refer to RFC 7047 Section 3.2.
 */
public final class UuidBaseType implements BaseType {
    /**
     * RefType is strong or weak. refer to base-type of RFC 7047 Section 3.2.
     */
    public enum RefType {
        STRONG("strong"), WEAK("weak");

        private String refType;

        RefType(String refType) {
            this.refType = refType;
        }

        /**
         * Returns the refType for RefType.
         * @return the refType
         */
        public String refType() {
            return refType;
        }
    }

    private final String refTable;
    private final String refType;

    /**
     * Constructs a UuidBaseType object.
     */
    public UuidBaseType() {
        this.refTable = null;
        this.refType = RefType.STRONG.refType();
    }

    /**
     * Constructs a UuidBaseType object.
     * @param refTable refTable constraint
     * @param refType refType constraint
     */
    public UuidBaseType(String refTable, String refType) {
        checkNotNull(refType, "refType cannot be null");
        this.refTable = refTable;
        this.refType = refType;
    }

    /**
     * Get refTable.
     * @return refTable
     */
    public String getRefTable() {
        return refTable;
    }

    /**
     * Get refType.
     * @return refType
     */
    public String getRefType() {
        return refType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(refTable, refType);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof UuidBaseType) {
            final UuidBaseType other = (UuidBaseType) obj;
            return Objects.equals(this.refTable, other.refTable)
                    && Objects.equals(this.refType, other.refType);
        }
        return false;
    }

    @Override
    public String toString() {
        return toStringHelper(this).add("refTable", refTable)
                .add("refType", refType).toString();
    }
}
