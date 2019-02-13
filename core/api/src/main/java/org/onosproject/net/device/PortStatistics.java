/*
 * Copyright 2015-present Open Networking Foundation
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
package org.onosproject.net.device;

import org.onosproject.net.Annotated;
import org.onosproject.net.Annotations;
import org.onosproject.net.PortNumber;

import static org.onosproject.net.DefaultAnnotations.EMPTY;

/**
 * Statistics of a port.
 */
public interface PortStatistics extends Annotated {

    /**
     * Returns the port number.
     *
     * @return port number
     */
    PortNumber portNumber();

    /**
     * Returns the number of packets received.
     *
     * @return the number of packets received
     */
    long packetsReceived();

    /**
     * Returns the number of packets sent.
     *
     * @return the number of packets sent
     */
    long packetsSent();

    /**
     * Returns the bytes received.
     *
     * @return the bytes received
     */
    long bytesReceived();

    /**
     * Returns the bytes sent.
     *
     * @return the bytes sent
     */
    long bytesSent();

    /**
     * Returns the number of packets dropped by RX.
     *
     * @return the number of packets dropped by RX
     */
    long packetsRxDropped();

    /**
     * Returns the number of packets dropped by TX.
     *
     * @return the number of packets dropped by TX
     */
    long packetsTxDropped();

    /**
     * Returns the number of transmit errors.
     *
     * @return the number of transmit errors
     */
    long packetsRxErrors();

    /**
     * Returns the number of receive errors.
     *
     * @return the number of receive error
     */
    long packetsTxErrors();

    /**
     * Returns the time port has been alive in seconds.
     *
     * @return the time port has been alive in seconds
     */
    long durationSec();

    /**
     * Returns the time port has been alive in nano seconds.
     *
     * @return the time port has been alive in nano seconds
     */
    long durationNano();

    @Override
    default Annotations annotations() {
        return EMPTY;
    }

    /**
     * Returns true if all the port stats are zero, excluding TxErrors and RxErrors.
     *
     * @return boolean true if all port stats are zero
     */
    boolean isZero();

}
