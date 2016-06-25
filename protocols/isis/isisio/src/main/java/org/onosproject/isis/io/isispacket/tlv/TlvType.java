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
package org.onosproject.isis.io.isispacket.tlv;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

/**
 * Representation of various values for TLV types.
 */
public enum TlvType {
    AREAADDRESS(1),
    ISREACHABILITY(2),
    ISNEIGHBORS(6),
    PADDING(8),
    LSPENTRY(9),
    AUTHENTICATION(10),
    HOSTNAME(137),
    EXTENDEDISREACHABILITY(22),
    ISALIAS(24),
    IPINTERNALREACHABILITY(128),
    PROTOCOLSUPPORTED(129),
    IPEXTERNALREACHABILITY(130),
    IPEXTENDEDREACHABILITY(135),
    IDRPINFORMATION(131),
    IPINTERFACEADDRESS(132),
    ADJACENCYSTATE(240);

    // Reverse lookup table
    private static final Map<Integer, TlvType> LOOKUP = new HashMap<>();

    // Populate the lookup table on loading time
    static {
        for (TlvType isisTlvType : EnumSet.allOf(TlvType.class)) {
            LOOKUP.put(isisTlvType.value(), isisTlvType);
        }
    }

    private int value;

    /**
     * Sets the TLV type value.
     *
     * @param value value.
     */
    TlvType(int value) {
        this.value = value;
    }

    /**
     * Gets the enum instance from type value - reverse lookup purpose.
     *
     * @param tlvTypeValue TLV type value
     * @return ISIS TLV type instance
     */
    public static TlvType get(int tlvTypeValue) {
        return LOOKUP.get(tlvTypeValue);
    }

    /**
     * Gets value.
     *
     * @return value
     */
    public int value() {
        return value;
    }
}