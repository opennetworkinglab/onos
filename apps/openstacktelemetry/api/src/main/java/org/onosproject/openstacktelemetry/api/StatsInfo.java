/*
 * Copyright 2018-present Open Networking Foundation
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
 * Stats info interface.
 */
public interface StatsInfo {

    /**
     * Obtains startup time.
     *
     * @return startup time
     */
    long startupTime();

    /**
     * Obtains first packet arrival time.
     *
     * @return first packet arrival time
     */
    long fstPktArrTime();

    /**
     * Obtains last packet offset.
     *
     * @return last packet offset
     */
    int lstPktOffset();

    /**
     * Obtains previous accumulated bytes.
     *
     * @return previous accumulated bytes
     */
    long prevAccBytes();

    /**
     * Obtains previous accumulated packets.
     *
     * @return previous accumulated packets
     */
    int prevAccPkts();

    /**
     * Obtains current accumulated bytes.
     *
     * @return current accumulated bytes
     */
    long currAccBytes();

    /**
     * Obtains current accumulated packets.
     *
     * @return current accumulated packets
     */
    int currAccPkts();

    /**
     * Obtains error packets stats.
     *
     * @return error packets stats
     */
    short errorPkts();

    /**
     * Obtains dropped packets stats.
     *
     * @return dropped packets stats
     */
    short dropPkts();

    interface Builder {

        /**
         * Sets startup time.
         *
         * @param startupTime startup time
         * @return builder instance
         */
        Builder withStartupTime(long startupTime);

        /**
         * Sets first packet arrival time.
         *
         * @param fstPktArrTime first packet arrival time
         * @return builder instance
         */
        Builder withFstPktArrTime(long fstPktArrTime);

        /**
         * Sets last packet offset.
         *
         * @param lstPktOffset last packet offset
         * @return builder instance
         */
        Builder withLstPktOffset(int lstPktOffset);

        /**
         * Sets previous accumulated bytes.
         *
         * @param prevAccBytes previous accumulated bytes
         * @return builder instance
         */
        Builder withPrevAccBytes(long prevAccBytes);

        /**
         * Sets previous accumulated packets.
         *
         * @param prevAccPkts previous accumulated packets
         * @return builder instance
         */
        Builder withPrevAccPkts(int prevAccPkts);

        /**
         * Sets current accumulated bytes.
         *
         * @param currAccBytes current accumulated bytes
         * @return builder instance
         */
        Builder withCurrAccBytes(long currAccBytes);

        /**
         * Sets currently accumulated packets.
         *
         * @param currAccPkts current accumulated packets
         * @return builder instance
         */
        Builder withCurrAccPkts(int currAccPkts);

        /**
         * Sets error packets stats.
         *
         * @param errorPkts error packets stats
         * @return builder instance
         */
        Builder withErrorPkts(short errorPkts);

        /**
         * Sets dropped packets stats.
         *
         * @param dropPkts dropped packets stats
         * @return builder instance
         */
        Builder withDropPkts(short dropPkts);

        /**
         * Creates flow level stats info instance.
         *
         * @return stats info instance
         */
        StatsInfo build();
    }
}
