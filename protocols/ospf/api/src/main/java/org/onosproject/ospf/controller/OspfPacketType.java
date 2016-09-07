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
package org.onosproject.ospf.controller;

/**
 * Representation of different OSPF packet types.
 */
public enum OspfPacketType {

    /**
     * OSPF hello packet.
     */
    HELLO(1),
    /**
     * OSPF device description packet.
     */
    DD(2),
    /**
     * OSPF link state request packet.
     */
    LSREQUEST(3),
    /**
     * OSPF link state update packet.
     */
    LSUPDATE(4),
    /**
     * OSPF link state acknowledge packet.
     */
    LSAACK(5);

    private int value;

    /**
     * Creates instance of OSPF packet types.
     *
     * @param value OSPF packet types
     */
    OspfPacketType(int value) {
        this.value = value;
    }

    /**
     * Gets the value.
     *
     * @return value
     */
    public int value() {
        return value;
    }
}