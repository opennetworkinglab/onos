/*
 * Copyright 2019-present Open Networking Foundation
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
package org.onosproject.openstacktelemetry.api;

/**
 * Link stats info interface.
 */
public interface LinkStatsInfo {

    /**
     * Obtains TX packet count.
     *
     * @return TX packet count
     */
    long getTxPacket();

    /**
     * Obtains RX packet count.
     *
     * @return RX packet count
     */
    long getRxPacket();

    /**
     * Obtains TX byte count.
     *
     * @return TX byte count
     */
    long getTxByte();

    /**
     * Obtains RX byte count.
     *
     * @return RX byte count
     */
    long getRxByte();

    /**
     * Obtains TX drop count.
     *
     * @return TX drop count
     */
    long getTxDrop();

    /**
     * Obtains RX drop count.
     *
     * @return RX drop count
     */
    long getRxDrop();

    /**
     * Obtains timestamp.
     *
     * @return timestamp
     */
    long getTimestamp();

    /**
     * Builder class of LinkStatsInfo.
     */
    interface Builder {

        /**
         * Sets TX packet count.
         *
         * @param txPacket TX packet count
         * @return builder instance
         */
        Builder withTxPacket(long txPacket);

        /**
         * Sets RX packet count.
         *
         * @param rxPacket RX packet count
         * @return builder instance
         */
        Builder withRxPacket(long rxPacket);

        /**
         * Sets TX byte count.
         *
         * @param txByte TX byte count
         * @return builder instance
         */
        Builder withTxByte(long txByte);

        /**
         * Sets RX byte count.
         *
         * @param rxByte RX byte count
         * @return builder instance
         */
        Builder withRxByte(long rxByte);

        /**
         * Sets TX drop count.
         *
         * @param txDrop TX drop count
         * @return builder instance
         */
        Builder withTxDrop(long txDrop);

        /**
         * Sets RX drop count.
         *
         * @param rxDrop RX drop count
         * @return builder instance
         */
        Builder withRxDrop(long rxDrop);

        /**
         * Sets timestamp.
         *
         * @param timestamp timestamp
         * @return builder instance
         */
        Builder withTimestamp(long timestamp);

        /**
         * Creates a LinkStatsInfo instance.
         *
         * @return builder instance
         */
        LinkStatsInfo build();
    }
}
