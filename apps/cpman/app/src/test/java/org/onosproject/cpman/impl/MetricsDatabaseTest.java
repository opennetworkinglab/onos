/*
 * Copyright 2016 Open Networking Laboratory
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
package org.onosproject.cpman.impl;

import org.junit.Before;
import org.junit.Test;
import org.onosproject.cpman.MetricsDatabase;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

/**
 * Unit test for control plane metrics back-end database.
 */
public class MetricsDatabaseTest {

    private MetricsDatabase mdb;
    private static final String CPU_METRIC = "cpu";
    private static final String CPU_LOAD = "load";
    private static final String MEMORY_METRIC = "memory";
    private static final String MEMORY_FREE_PERC = "freePerc";
    private static final String MEMORY_USED_PERC = "usedPerc";

    /**
     * Initializes metrics database instance.
     */
    @Before
    public void setUp() {
        mdb = new DefaultMetricsDatabase.Builder()
                .withMetricName(CPU_METRIC)
                .addMetricType(CPU_LOAD)
                .build();
    }

    /**
     * Tests the metric update function.
     */
    @Test
    public void testMetricUpdate() {
        long currentTime = System.currentTimeMillis() / 1000L;

        mdb.updateMetric(CPU_LOAD, 30, currentTime);
        assertThat(30D, is(mdb.recentMetric(CPU_LOAD)));

        mdb.updateMetric(CPU_LOAD, 40, currentTime + 60);
        assertThat(40D, is(mdb.recentMetric(CPU_LOAD)));

        mdb.updateMetric(CPU_LOAD, 50, currentTime + 120);
        assertThat(50D, is(mdb.recentMetric(CPU_LOAD)));
    }

    /**
     * Tests the metric range fetch function.
     */
    @Test
    public void testMetricRangeFetch() {
        // full range fetch
        assertThat(mdb.metrics(CPU_LOAD).length, is(60 * 24));

        // query one minute time range
        assertThat(mdb.recentMetrics(CPU_LOAD, 1, TimeUnit.MINUTES).length, is(1));

        // query one hour time range
        assertThat(mdb.recentMetrics(CPU_LOAD, 1, TimeUnit.HOURS).length, is(60));

        // query one day time range
        assertThat(mdb.recentMetrics(CPU_LOAD, 1, TimeUnit.DAYS).length, is(60 * 24));

        // query a specific time range
        long endTime = System.currentTimeMillis() / 1000L;
        long startTime = endTime - 60 * 5;
        assertThat(mdb.metrics(CPU_LOAD, startTime, endTime).length, is(5));
    }

    /**
     * Test the projected time range.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testExceededTimeRange() {
        // query 25 hours time range
        assertThat(mdb.recentMetrics(CPU_LOAD, 25, TimeUnit.HOURS).length, is(60 * 24));
    }

    /**
     * Test the projected time range.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testInsufficientTimeRange() {
        // query 50 seconds time range
        assertThat(mdb.recentMetrics(CPU_LOAD, 50, TimeUnit.SECONDS).length, is(1));
    }

    /**
     * Test multiple metrics update and query.
     */
    @Test
    public void testMultipleMetrics() {
        MetricsDatabase multiMdb = new DefaultMetricsDatabase.Builder()
                        .withMetricName(MEMORY_METRIC)
                        .addMetricType(MEMORY_FREE_PERC)
                        .addMetricType(MEMORY_USED_PERC)
                        .build();

        Map<String, Double> metrics = new HashMap<>();
        metrics.putIfAbsent(MEMORY_FREE_PERC, 30D);
        metrics.putIfAbsent(MEMORY_USED_PERC, 70D);
        multiMdb.updateMetrics(metrics);

        assertThat(30D, is(multiMdb.recentMetric(MEMORY_FREE_PERC)));
        assertThat(70D, is(multiMdb.recentMetric(MEMORY_USED_PERC)));
    }
}
