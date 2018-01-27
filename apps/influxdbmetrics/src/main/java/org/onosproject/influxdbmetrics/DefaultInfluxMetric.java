/*
 * Copyright 2016-present Open Networking Foundation
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
package org.onosproject.influxdbmetrics;

import static com.google.common.base.Preconditions.checkNotNull;

import java.time.Instant;
import java.time.format.DateTimeFormatter;

/**
 * Default implementation of influx metric.
 */
public final class DefaultInfluxMetric implements InfluxMetric {

    private double oneMinRate;
    private Instant time;

    private DefaultInfluxMetric(double oneMinRate, Instant time) {
        this.oneMinRate = oneMinRate;
        this.time = time;
    }

    @Override
    public double oneMinRate() {
        return oneMinRate;
    }

    @Override
    public Instant time() {
        return time;
    }

    public static final class Builder implements InfluxMetric.Builder {

        private double oneMinRate;
        private String timestamp;
        private static final String TIMESTAMP_MSG = "Must specify a timestamp.";
        private static final String ONE_MIN_RATE_MSG = "Must specify one minute rate.";

        public Builder() {
        }

        @Override
        public InfluxMetric.Builder oneMinRate(double rate) {
            this.oneMinRate = rate;
            return this;
        }

        @Override
        public InfluxMetric.Builder time(String time) {
            this.timestamp = time;
            return this;
        }

        @Override
        public InfluxMetric build() {
            checkNotNull(timestamp, TIMESTAMP_MSG);

            return new DefaultInfluxMetric(oneMinRate, parseTime(timestamp));
        }

        private Instant parseTime(String time) {
            return DateTimeFormatter.ISO_DATE_TIME.parse(time, Instant::from);
        }
    }
}
