/*
 * Copyright 2017-present Open Networking Foundation
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

package org.onosproject.bgpio.protocol.evpn;

/**
 * Enum to provide EVPN RouteType.
 */
public enum BgpEvpnRouteType {
    ETHERNET_AUTO_DISCOVERY(1), MAC_IP_ADVERTISEMENT(2),
    INCLUSIVE_MULTICASE_ETHERNET(3), ETHERNET_SEGMENT(4);
    int value;

    /**
     * Assign val with the value as the route type.
     *
     * @param val route type
     */
    BgpEvpnRouteType(int val) {
        value = val;
    }

    /**
     * Returns value of route type.
     *
     * @return route type
     */
    public byte getType() {
        return (byte) value;
    }
}
