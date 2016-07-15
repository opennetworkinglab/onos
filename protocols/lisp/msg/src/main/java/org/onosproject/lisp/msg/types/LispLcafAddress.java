/*
 * Copyright 2016-present Open Networking Laboratory
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
package org.onosproject.lisp.msg.types;

import java.util.Objects;

import static com.google.common.base.MoreObjects.toStringHelper;

/**
 * LISP Canonical Address Formatted address class.
 */
public class LispLcafAddress extends LispAfiAddress {

    protected final LispCanonicalAddressFormatEnum lcafType;
    protected final byte value;

    /**
     * Initializes LCAF address.
     *
     * @param lcafType LCAF type
     * @param value LCAF value
     */
    protected LispLcafAddress(LispCanonicalAddressFormatEnum lcafType, byte value) {
        super(AddressFamilyIdentifierEnum.LCAF);
        this.lcafType = lcafType;
        this.value = value;
    }

    /**
     * Obtains LCAF type.
     *
     * @return LCAF type
     */
    public LispCanonicalAddressFormatEnum getType() {
        return lcafType;
    }

    /**
     * Obtains LCAF value.
     *
     * @return LCAF value
     */
    public byte getValue() {
        return value;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((lcafType == null) ? 0 : lcafType.hashCode());
        result = prime * result + value;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof LispLcafAddress) {
            final LispLcafAddress other = (LispLcafAddress) obj;
            return Objects.equals(this.lcafType, other.lcafType) &&
                    Objects.equals(this.value, other.value);
        }
        return false;
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("lcafType", lcafType)
                .add("lcafValue", value)
                .toString();
    }
}
