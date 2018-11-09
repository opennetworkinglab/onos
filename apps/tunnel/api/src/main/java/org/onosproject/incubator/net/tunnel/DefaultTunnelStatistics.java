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

package org.onosproject.incubator.net.tunnel;

import com.google.common.annotations.Beta;

import java.time.Duration;
import java.util.List;

/**
 * Default implementation of immutable tunnel statistics.
 */
@Beta
public final class DefaultTunnelStatistics implements TunnelStatistics {
    private final TunnelId tunnelId;
    private final double bwUtilization;
    private final double packetLossRatio;
    private final Duration flowDelay;
    private final List<String> alarms;

    private DefaultTunnelStatistics(TunnelId tunnelId,
                                    double bwUtilization,
                                    double packetLossRatio,
                                    Duration flowDelay,
                                    List<String> alarms) {
        this.tunnelId = tunnelId;
        this.bwUtilization = bwUtilization;
        this.packetLossRatio = packetLossRatio;
        this.flowDelay = flowDelay;
        this.alarms = alarms;
    }

    private DefaultTunnelStatistics() {
        this.tunnelId = null;
        this.bwUtilization = 0;
        this.packetLossRatio = 0;
        this.flowDelay = null;
        this.alarms = null;
    }


    @Override
    public TunnelId id() {
        return this.tunnelId;
    }

    @Override
    public double bandwidthUtilization() {
        return this.bwUtilization;
    }

    @Override
    public double packetLossRate() {
        return this.packetLossRatio;
    }

    @Override
    public Duration flowDelay() {
        return this.flowDelay;
    }


    @Override
    public List<String> alarms() {
        return this.alarms;
    }

    /**
     * Builder for tunnelStatistics.
     */
    public static final class Builder {
        TunnelId tunnelId;
        double bwUtilization;
        double packetLossRatio;
        Duration flowDelay;
        List<String> alarms;

        public Builder() {

        }

        /**
         * Set tunnel id.
         *
         * @param tunnelId tunnel id
         * @return builder object
         */
        public Builder setTunnelId(TunnelId tunnelId) {
            this.tunnelId = tunnelId;

            return this;
        }

        /**
         * set bandwidth utilization.
         *
         * @param bwUtilization bandwidth utilization
         * @return builder object
         */
        public Builder setBwUtilization(double bwUtilization) {
            this.bwUtilization = bwUtilization;

            return this;
        }

        /**
         * Set packet loss ratio.
         *
         * @param packetLossRatio packet loss ratio
         * @return builder object
         */
        public Builder setPacketLossRatio(double packetLossRatio) {
            this.packetLossRatio = packetLossRatio;

            return this;
        }

        /**
         * Set flow delay.
         *
         * @param flowDelay flow delay
         * @return builder object
         */
        public Builder setFlowDelay(Duration flowDelay) {
            this.flowDelay = flowDelay;

            return this;
        }

        /**
         * Set alarms.
         *
         * @param alarms alarms of a tunnel
         * @return builder object
         */
        public Builder setAlarms(List<String> alarms) {
            this.alarms = alarms;

            return this;
        }

        /**
         * Creates a TunnelStatistics object.
         *
         * @return DefaultTunnelStatistics
         */
        public DefaultTunnelStatistics build() {
            return new DefaultTunnelStatistics(tunnelId,
                                               bwUtilization,
                                               packetLossRatio,
                                               flowDelay,
                                               alarms);
        }
    }
}
