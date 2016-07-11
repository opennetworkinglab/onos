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

import org.onosproject.yang.gen.v1.l3vpn.comm.type.rev20141225.nel3vpncommtype.l3vpncommonvrfrttype
            .L3VpncommonVrfRtTypeEnum;
import java.util.Objects;
import com.google.common.base.MoreObjects;

/**
 * Represents the implementation of l3VpncommonVrfRtType.
 */
public final class L3VpncommonVrfRtType {

    private L3VpncommonVrfRtTypeEnum enumeration;

    /**
     * Creates an instance of l3VpncommonVrfRtType.
     */
    private L3VpncommonVrfRtType() {
    }

    /**
     * Creates an instance of l3VpncommonVrfRtTypeForTypeEnumeration.
     *
     * @param value value of l3VpncommonVrfRtTypeForTypeEnumeration
     */
    public L3VpncommonVrfRtType(L3VpncommonVrfRtTypeEnum value) {
        this.enumeration = value;
    }

    /**
     * Returns the object of l3VpncommonVrfRtTypeForTypeEnumeration.
     *
     * @param value value of l3VpncommonVrfRtTypeForTypeEnumeration
     * @return Object of l3VpncommonVrfRtTypeForTypeEnumeration
     */
    public static L3VpncommonVrfRtType of(L3VpncommonVrfRtTypeEnum value) {
        return new L3VpncommonVrfRtType(value);
    }

    /**
     * Returns the attribute enumeration.
     *
     * @return value of enumeration
     */
    public L3VpncommonVrfRtTypeEnum enumeration() {
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
        if (obj instanceof L3VpncommonVrfRtType) {
            L3VpncommonVrfRtType other = (L3VpncommonVrfRtType) obj;
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
     * Returns the object of l3VpncommonVrfRtType fromString input String.
     *
     * @param valInString input String
     * @return Object of l3VpncommonVrfRtType
     */
    public static L3VpncommonVrfRtType fromString(String valInString) {
        try {
            L3VpncommonVrfRtTypeEnum tmpVal = L3VpncommonVrfRtTypeEnum.fromString(valInString);
            return of(tmpVal);
        } catch (Exception e) {
        }
        return null;
    }
}
