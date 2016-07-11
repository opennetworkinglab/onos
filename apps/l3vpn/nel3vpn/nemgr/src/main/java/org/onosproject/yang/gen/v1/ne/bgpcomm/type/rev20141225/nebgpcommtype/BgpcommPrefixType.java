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

package org.onosproject.yang.gen.v1.ne.bgpcomm.type.rev20141225.nebgpcommtype;

import org.onosproject.yang.gen.v1.ne.bgpcomm.type.rev20141225.nebgpcommtype.bgpcommprefixtype.BgpcommPrefixTypeEnum;
import java.util.Objects;
import com.google.common.base.MoreObjects;

/**
 * Represents the implementation of bgpcommPrefixType.
 */
public final class BgpcommPrefixType {

    private BgpcommPrefixTypeEnum enumeration;

    /**
     * Creates an instance of bgpcommPrefixType.
     */
    private BgpcommPrefixType() {
    }

    /**
     * Creates an instance of bgpcommPrefixTypeForTypeEnumeration.
     *
     * @param value value of bgpcommPrefixTypeForTypeEnumeration
     */
    public BgpcommPrefixType(BgpcommPrefixTypeEnum value) {
        this.enumeration = value;
    }

    /**
     * Returns the object of bgpcommPrefixTypeForTypeEnumeration.
     *
     * @param value value of bgpcommPrefixTypeForTypeEnumeration
     * @return Object of bgpcommPrefixTypeForTypeEnumeration
     */
    public static BgpcommPrefixType of(BgpcommPrefixTypeEnum value) {
        return new BgpcommPrefixType(value);
    }

    /**
     * Returns the attribute enumeration.
     *
     * @return value of enumeration
     */
    public BgpcommPrefixTypeEnum enumeration() {
        return enumeration;
    }

    @Override
    public int hashCode() {
        return Objects.hash(enumeration);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof BgpcommPrefixType) {
            BgpcommPrefixType other = (BgpcommPrefixType) obj;
            return
                 Objects.equals(enumeration, other.enumeration);
        }
        return false;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
            .add("enumeration", enumeration)
            .toString();
    }

    /**
     * Returns the object of bgpcommPrefixType fromString input String.
     *
     * @param valInString input String
     * @return Object of bgpcommPrefixType
     */
    public static BgpcommPrefixType fromString(String valInString) {
        try {
            BgpcommPrefixTypeEnum tmpVal = BgpcommPrefixTypeEnum.fromString(valInString);
            return of(tmpVal);
        } catch (Exception e) {
        }
        return null;
    }
}
