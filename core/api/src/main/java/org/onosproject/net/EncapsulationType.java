/*
 * Copyright 2015-present Open Networking Laboratory
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

package org.onosproject.net;

public enum EncapsulationType {
    /**
     * Indicates no encapsulation.
     */
    NONE,
    /**
     * Indicates an MPLS encapsulation.
     */
    MPLS,
    /**
     * Indicates a VLAN encapsulation.
     */
    VLAN;

    /**
     * Alternative method to valueOf. It returns the encapsulation type constant
     * corresponding to the given string. If the parameter does not match a
     * constant name, or is null, {@link #NONE} is returned.
     *
     * @param encap the string representing the encapsulation type
     * @return the EncapsulationType constant corresponding to the string given
     */
    public static EncapsulationType enumFromString(String encap) {
        // Return EncapsulationType.NONE if the value is not found, or if null
        // or an empty string are given
        EncapsulationType type = NONE;
        if (encap != null && !encap.isEmpty()) {
            for (EncapsulationType t : values()) {
                if (encap.equalsIgnoreCase(t.toString())) {
                    type = valueOf(encap.toUpperCase());
                    break;
                }
            }
        }
        return type;
    }
}