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

package org.onosproject.yang.gen.v1.ne.bgpcomm.type.rev20141225.nebgpcommtype.bgpcommimrouteprotocol;

/**
 * Represents ENUM data of bgpcommImRouteProtocolEnum.
 */
public enum BgpcommImRouteProtocolEnum {

    /**
     * Represents direct.
     */
    DIRECT(0),

    /**
     * Represents ospf.
     */
    OSPF(1),

    /**
     * Represents isis.
     */
    ISIS(2),

    /**
     * Represents yangAutoPrefixStatic.
     */
    STATIC(3),

    /**
     * Represents rip.
     */
    RIP(4),

    /**
     * Represents ospfv3.
     */
    OSPFV3(5),

    /**
     * Represents ripng.
     */
    RIPNG(6),

    /**
     * Represents unr.
     */
    UNR(7),

    /**
     * Represents opRoute.
     */
    OP_ROUTE(8);

    private int bgpcommImRouteProtocolEnum;

    /**
     * Creates an instance of bgpcommImRouteProtocolEnum.
     *
     * @param value value of bgpcommImRouteProtocolEnum
     */
    BgpcommImRouteProtocolEnum(int value) {
        bgpcommImRouteProtocolEnum = value;
    }

    /**
     * Returns the object of bgpcommImRouteProtocolEnumForTypeInt.
     *
     * @param value value of bgpcommImRouteProtocolEnumForTypeInt
     * @return Object of bgpcommImRouteProtocolEnumForTypeInt
     */
    public static BgpcommImRouteProtocolEnum of(int value) {
        switch (value) {
            case 0:
                return BgpcommImRouteProtocolEnum.DIRECT;
            case 1:
                return BgpcommImRouteProtocolEnum.OSPF;
            case 2:
                return BgpcommImRouteProtocolEnum.ISIS;
            case 3:
                return BgpcommImRouteProtocolEnum.STATIC;
            case 4:
                return BgpcommImRouteProtocolEnum.RIP;
            case 5:
                return BgpcommImRouteProtocolEnum.OSPFV3;
            case 6:
                return BgpcommImRouteProtocolEnum.RIPNG;
            case 7:
                return BgpcommImRouteProtocolEnum.UNR;
            case 8:
                return BgpcommImRouteProtocolEnum.OP_ROUTE;
            default :
                return null;
        }
    }

    /**
     * Returns the attribute bgpcommImRouteProtocolEnum.
     *
     * @return value of bgpcommImRouteProtocolEnum
     */
    public int bgpcommImRouteProtocolEnum() {
        return bgpcommImRouteProtocolEnum;
    }

    /**
     * Returns the object of bgpcommImRouteProtocolEnum fromString input String.
     *
     * @param valInString input String
     * @return Object of bgpcommImRouteProtocolEnum
     */
    public static BgpcommImRouteProtocolEnum fromString(String valInString) {
        try {
            int tmpVal = Integer.parseInt(valInString);
            return of(tmpVal);
        } catch (Exception e) {
        }
        return null;
    }
}
