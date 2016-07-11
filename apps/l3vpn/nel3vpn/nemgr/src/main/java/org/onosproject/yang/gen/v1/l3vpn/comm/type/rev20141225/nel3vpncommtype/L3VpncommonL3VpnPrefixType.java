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

package org.onosproject.yang.gen.v1.l3vpn.comm.type.rev20141225.nel3vpncommtype;

import org.onosproject.yang.gen.v1.l3vpn.comm.type.rev20141225.nel3vpncommtype.l3vpncommonl3vpnprefixtype
            .L3VpncommonL3VpnPrefixTypeEnum;
import java.util.Objects;
import com.google.common.base.MoreObjects;

/**
 * Represents the implementation of l3VpncommonL3VpnPrefixType.
 */
public final class L3VpncommonL3VpnPrefixType {

    private L3VpncommonL3VpnPrefixTypeEnum enumeration;

    /**
     * Creates an instance of l3VpncommonL3VpnPrefixType.
     */
    private L3VpncommonL3VpnPrefixType() {
    }

    /**
     * Creates an instance of l3VpncommonL3VpnPrefixTypeForTypeEnumeration.
     *
     * @param value value of l3VpncommonL3VpnPrefixTypeForTypeEnumeration
     */
    public L3VpncommonL3VpnPrefixType(L3VpncommonL3VpnPrefixTypeEnum value) {
        this.enumeration = value;
    }

    /**
     * Returns the object of l3VpncommonL3VpnPrefixTypeForTypeEnumeration.
     *
     * @param value value of l3VpncommonL3VpnPrefixTypeForTypeEnumeration
     * @return Object of l3VpncommonL3VpnPrefixTypeForTypeEnumeration
     */
    public static L3VpncommonL3VpnPrefixType of(L3VpncommonL3VpnPrefixTypeEnum value) {
        return new L3VpncommonL3VpnPrefixType(value);
    }

    /**
     * Returns the attribute enumeration.
     *
     * @return value of enumeration
     */
    public L3VpncommonL3VpnPrefixTypeEnum enumeration() {
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
        if (obj instanceof L3VpncommonL3VpnPrefixType) {
            L3VpncommonL3VpnPrefixType other = (L3VpncommonL3VpnPrefixType) obj;
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
     * Returns the object of l3VpncommonL3VpnPrefixType fromString input String.
     *
     * @param valInString input String
     * @return Object of l3VpncommonL3VpnPrefixType
     */
    public static L3VpncommonL3VpnPrefixType fromString(String valInString) {
        try {
            L3VpncommonL3VpnPrefixTypeEnum tmpVal = L3VpncommonL3VpnPrefixTypeEnum.fromString(valInString);
            return of(tmpVal);
        } catch (Exception e) {
        }
        return null;
    }
}
