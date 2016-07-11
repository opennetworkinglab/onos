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

package org.onosproject.yang.gen.v1.l3vpn.comm.type.rev20141225.nel3vpncommtype.l3vpncommonl3vpnprefixtype;

/**
 * Represents ENUM data of l3VpncommonL3VpnPrefixTypeEnum.
 */
public enum L3VpncommonL3VpnPrefixTypeEnum {

    /**
     * Represents ipv4Uni.
     */
    IPV4UNI(0),

    /**
     * Represents ipv6Uni.
     */
    IPV6UNI(1);

    private int l3VpncommonL3VpnPrefixTypeEnum;

    /**
     * Creates an instance of l3VpncommonL3VpnPrefixTypeEnum.
     *
     * @param value value of l3VpncommonL3VpnPrefixTypeEnum
     */
    L3VpncommonL3VpnPrefixTypeEnum(int value) {
        l3VpncommonL3VpnPrefixTypeEnum = value;
    }

    /**
     * Returns the object of l3VpncommonL3VpnPrefixTypeEnumForTypeInt.
     *
     * @param value value of l3VpncommonL3VpnPrefixTypeEnumForTypeInt
     * @return Object of l3VpncommonL3VpnPrefixTypeEnumForTypeInt
     */
    public static L3VpncommonL3VpnPrefixTypeEnum of(int value) {
        switch (value) {
            case 0:
                return L3VpncommonL3VpnPrefixTypeEnum.IPV4UNI;
            case 1:
                return L3VpncommonL3VpnPrefixTypeEnum.IPV6UNI;
            default :
                return null;
        }
    }

    /**
     * Returns the attribute l3VpncommonL3VpnPrefixTypeEnum.
     *
     * @return value of l3VpncommonL3VpnPrefixTypeEnum
     */
    public int l3VpncommonL3VpnPrefixTypeEnum() {
        return l3VpncommonL3VpnPrefixTypeEnum;
    }

    /**
     * Returns the object of l3VpncommonL3VpnPrefixTypeEnum fromString input String.
     *
     * @param valInString input String
     * @return Object of l3VpncommonL3VpnPrefixTypeEnum
     */
    public static L3VpncommonL3VpnPrefixTypeEnum fromString(String valInString) {
        try {
            int tmpVal = Integer.parseInt(valInString);
            return of(tmpVal);
        } catch (Exception e) {
        }
        return null;
    }
}
