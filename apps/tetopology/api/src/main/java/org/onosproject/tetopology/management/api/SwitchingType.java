/*
 * Copyright 2016 Open Networking Laboratory
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
package org.onosproject.tetopology.management.api;

/**
 * Type of switching on a link.
 * See RFC 3471 for details.
 */
public enum SwitchingType {

    /**
     * Designates packet-switch capable-1 (PSC-1).
     */
    PACKET_SWITCH_CAPABLE1(1),

    /**
     * Designates packet-switch capable-2 (PSC-2).
     */
    PACKET_SWITCH_CAPABLE2(2),

    /**
     * Designates packet-switch capable-3 (PSC-3).
     */
    PACKET_SWITCH_CAPABLE3(3),

    /**
     * Designates packet-switch capable-4 (PSC-4).
     */
    PACKET_SWITCH_CAPABLE4(4),

    /**
     * Designates ethernet virtual private line (EVPL).
     */
    ETHERNET_VIRTUAL_PRIVATE_LINE(5),

    /**
     * Designates layer-2 switch capable (L2SC).
     */
    LAYER2_SWITCH_CAPABLE(51),

    /**
     * Designates time-division-multiplex capable (TDM).
     */
    TIME_DIVISION_MULTIPLEX_CAPABLE(100),

    /**
     * Designates OTN-TDM capable.
     */
    OTN_TDM_CAPABLE(101),

    /**
     * Designates lambda-switch capable (LSC).
     */
    LAMBDA_SWITCH_CAPABLE(150),

    /**
     * Designates fiber-switch capable (FSC).
     */
    FIBER_SWITCH_CAPABLE(200);

    private int value;

    /**
     * Creates an instance of a switching type constant corresponding
     * to the given integer value.
     *
     * @param value integer value
     */
    SwitchingType(int value) {
        this.value = value;
    }

    /**
     * Returns the integer value of the switching type.
     *
     * @return integer value
     */
    public int value() {
        return value;
    }

    /**
     * Returns the switching type corresponding to a given integer
     * value. If the given value is not valid, a null is returned.
     *
     * @param value integer value
     * @return corresponding switching type; or null if value is invalid
     */
    public static SwitchingType of(int value) {
        switch (value) {
            case 1:
                return PACKET_SWITCH_CAPABLE1;
            case 2:
                return PACKET_SWITCH_CAPABLE2;
            case 3:
                return PACKET_SWITCH_CAPABLE3;
            case 4:
                return PACKET_SWITCH_CAPABLE4;
            case 5:
                return ETHERNET_VIRTUAL_PRIVATE_LINE;
            case 51:
                return LAYER2_SWITCH_CAPABLE;
            case 100:
                return TIME_DIVISION_MULTIPLEX_CAPABLE;
            case 101:
                return OTN_TDM_CAPABLE;
            case 150:
                return LAMBDA_SWITCH_CAPABLE;
            case 200:
                return FIBER_SWITCH_CAPABLE;
            default:
                return null;
        }
    }
}
