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
package org.onosproject.codec.impl;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Meter;
import com.codahale.metrics.Metric;
import com.codahale.metrics.Timer;
import com.fasterxml.jackson.databind.JsonNode;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeDiagnosingMatcher;

import java.util.concurrent.TimeUnit;

/**
 * Hamcrest matcher for metrics.
 */
public final class MetricJsonMatcher extends TypeSafeDiagnosingMatcher<JsonNode> {

    private final Metric metric;

    private MetricJsonMatcher(Metric metric) {
        this.metric = metric;
    }

    @Override
    protected boolean matchesSafely(JsonNode jsonMetric, Description description) {

        // check counter
        if (metric instanceof Counter) {
            Counter counter = (Counter) metric;
            long jsonCounter = jsonMetric.get("counter").asLong();
            long counterVal = counter.getCount();
            if (jsonCounter != counterVal) {
                description.appendText("counter was " + jsonCounter);
                return false;
            }
        }

        // check meter
        if (metric instanceof Meter) {
            Meter meter = (Meter) metric;

            long jsonCounter = jsonMetric.get("counter").asLong();
            long counterVal = meter.getCount();
            if (jsonCounter != counterVal) {
                description.appendText("counter was " + jsonCounter);
                return false;
            }

            double jsonOneMinRate = jsonMetric.get("1_min_rate").asDouble();
            double oneMinRate = meter.getOneMinuteRate();
            if (jsonOneMinRate != oneMinRate) {
                description.appendText("one minute rate was " + jsonOneMinRate);
                return false;
            }

            double jsonFiveMinRate = jsonMetric.get("5_min_rate").asDouble();
            double fiveMinRate = meter.getFiveMinuteRate();
            if (jsonFiveMinRate != fiveMinRate) {
                description.appendText("five minute rate was " + jsonFiveMinRate);
                return false;
            }

            double jsonFiftMinRate = jsonMetric.get("15_min_rate").asDouble();
            double fiftMinRate = meter.getFifteenMinuteRate();
            if (jsonFiftMinRate != fiftMinRate) {
                description.appendText("fifteen minute rate was " + jsonFiftMinRate);
                return false;
            }
        }

        // check timer
        if (metric instanceof Timer) {
            Timer timer = (Timer) metric;

            long jsonCounter = jsonMetric.get("counter").asLong();
            long counterVal = timer.getCount();
            if (jsonCounter != counterVal) {
                description.appendText("counter was " + jsonCounter);
                return false;
            }

            double jsonOneMinRate = jsonMetric.get("1_min_rate").asDouble();
            double oneMinRate = timer.getOneMinuteRate();
            if (jsonOneMinRate != oneMinRate) {
                description.appendText("one minute rate was " + jsonOneMinRate);
                return false;
            }

            double jsonFiveMinRate = jsonMetric.get("5_min_rate").asDouble();
            double fiveMinRate = timer.getFiveMinuteRate();
            if (jsonFiveMinRate != fiveMinRate) {
                description.appendText("five minute rate was " + jsonFiveMinRate);
                return false;
            }

            double jsonFiftMinRate = jsonMetric.get("15_min_rate").asDouble();
            double fiftMinRate = timer.getFifteenMinuteRate();
            if (jsonFiftMinRate != fiftMinRate) {
                description.appendText("fifteen minute rate was " + jsonFiftMinRate);
                return false;
            }

            double jsonMean = jsonMetric.get("mean").asDouble();
            double mean = nanoToMs(timer.getSnapshot().getMean());
            if (jsonMean != mean) {
                description.appendText("mean was " + jsonMean);
                return false;
            }

            double jsonMin = jsonMetric.get("min").asDouble();
            double min = nanoToMs(timer.getSnapshot().getMin());
            if (jsonMin != min) {
                description.appendText("json min was " + jsonMin);
                return false;
            }

            double jsonMax = jsonMetric.get("max").asDouble();
            double max = nanoToMs(timer.getSnapshot().getMax());
            if (jsonMax != max) {
                description.appendText("max was " + jsonMax);
                return false;
            }

            double jsonStdDev = jsonMetric.get("stddev").asDouble();
            double stdDev = nanoToMs(timer.getSnapshot().getStdDev());
            if (jsonStdDev != stdDev) {
                description.appendText("stddev was " + jsonStdDev);
                return false;
            }
        }

        return true;
    }

    @Override
    public void describeTo(Description description) {
        description.appendText(metric.toString());
    }

    /**
     * Factory to allocate a metric matcher.
     *
     * @param metric metric object we are looking for
     * @return matcher
     */
    public static MetricJsonMatcher matchesMetric(Metric metric) {
        return new MetricJsonMatcher(metric);
    }

    private double nanoToMs(double nano) {
        return TimeUnit.MILLISECONDS.convert((long) nano, TimeUnit.NANOSECONDS);
    }
}