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
package org.onosproject.codec.impl;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Meter;
import com.codahale.metrics.Metric;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Timer;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onosproject.codec.CodecContext;
import org.onosproject.codec.JsonCodec;

import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Codec for the Metric class.
 */
public class MetricCodec extends JsonCodec<Metric> {

    // JSON field names
    private static final String COUNTER = "counter";

    private static final String GAUGE = "gauge";
    private static final String VALUE = "value";

    private static final String METER = "meter";
    private static final String MEAN_RATE = "mean_rate";
    private static final String ONE_MIN_RATE = "1_min_rate";
    private static final String FIVE_MIN_RATE = "5_min_rate";
    private static final String FIFT_MIN_RATE = "15_min_rate";

    private static final String HISTOGRAM = "histogram";
    private static final String MIN = "min";
    private static final String MAX = "max";
    private static final String MEAN = "mean";
    private static final String STDDEV = "stddev";

    private static final String TIMER = "timer";

    @Override
    public ObjectNode encode(Metric metric, CodecContext context) {
        checkNotNull(metric, "Metric cannot be null");

        ObjectNode objectNode = context.mapper().createObjectNode();
        ObjectNode dataNode = context.mapper().createObjectNode();

        if (metric instanceof Counter) {
            dataNode.put(COUNTER, ((Counter) metric).getCount());
            objectNode.set(COUNTER, dataNode);
        } else if (metric instanceof Gauge) {
            objectNode.put(VALUE, ((Gauge) metric).getValue().toString());
            objectNode.set(GAUGE, dataNode);
        } else if (metric instanceof Meter) {
            dataNode.put(COUNTER, ((Meter) metric).getCount());
            dataNode.put(MEAN_RATE, ((Meter) metric).getMeanRate());
            dataNode.put(ONE_MIN_RATE, ((Meter) metric).getOneMinuteRate());
            dataNode.put(FIVE_MIN_RATE, ((Meter) metric).getFiveMinuteRate());
            dataNode.put(FIFT_MIN_RATE, ((Meter) metric).getFifteenMinuteRate());
            objectNode.set(METER, dataNode);
        } else if (metric instanceof Histogram) {
            dataNode.put(COUNTER, ((Histogram) metric).getCount());
            dataNode.put(MEAN, ((Histogram) metric).getSnapshot().getMean());
            dataNode.put(MIN, ((Histogram) metric).getSnapshot().getMin());
            dataNode.put(MAX, ((Histogram) metric).getSnapshot().getMax());
            dataNode.put(STDDEV, ((Histogram) metric).getSnapshot().getStdDev());
            objectNode.set(HISTOGRAM, dataNode);
        } else if (metric instanceof Timer) {
            dataNode.put(COUNTER, ((Timer) metric).getCount());
            dataNode.put(MEAN_RATE, ((Timer) metric).getMeanRate());
            dataNode.put(ONE_MIN_RATE, ((Timer) metric).getOneMinuteRate());
            dataNode.put(FIVE_MIN_RATE, ((Timer) metric).getFiveMinuteRate());
            dataNode.put(FIFT_MIN_RATE, ((Timer) metric).getFifteenMinuteRate());
            dataNode.put(MEAN, nanoToMs(((Timer) metric).getSnapshot().getMean()));
            dataNode.put(MIN, nanoToMs(((Timer) metric).getSnapshot().getMin()));
            dataNode.put(MAX, nanoToMs(((Timer) metric).getSnapshot().getMax()));
            dataNode.put(STDDEV, nanoToMs(((Timer) metric).getSnapshot().getStdDev()));
            objectNode.set(TIMER, dataNode);
        }
        return objectNode;
    }

    private double nanoToMs(double nano) {
        return TimeUnit.MILLISECONDS.convert((long) nano, TimeUnit.NANOSECONDS);
    }
}
