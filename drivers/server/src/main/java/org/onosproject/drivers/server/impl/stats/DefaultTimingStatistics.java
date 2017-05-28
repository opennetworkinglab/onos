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

import org.onosproject.drivers.server.stats.TimingStatistics;

import com.google.common.base.MoreObjects;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Default implementation for timing statistics.
 */
public final class DefaultTimingStatistics implements TimingStatistics {

    private final long deployCommandParsingTime;
    private final long deployCommandLaunchingTime;
    private long autoscaleTime;

    private DefaultTimingStatistics(
            long parsingTime,
            long launchingTime,
            long autoscaleTime) {
        checkArgument(parsingTime   >= 0, "Parsing time is negative");
        checkArgument(launchingTime >= 0, "Launching time is negative");
        checkArgument(autoscaleTime >= 0, "Autoscale time is negative");

        this.deployCommandParsingTime   = parsingTime;
        this.deployCommandLaunchingTime = launchingTime;
        this.autoscaleTime = autoscaleTime;
    }

    // Constructor for serializer
    private DefaultTimingStatistics() {
        this.deployCommandParsingTime   = 0;
        this.deployCommandLaunchingTime = 0;
        this.autoscaleTime = 0;
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
    public long autoscaleTime() {
        return this.autoscaleTime;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .omitNullValues()
                .add("parsing",   this.deployCommandParsingTime())
                .add("launching", this.deployCommandLaunchingTime())
                .add("total",     this.totalDeploymentTime())
                .add("autoscale", this.autoscaleTime())
                .toString();
    }

    public static final class Builder {

        long deployCommandParsingTime;
        long deployCommandLaunchingTime;
        long autoscaleTime;

        private Builder() {

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
         * @param autoscaleTime time required to autoscale
         * @return builder object
         */
        public Builder setAutoscaleTime(long autoscaleTime) {
            this.autoscaleTime = autoscaleTime;

            return this;
        }

        /**
         * Creates a DefaultTimingStatistics object.
         *
         * @return DefaultTimingStatistics object
         */
        public DefaultTimingStatistics build() {
            return new DefaultTimingStatistics(
                deployCommandParsingTime,
                deployCommandLaunchingTime,
                autoscaleTime
            );
        }
    }

}
