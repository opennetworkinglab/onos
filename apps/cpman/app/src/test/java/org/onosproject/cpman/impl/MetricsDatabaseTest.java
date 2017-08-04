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
package org.onosproject.cpman.impl;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.onosproject.cpman.ControlMetricType;
import org.onosproject.cpman.ControlResource;
import org.onosproject.cpman.MetricsDatabase;
import org.onosproject.net.DeviceId;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
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
    private static final String DEFAULT_RES = "resource";
    private static final String MEMORY_METRIC = "memory";
    private static final String MEMORY_FREE_PERC = "freePerc";
    private static final String MEMORY_USED_PERC = "usedPerc";
    private Map<DeviceId, MetricsDatabase> devMetricsMap;

    /**
     * Initializes metrics database instance.
     */
    @Before
    public void setUp() {
        mdb = new DefaultMetricsDatabase.Builder()
                .withMetricName(CPU_METRIC)
                .withResourceName(DEFAULT_RES)
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
    @Ignore("FIXME: in some cases it returns incorrect range result, known as RRD4J bug")
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
                        .withResourceName(DEFAULT_RES)
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

    /**
     * Tests device metrics map update and query.
     */
    @Test
    public void testDeviceMetricsMap() {
        ControlResource.Type type = ControlResource.Type.CONTROL_MESSAGE;
        DeviceId devId1 = DeviceId.deviceId("of:0000000000000101");
        DeviceId devId2 = DeviceId.deviceId("of:0000000000000102");

        devMetricsMap = Maps.newHashMap();

        Set<DeviceId> devices = ImmutableSet.of(devId1, devId2);
        devices.forEach(dev -> {
            if (!devMetricsMap.containsKey(dev)) {
                devMetricsMap.put(dev, genMDbBuilder(type, ControlResource.CONTROL_MESSAGE_METRICS)
                        .withResourceName(dev.toString())
                        .build());
            }
        });

        Map<String, Double> metrics1 = new HashMap<>();
        ControlResource.CONTROL_MESSAGE_METRICS.forEach(msgType ->
                metrics1.putIfAbsent(msgType.toString(), 10D));

        Map<String, Double> metrics2 = new HashMap<>();
        ControlResource.CONTROL_MESSAGE_METRICS.forEach(msgType ->
                metrics2.putIfAbsent(msgType.toString(), 20D));


        devMetricsMap.get(devId1).updateMetrics(metrics1);
        devMetricsMap.get(devId2).updateMetrics(metrics2);

        ControlResource.CONTROL_MESSAGE_METRICS.forEach(msgType ->
                assertThat(10D, is(devMetricsMap.get(devId1).recentMetric(msgType.toString())))
        );

        ControlResource.CONTROL_MESSAGE_METRICS.forEach(msgType ->
                assertThat(20D, is(devMetricsMap.get(devId2).recentMetric(msgType.toString())))
        );
    }

    private MetricsDatabase.Builder genMDbBuilder(ControlResource.Type resourceType,
                                          Set<ControlMetricType> metricTypes) {
        MetricsDatabase.Builder builder = new DefaultMetricsDatabase.Builder();
        builder.withMetricName(resourceType.toString());
        metricTypes.forEach(type -> builder.addMetricType(type.toString()));
        return builder;
    }
}
