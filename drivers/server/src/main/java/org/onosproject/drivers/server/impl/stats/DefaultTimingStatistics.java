/*
 * Copyright 2017-present Open Networking Foundation
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

package org.onosproject.drivers.server.impl.stats;

import org.onosproject.drivers.server.stats.MonitoringUnit;
import org.onosproject.drivers.server.stats.TimingStatistics;

import com.google.common.base.Strings;
import com.google.common.base.MoreObjects;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.onosproject.drivers.server.Constants.MSG_STATS_TIMING_AUTO_SCALE_NEGATIVE;
import static org.onosproject.drivers.server.Constants.MSG_STATS_TIMING_LAUNCH_NEGATIVE;
import static org.onosproject.drivers.server.Constants.MSG_STATS_TIMING_PARSE_NEGATIVE;
import static org.onosproject.drivers.server.Constants.MSG_STATS_UNIT_NULL;
import static org.onosproject.drivers.server.stats.MonitoringUnit.LatencyUnit;

/**
 * Default implementation for timing statistics.
 */
public final class DefaultTimingStatistics implements TimingStatistics {

    private static final LatencyUnit DEF_TIME_UNIT = LatencyUnit.NANO_SECOND;

    private final MonitoringUnit unit;
    private final long deployCommandParsingTime;
    private final long deployCommandLaunchingTime;
    private final long autoScaleTime;

    private DefaultTimingStatistics(
            MonitoringUnit unit,
            long parsingTime,
            long launchingTime,
            long autoScaleTime) {
        checkNotNull(unit, MSG_STATS_UNIT_NULL);
        checkArgument(parsingTime >= 0, MSG_STATS_TIMING_PARSE_NEGATIVE);
        checkArgument(launchingTime >= 0, MSG_STATS_TIMING_LAUNCH_NEGATIVE);
        checkArgument(autoScaleTime >= 0, MSG_STATS_TIMING_AUTO_SCALE_NEGATIVE);

        this.unit = unit;
        this.deployCommandParsingTime = parsingTime;
        this.deployCommandLaunchingTime = launchingTime;
        this.autoScaleTime = autoScaleTime;
    }

    // Constructor for serializer
    private DefaultTimingStatistics() {
        this.unit = null;
        this.deployCommandParsingTime = 0;
        this.deployCommandLaunchingTime = 0;
        this.autoScaleTime = 0;
    }

    /**
     * Creates a builder for DefaultTimingStatistics object.
     *
     * @return builder object for DefaultTimingStatistics object
     */
    public static DefaultTimingStatistics.Builder builder() {
        return new Builder();
    }

    @Override
    public MonitoringUnit unit() {
        return this.unit;
    }

    @Override
    public long deployCommandParsingTime() {
        return this.deployCommandParsingTime;
    }

    @Override
    public long deployCommandLaunchingTime() {
        return this.deployCommandLaunchingTime;
    }

    @Override
    public long totalDeploymentTime() {
        return this.deployCommandParsingTime + this.deployCommandLaunchingTime;
    }

    @Override
    public long autoScaleTime() {
        return this.autoScaleTime;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .omitNullValues()
                .add("unit", this.unit())
                .add("parsingTime", this.deployCommandParsingTime())
                .add("launchingTime", this.deployCommandLaunchingTime())
                .add("deploymentTime", this.totalDeploymentTime())
                .add("autoScaleTime", this.autoScaleTime())
                .toString();
    }

    public static final class Builder {

        MonitoringUnit unit = DEF_TIME_UNIT;
        long deployCommandParsingTime;
        long deployCommandLaunchingTime;
        long autoScaleTime;

        private Builder() {

        }

        /**
         * Sets time statistics unit.
         *
         * @param unitStr time statistics unit as a string
         * @return builder object
         */
        public Builder setUnit(String unitStr) {
            if (!Strings.isNullOrEmpty(unitStr)) {
                this.unit = LatencyUnit.getByName(unitStr);
            }

            return this;
        }

        /**
         * Sets parsing time.
         *
         * @param parsingTime parsing time
         * @return builder object
         */
        public Builder setParsingTime(long parsingTime) {
            this.deployCommandParsingTime = parsingTime;

            return this;
        }

        /**
         * Sets launching time.
         *
         * @param launchingTime launching time
         * @return builder object
         */
        public Builder setLaunchingTime(long launchingTime) {
            this.deployCommandLaunchingTime = launchingTime;

            return this;
        }

        /**
         * Sets autoscale time.
         *
         * @param autoScaleTime time required to autoscale
         * @return builder object
         */
        public Builder setAutoScaleTime(long autoScaleTime) {
            this.autoScaleTime = autoScaleTime;

            return this;
        }

        /**
         * Creates a DefaultTimingStatistics object.
         *
         * @return DefaultTimingStatistics object
         */
        public DefaultTimingStatistics build() {
            return new DefaultTimingStatistics(
                unit, deployCommandParsingTime,
                deployCommandLaunchingTime, autoScaleTime);
        }

    }

}
