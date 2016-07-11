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

package org.onosproject.yang.gen.v1.ne.bgpcomm.type.rev20141225.nebgpcommtype.bgpcommprefixtype;

/**
 * Represents ENUM data of bgpcommPrefixTypeEnum.
 */
public enum BgpcommPrefixTypeEnum {

    /**
     * Represents ipv4Uni.
     */
    IPV4UNI(0),

    /**
     * Represents ipv4Multi.
     */
    IPV4MULTI(1),

    /**
     * Represents ipv4Vpn.
     */
    IPV4VPN(2),

    /**
     * Represents ipv6Uni.
     */
    IPV6UNI(3),

    /**
     * Represents ipv6Vpn.
     */
    IPV6VPN(4),

    /**
     * Represents ipv4Flow.
     */
    IPV4FLOW(5),

    /**
     * Represents l2Vpnad.
     */
    L2VPNAD(6),

    /**
     * Represents mvpn.
     */
    MVPN(7),

    /**
     * Represents evpn.
     */
    EVPN(8),

    /**
     * Represents ipv4Vpnmcast.
     */
    IPV4VPNMCAST(9),

    /**
     * Represents ls.
     */
    LS(10),

    /**
     * Represents mdt.
     */
    MDT(11);

    private int bgpcommPrefixTypeEnum;

    /**
     * Creates an instance of bgpcommPrefixTypeEnum.
     *
     * @param value value of bgpcommPrefixTypeEnum
     */
    BgpcommPrefixTypeEnum(int value) {
        bgpcommPrefixTypeEnum = value;
    }

    /**
     * Returns the object of bgpcommPrefixTypeEnumForTypeInt.
     *
     * @param value value of bgpcommPrefixTypeEnumForTypeInt
     * @return Object of bgpcommPrefixTypeEnumForTypeInt
     */
    public static BgpcommPrefixTypeEnum of(int value) {
        switch (value) {
            case 0:
                return BgpcommPrefixTypeEnum.IPV4UNI;
            case 1:
                return BgpcommPrefixTypeEnum.IPV4MULTI;
            case 2:
                return BgpcommPrefixTypeEnum.IPV4VPN;
            case 3:
                return BgpcommPrefixTypeEnum.IPV6UNI;
            case 4:
                return BgpcommPrefixTypeEnum.IPV6VPN;
            case 5:
                return BgpcommPrefixTypeEnum.IPV4FLOW;
            case 6:
                return BgpcommPrefixTypeEnum.L2VPNAD;
            case 7:
                return BgpcommPrefixTypeEnum.MVPN;
            case 8:
                return BgpcommPrefixTypeEnum.EVPN;
            case 9:
                return BgpcommPrefixTypeEnum.IPV4VPNMCAST;
            case 10:
                return BgpcommPrefixTypeEnum.LS;
            case 11:
                return BgpcommPrefixTypeEnum.MDT;
            default :
                return null;
        }
    }

    /**
     * Returns the attribute bgpcommPrefixTypeEnum.
     *
     * @return value of bgpcommPrefixTypeEnum
     */
    public int bgpcommPrefixTypeEnum() {
        return bgpcommPrefixTypeEnum;
    }

    /**
     * Returns the object of bgpcommPrefixTypeEnum fromString input String.
     *
     * @param valInString input String
     * @return Object of bgpcommPrefixTypeEnum
     */
    public static BgpcommPrefixTypeEnum fromString(String valInString) {
        try {
            int tmpVal = Integer.parseInt(valInString);
            return of(tmpVal);
        } catch (Exception e) {
        }
        return null;
    }
}
