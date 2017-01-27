/*
 * Copyright 2017-present Open Networking Laboratory
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

package org.onosproject.routing.fpm.protocol;

/**
 * Netlink message types.
 * <p>
 * This is a subset of the types used for routing messages (rtnelink).
 * Taken from linux/rtnetlink.h
 * </p>
 */
public enum NetlinkMessageType {
    RTM_NEWROUTE(24),
    RTM_DELROUTE(25),
    RTM_GETROUTE(26);

    private final int type;

    /**
     * Enum constructor.
     *
     * @param type integer type value
     */
    NetlinkMessageType(int type) {
        this.type = type;
    }

    /**
     * Returns the integer type value for this message type.
     *
     * @return type value
     */
    public int type() {
        return type;
    }

    /**
     * Gets the NetlinkMessageType for the given integer type value.
     *
     * @param type type value
     * @return Netlink message type, or null if unsupported type value
     */
    public static NetlinkMessageType get(int type) {
        for (NetlinkMessageType m : NetlinkMessageType.values()) {
            if (m.type() == type) {
                return m;
            }
        }
        return null;
    }
}
