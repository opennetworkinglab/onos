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

import org.jboss.netty.buffer.ChannelBuffer;
import org.onlab.packet.MacAddress;

/**
 * Representation of an ISIS Message.
 */
public interface IsisMessage {

    /**
     * Returns the interface index on which the message received.
     *
     * @return interface index on which the message received
     */
    int interfaceIndex();

    /**
     * Sets the interface index on which the message received.
     *
     * @param interfaceIndex interface index on which the message received
     */
    void setInterfaceIndex(int interfaceIndex);

    /**
     * Returns the interface mac address on which the message received.
     *
     * @return interface mac address on which the message received
     */
    MacAddress interfaceMac();

    /**
     * Sets the interface mac address on which the message received.
     *
     * @param interfaceMac mac address on which the message received
     */
    void setInterfaceMac(MacAddress interfaceMac);

    /**
     * Returns the mac address of the message sender.
     *
     * @return mac address of the message sender
     */
    MacAddress sourceMac();

    /**
     * Sets the mac address of the message sender.
     *
     * @param sourceMac mac address of the message sender
     */
    void setSourceMac(MacAddress sourceMac);

    /**
     * Returns the type of ISIS PDU.
     *
     * @return ISIS PDU type instance
     */
    IsisPduType isisPduType();

    /**
     * Reads from channel buffer and initializes the type of PDU.
     *
     * @param channelBuffer channel buffer instance
     */
    void readFrom(ChannelBuffer channelBuffer);

    /**
     * Returns IsisMessage as byte array.
     *
     * @return ISIS message as bytes
     */
    byte[] asBytes();
}
