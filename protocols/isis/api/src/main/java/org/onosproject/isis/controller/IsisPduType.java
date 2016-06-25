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
package org.onosproject.isis.controller;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

/**
 * Representation of ISIS PDU types.
 */
public enum IsisPduType {

    /**
     * Represents Level-1 LAN hello packet.
     */
    L1HELLOPDU(15),
    /**
     * Represents Level-2 LAN hello packet.
     */
    L2HELLOPDU(16),
    /**
     * Represents point-to-point hello packet.
     */
    P2PHELLOPDU(17),
    /**
     * Represents Level-1 link state packet.
     */
    L1LSPDU(18),
    /**
     * Represents Level-2 link state packet.
     */
    L2LSPDU(20),
    /**
     * Represents Level-1 complete sequence number packet.
     */
    L1CSNP(24),
    /**
     * Represents Level-2 complete sequence number packet.
     */
    L2CSNP(25),
    /**
     * Represents Level-1 partial sequence number packet.
     */
    L1PSNP(26),
    /**
     * Represents Level-2 partial sequence number packet.
     */
    L2PSNP(27);

    // Reverse lookup table
    private static final Map<Integer, IsisPduType> LOOKUP = new HashMap<>();

    // Populate the lookup table on loading time
    static {
        for (IsisPduType isisPduType : EnumSet.allOf(IsisPduType.class)) {
            LOOKUP.put(isisPduType.value(), isisPduType);
        }
    }

    private int value;

    /**
     * Creates an instance of ISIS PDU type.
     *
     * @param value represents ISIS PDU type
     */
    private IsisPduType(int value) {
        this.value = value;
    }

    /**
     * Gets the enum instance from type value - reverse lookup purpose.
     *
     * @param pduTypeValue PDU type value
     * @return ISIS PDU type instance
     */
    public static IsisPduType get(int pduTypeValue) {
        return LOOKUP.get(pduTypeValue);
    }

    /**
     * Gets the value representing PDU type.
     *
     * @return value represents PDU type
     */
    public int value() {
        return value;
    }
}