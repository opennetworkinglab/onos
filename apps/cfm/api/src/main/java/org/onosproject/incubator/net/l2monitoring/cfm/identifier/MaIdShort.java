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
package org.onosproject.incubator.net.l2monitoring.cfm.identifier;

/**
 * Representation of a Maintenance Association ID.
 *
 * The MA Id consists of a name and a name type.
 * In certain applications the MA name and type together with the MD Name and
 * name type are embedded in to a TLV and passed between systems, and so it is
 * important that all combinations of the name and name type can be represented here.
 *
 * IEEE 802.1Q Table 21-20—Short MA Name Format.
 */
public interface MaIdShort {

    /**
     * Get the MA name as a string.
     * @return A string representation of the name
     */
    String maName();

    /**
     * Get the length of the MD name.
     * @return The length of the name in bytes
     */
    int getNameLength();

    /**
     * The type of the name.
     * @return An enumerated value
     */
    MaIdType nameType();

    /**
     * Supported types of MD identifier.
     */
    enum MaIdType {
        /**
         * Implemented as {@link MaIdCharStr}.
         */
        CHARACTERSTRING,
        /**
         * Implemented as {@link MaId2Octet}.
         */
        TWOOCTET,
        /**
         * Implemented as {@link MaIdIccY1731}.
         */
        ICCY1731,
        /**
         * Implemented as {@link MaIdPrimaryVid}.
         */
        PRIMARYVID,
        /**
         * Implemented as {@link MaIdRfc2685VpnId}.
         */
        RFC2685VPNID
    }
}
