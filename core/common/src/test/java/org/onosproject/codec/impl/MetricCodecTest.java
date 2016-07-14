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
import com.codahale.metrics.Meter;
import com.codahale.metrics.Metric;
import com.codahale.metrics.Timer;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Before;
import org.junit.Test;
import org.onosproject.codec.JsonCodec;
import org.onosproject.core.CoreService;

import java.util.concurrent.TimeUnit;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.replay;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.onosproject.codec.impl.MetricJsonMatcher.matchesMetric;

/**
 * Unit tests for Metric codec.
 */
public class MetricCodecTest {

    MockCodecContext context;
    JsonCodec<Metric> metricCodec;
    final CoreService mockCoreService = createMock(CoreService.class);

    /**
     * Sets up for each test.  Creates a context and fetches the metric codec.
     */
    @Before
    public void setUp() {
        context = new MockCodecContext();
        metricCodec = context.codec(Metric.class);
        assertThat(metricCodec, notNullValue());

        replay(mockCoreService);
        context.registerService(CoreService.class, mockCoreService);
    }

    /**
     * Tests encoding of a Metric object.
     */
    @Test
    public void testMetricEncode() {
        Counter counter = new Counter();
        Meter meter = new Meter();
        Timer timer = new Timer();

        counter.inc();
        meter.mark();
        timer.update(1, TimeUnit.MILLISECONDS);

        ObjectNode counterJson = metricCodec.encode(counter, context);
        assertThat(counterJson.get("counter"), matchesMetric(counter));

        ObjectNode meterJson = metricCodec.encode(meter, context);
        assertThat(meterJson.get("meter"), matchesMetric(meter));

        ObjectNode timerJson = metricCodec.encode(timer, context);
        assertThat(timerJson.get("timer"), matchesMetric(timer));
    }
}
