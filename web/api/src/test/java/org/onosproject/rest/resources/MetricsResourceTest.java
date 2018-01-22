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
package org.onosproject.rest.resources;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Meter;
import com.codahale.metrics.Metric;
import com.codahale.metrics.Timer;
import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.google.common.collect.ImmutableMap;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onlab.metrics.MetricsService;
import org.onlab.osgi.ServiceDirectory;
import org.onlab.osgi.TestServiceDirectory;
import org.onosproject.codec.CodecService;
import org.onosproject.codec.impl.CodecManager;

import javax.ws.rs.client.WebTarget;
import java.util.concurrent.TimeUnit;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for Metrics REST APIs.
 */
public class MetricsResourceTest extends ResourceTest {
    MetricsService mockMetricsService;

    /**
     * Initializes test mocks and environment.
     */
    @Before
    public void setUpTest() {
        mockMetricsService = createMock(MetricsService.class);

        // Register the services needed for the test
        final CodecManager codecService = new CodecManager();
        codecService.activate();
        ServiceDirectory testDirectory =
                new TestServiceDirectory()
                        .add(MetricsService.class, mockMetricsService)
                        .add(CodecService.class, codecService);
        setServiceDirectory(testDirectory);
    }

    /**
     * Verifies mocks.
     */
    @After
    public void tearDownTest() {
        verify(mockMetricsService);
    }

    /**
     * Tests GetAllMetrics method.
     */
    @Test
    public void testGetAllMetrics() {
        Counter onosCounter = new Counter();
        onosCounter.inc();

        Meter onosMeter = new Meter();
        onosMeter.mark();

        Timer onosTimer = new Timer();
        onosTimer.update(1, TimeUnit.MILLISECONDS);

        ImmutableMap<String, Metric> metrics =
                new ImmutableMap.Builder<String, Metric>()
                        .put("onosCounter", onosCounter)
                        .put("onosMeter", onosMeter)
                        .put("onosTimer", onosTimer)
                        .build();

        expect(mockMetricsService.getMetrics())
                .andReturn(metrics)
                .anyTimes();

        replay(mockMetricsService);

        WebTarget wt = target();
        String response = wt.path("metrics").request().get(String.class);
        assertThat(response, containsString("{\"metrics\":["));

        JsonObject result = Json.parse(response).asObject();
        assertThat(result, notNullValue());

        JsonArray jsonMetrics = result.get("metrics").asArray();
        assertThat(jsonMetrics, notNullValue());
        assertThat(jsonMetrics.size(), is(3));

        assertTrue(matchesMetric(metrics.get("onosCounter")).matchesSafely(jsonMetrics.get(0).asObject()));
        assertTrue(matchesMetric(metrics.get("onosMeter")).matchesSafely(jsonMetrics.get(1).asObject()));
        assertTrue(matchesMetric(metrics.get("onosTimer")).matchesSafely(jsonMetrics.get(2).asObject()));
    }

    /**
     * Hamcrest matcher to check that a metric representation in JSON matches
     * the actual metric.
     */
    public static class MetricJsonMatcher extends TypeSafeMatcher<JsonObject> {
        private final Metric metric;
        private String reason = "";

        public MetricJsonMatcher(Metric metricValue) {
            this.metric = metricValue;
        }

        @Override
        public boolean matchesSafely(JsonObject jsonObject) {

            JsonObject jsonMetric = jsonObject.get("metric").asObject();
            JsonObject jsonCounter;
            JsonObject jsonMeter;
            JsonObject jsonTimer;
            Counter counter;
            Meter meter;
            Timer timer;

            // check counter metric
            if (jsonMetric.get("counter") != null) {
                jsonCounter = jsonMetric.get("counter").asObject();
                counter = (Counter) metric;
                if (jsonCounter.get("counter").asLong() != counter.getCount()) {
                    reason = "counter " + counter.getCount();
                    return false;
                }
            }

            // check meter metric
            if (jsonMetric.get("meter") != null) {
                jsonMeter = jsonMetric.get("meter").asObject();
                meter = (Meter) metric;

                if (jsonMeter.get("counter").asLong() != meter.getCount()) {
                    reason = "counter " + meter.getCount();
                    return false;
                }

                if (jsonMeter.get("1_min_rate").asDouble() != meter.getOneMinuteRate()) {
                    reason = "1 minute rate " + meter.getOneMinuteRate();
                    return false;
                }

                if (jsonMeter.get("5_min_rate").asDouble() != meter.getOneMinuteRate()) {
                    reason = "5 minute rate " + meter.getFiveMinuteRate();
                    return false;
                }

                if (jsonMeter.get("15_min_rate").asDouble() != meter.getFifteenMinuteRate()) {
                    reason = "15 minute rate " + meter.getFifteenMinuteRate();
                    return false;
                }
            }

            if (jsonMetric.get("timer") != null) {
                jsonTimer = jsonMetric.get("timer").asObject();
                timer = (Timer) metric;

                if (jsonTimer.get("counter").asLong() != timer.getCount()) {
                    reason = "counter " + timer.getCount();
                    return false;
                }

                if (jsonTimer.get("1_min_rate").asDouble() != timer.getOneMinuteRate()) {
                    reason = "1 minute rate " + timer.getOneMinuteRate();
                    return false;
                }

                if (jsonTimer.get("5_min_rate").asDouble() != timer.getOneMinuteRate()) {
                    reason = "5 minute rate " + timer.getFiveMinuteRate();
                    return false;
                }

                if (jsonTimer.get("15_min_rate").asDouble() != timer.getFifteenMinuteRate()) {
                    reason = "15 minute rate " + timer.getFifteenMinuteRate();
                    return false;
                }

                if (jsonTimer.get("mean").asDouble() != nanoToMs(timer.getSnapshot().getMean())) {
                    reason = "mean " + timer.getSnapshot().getMean();
                    return false;
                }

                if (jsonTimer.get("min").asDouble() != nanoToMs(timer.getSnapshot().getMin())) {
                    reason = "min " + timer.getSnapshot().getMin();
                    return false;
                }

                if (jsonTimer.get("max").asDouble() != nanoToMs(timer.getSnapshot().getMax())) {
                    reason = "max " + timer.getSnapshot().getMax();
                    return false;
                }

                if (jsonTimer.get("stddev").asDouble() != nanoToMs(timer.getSnapshot().getStdDev())) {
                    reason = "stddev " + timer.getSnapshot().getStdDev();
                    return false;
                }
            }

            return true;
        }

        @Override
        public void describeTo(Description description) {
            description.appendText(reason);
        }

        private double nanoToMs(double nano) {
            return nano / 1_000_000D;
        }
    }

    /**
     * Factory to allocate an metric matcher.
     *
     * @param metric metric object we are looking for
     * @return matcher
     */
    private static MetricJsonMatcher matchesMetric(Metric metric) {
        return new MetricJsonMatcher(metric);
    }
}
