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
package org.onosproject.ospf.protocol.util;

/**
 * Representation of an OSPF Interface states.
 */
public enum OspfInterfaceState {

    DOWN(1),
    LOOPBACK(2),
    WAITING(3),
    POINT2POINT(4),
    DROTHER(5),
    BDR(6),
    DR(7);

    private int value;

    /**
     * Creates an instance of interface state.
     *
     * @param value Interface state value
     */
    OspfInterfaceState(int value) {
        this.value = value;
    }

    /**
     * Gets value for Interface state.
     *
     * @return value Interface state
     */
    public int value() {
        return value;
    }

    /**
     * Gets interface state.
     *
     * @return interface state
     */
    public String interfaceState() {
        String state = null;
        switch (value) {
            case 1:
                state = "DOWN";
                break;
            case 2:
                state = "LOOPBACK";
                break;
            case 3:
                state = "WAITING";
                break;
            case 4:
                state = "POINT2POINT";
                break;
            case 5:
                state = "DROTHER";
                break;
            case 6:
                state = "BDR";
                break;
            case 7:
                state = "DR";
                break;
            default:
                break;
        }
        return state;
    }
}