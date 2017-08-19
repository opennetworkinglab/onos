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

import java.time.Instant;

/**
 * Metric that represents all values queried from influx database.
 */
public interface InfluxMetric {

    /**
     * Returns one minute rate of the given metric.
     *
     * @return one minute rate of the given metric
     */
    double oneMinRate();


    /**
     * Returns collected timestamp of the given metric.
     *
     * @return collected timestamp of the given metric
     */
    Instant time();

    /**
     * A builder of InfluxMetric.
     */
    interface Builder {

        /**
         * Sets one minute rate.
         *
         * @param rate one minute rate
         * @return builder object
         */
        Builder oneMinRate(double rate);

        /**
         * Sets collected timestamp.
         *
         * @param time timestamp
         * @return builder object
         */
        Builder time(String time);

        /**
         * Builds a influx metric instance.
         *
         * @return influx metric instance
         */
        InfluxMetric build();
    }
}
