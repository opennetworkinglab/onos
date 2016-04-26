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

package org.onosproject.bmv2.api.runtime;

import org.onlab.util.ImmutableByteSequence;

/**
 * A server that listens for requests from a BMv2 device.
 */
public interface Bmv2ControlPlaneServer {
    /**
     * Default listening port.
     */
    int DEFAULT_PORT = 40123;

    /**
     * Register the given hello listener, to be called each time a hello message is received from a BMv2 device.
     *
     * @param listener a hello listener
     */
    void addHelloListener(HelloListener listener);

    /**
     * Unregister the given hello listener.
     *
     * @param listener a hello listener
     */
    void removeHelloListener(HelloListener listener);

    /**
     * Register the given packet listener, to be called each time a packet-in message is received from a BMv2 device.
     *
     * @param listener a packet listener
     */
    void addPacketListener(PacketListener listener);

    /**
     * Unregister the given packet listener.
     *
     * @param listener a packet listener
     */
    void removePacketListener(PacketListener listener);

    interface HelloListener {

        /**
         * Handles a hello message.
         *
         * @param device the BMv2 device that originated the message
         */
        void handleHello(Bmv2Device device);
    }

    interface PacketListener {

        /**
         * Handles a packet-in message.
         *
         * @param device    the BMv2 device that originated the message
         * @param inputPort the device port where the packet was received
         * @param reason    a reason code
         * @param tableId   the table id that originated this packet-in
         * @param contextId the context id where the packet-in was originated
         * @param packet    the packet body
         */
        void handlePacketIn(Bmv2Device device, int inputPort, long reason, int tableId, int contextId,
                            ImmutableByteSequence packet);
    }
}