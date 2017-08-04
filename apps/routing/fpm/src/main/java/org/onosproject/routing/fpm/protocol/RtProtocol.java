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

package org.onosproject.routing.fpm.protocol;

/**
 * RtNetlink protocol value.
 * <p>
 * This is a subset of the protocol values used in rtnetlink.
 * Taken from linux/rtnetlink.h
 * </p>
 */
public enum RtProtocol {
    /**
     * Unspecified.
     */
    UNSPEC((short) 0),

    /**
     * Route installed by ICMP redirects.
     */
    REDIRECT((short) 1),

    /**
     * Route installed by kernel.
     */
    KERNEL((short) 2),

    /**
     * Route installed during boot.
     */
    BOOT((short) 3),

    /**
     * Route installed by administrator.
     */
    STATIC((short) 4),

    /**
     * GateD.
     */
    GATED((short) 8),

    /**
     * RDISC/ND router advertisements.
     */
    RA((short) 9),

    /**
     * Merit MRT.
     */
    MRT((short) 10),

    /**
     * Zebra.
     */
    ZEBRA((short) 11),

    /**
     * BIRD.
     */
    BIRD((short) 12),

    /**
     * DECnet routing daemon.
     */
    DNROUTED((short) 13),

    /**
     * XORP.
     */
    XORP((short) 14),

    /**
     * Netsukuku.
     */
    NTK((short) 15),

    /**
     * DHCP client.
     */
    DHCP((short) 16),

    /**
     * Multicast daemon.
     */
    MROUTED((short) 17),

    /**
     * Unknown.
     */
    UNKNOWN((short) 0);

    private final short value;

    /**
     * Constructor.
     *
     * @param value value
     */
    RtProtocol(short value) {
        this.value = value;
    }

    /**
     * Returns the value.
     *
     * @return value
     */
    public short value() {
        return value;
    }

    /**
     * Gets the RtProtocol for the given integer value.
     *
     * @param value value
     * @return RtProtocol, or null if unsupported type value
     */
    public static RtProtocol get(short value) {
        for (RtProtocol p : RtProtocol.values()) {
            if (p.value() == value) {
                return p;
            }
        }
        return UNKNOWN;
    }
}
