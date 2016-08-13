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
package org.onosproject.net.device;

import org.onosproject.net.DeviceId;

/**
 * Default implementation of immutable port statistics.
 */
public final class DefaultPortStatistics implements PortStatistics {

    private final DeviceId deviceId;
    private final int  port;
    private final long packetsReceived;
    private final long packetsSent;
    private final long bytesReceived;
    private final long bytesSent;
    private final long packetsRxDropped;
    private final long packetsTxDropped;
    private final long packetsRxErrors;
    private final long packetsTxErrors;
    private final long durationSec;
    private final long durationNano;

    private DefaultPortStatistics(DeviceId deviceId,
                                  int  port,
                                  long packetsReceived,
                                  long packetsSent,
                                  long bytesReceived,
                                  long bytesSent,
                                  long packetsRxDropped,
                                  long packetsTxDropped,
                                  long packetsRxErrors,
                                  long packetsTxErrors,
                                  long durationSec,
                                  long durationNano) {
        this.deviceId = deviceId;
        this.port = port;
        this.packetsReceived = packetsReceived;
        this.packetsSent = packetsSent;
        this.bytesReceived = bytesReceived;
        this.bytesSent = bytesSent;
        this.packetsRxDropped = packetsRxDropped;
        this.packetsTxDropped = packetsTxDropped;
        this.packetsRxErrors = packetsRxErrors;
        this.packetsTxErrors = packetsTxErrors;
        this.durationSec = durationSec;
        this.durationNano = durationNano;
    }

    // Constructor for serializer
    private DefaultPortStatistics() {
        this.deviceId = null;
        this.port = 0;
        this.packetsReceived = 0;
        this.packetsSent = 0;
        this.bytesReceived = 0;
        this.bytesSent = 0;
        this.packetsRxDropped = 0;
        this.packetsTxDropped = 0;
        this.packetsRxErrors = 0;
        this.packetsTxErrors = 0;
        this.durationSec = 0;
        this.durationNano = 0;
    }

    /**
     * Creates a builder for DefaultPortStatistics object.
     *
     * @return builder object for DefaultPortStatistics object
     */
    public static DefaultPortStatistics.Builder builder() {
        return new Builder();
    }

    @Override
    public int port() {
        return this.port;
    }

    @Override
    public long packetsReceived() {
        return this.packetsReceived;
    }

    @Override
    public long packetsSent() {
        return this.packetsSent;
    }

    @Override
    public long bytesReceived() {
        return this.bytesReceived;
    }

    @Override
    public long bytesSent() {
        return this.bytesSent;
    }

    @Override
    public long packetsRxDropped() {
        return this.packetsRxDropped;
    }

    @Override
    public long packetsTxDropped() {
        return this.packetsTxDropped;
    }

    @Override
    public long packetsRxErrors() {
        return this.packetsRxErrors;
    }

    @Override
    public long packetsTxErrors() {
        return this.packetsTxErrors;
    }

    @Override
    public long durationSec() {
        return this.durationSec;
    }

    @Override
    public long durationNano() {
        return this.durationNano;
    }

    @Override
    public boolean isZero() {
        return  bytesReceived() == 0 &&
                bytesSent() == 0 &&
                packetsReceived() == 0 &&
                packetsRxDropped() == 0 &&
                packetsSent() == 0 &&
                packetsTxDropped() == 0;
    }

    @Override
    public String toString() {
        return "device: " + deviceId + ", " +
                "port: " + this.port + ", " +
                "pktRx: " + this.packetsReceived + ", " +
                "pktTx: " + this.packetsSent + ", " +
                "byteRx: " + this.bytesReceived + ", " +
                "byteTx: " + this.bytesSent + ", " +
                "pktRxErr: " + this.packetsRxErrors + ", " +
                "pktTxErr: " + this.packetsTxErrors + ", " +
                "pktRxDrp: " + this.packetsRxDropped + ", " +
                "pktTxDrp: " + this.packetsTxDropped;
    }

    public static final class Builder {

        DeviceId deviceId;
        int port;
        long packetsReceived;
        long packetsSent;
        long bytesReceived;
        long bytesSent;
        long packetsRxDropped;
        long packetsTxDropped;
        long packetsRxErrors;
        long packetsTxErrors;
        long durationSec;
        long durationNano;

        private Builder() {

        }

        /**
         * Sets port number.
         *
         * @param port port number
         * @return builder object
         */
        public Builder setPort(int port) {
            this.port = port;

            return this;
        }

        /**
         * Sets the device identifier.
         *
         * @param deviceId device identifier
         * @return builder object
         */
        public Builder setDeviceId(DeviceId deviceId) {
            this.deviceId = deviceId;

            return this;
        }

        /**
         * Sets the number of packet received.
         *
         * @param packets number of packets received
         * @return  builder object
         */
        public Builder setPacketsReceived(long packets) {
            packetsReceived = packets;

            return this;
        }

        /**
         * Sets the number of packets sent.
         *
         * @param packets number of packets sent
         * @return  builder object
         */
        public Builder setPacketsSent(long packets) {
            packetsSent = packets;

            return this;
        }

        /**
         * Sets the number of received bytes.
         *
         * @param bytes number of received bytes.
         * @return  builder object
         */
        public Builder setBytesReceived(long bytes) {
            bytesReceived = bytes;

            return this;
        }

        /**
         * Sets the number of sent bytes.
         *
         * @param bytes number of sent bytes
         * @return builder object
         */
        public Builder setBytesSent(long bytes) {
            bytesSent = bytes;

            return this;
        }

        /**
         * Sets the number of packets dropped by RX.
         *
         * @param packets  number of packets dropped by RX
         * @return builder object
         */
        public Builder setPacketsRxDropped(long packets) {
            packetsRxDropped = packets;

            return this;
        }

        /**
         * Sets the number of packets dropped by TX.
         *
         * @param packets number of packets
         * @return builder object
         */
        public Builder setPacketsTxDropped(long packets) {
            packetsTxDropped = packets;

            return this;
        }

        /**
         * Sets the number of receive errors.
         *
         * @param packets number of receive errors
         * @return builder object
         */
        public Builder setPacketsRxErrors(long packets) {
            packetsRxErrors = packets;

            return this;
        }

        /**
         * Sets the number of transmit errors.
         *
         * @param packets number of transmit errors
         * @return builder object
         */
        public Builder setPacketsTxErrors(long packets) {
            packetsTxErrors = packets;

            return this;
        }

        /**
         * Sets the time port has been alive in seconds.
         *
         * @param sec time port has been alive in seconds
         * @return builder object
         */
        public Builder setDurationSec(long sec) {
            durationSec = sec;

            return this;
        }

        /**
         * Sets the time port has been alive in nano seconds.
         *
         * @param nano time port has been alive in nano seconds
         * @return builder object
         */
        public Builder setDurationNano(long nano) {
            durationNano = nano;

            return this;
        }

        /**
         * Creates a PortStatistics object.
         *
         * @return DefaultPortStatistics object
         */
        public DefaultPortStatistics build() {
            return new DefaultPortStatistics(
                    deviceId,
                    port,
                    packetsReceived,
                    packetsSent,
                    bytesReceived,
                    bytesSent,
                    packetsRxDropped,
                    packetsTxDropped,
                    packetsRxErrors,
                    packetsTxErrors,
                    durationSec,
                    durationNano);
        }

    }

}
