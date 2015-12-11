/*
 * Copyright 2014-2015 Open Networking Laboratory
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
package org.onosproject.rest;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Meter;
import com.codahale.metrics.Metric;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.google.common.collect.ImmutableMap;
import com.sun.jersey.api.client.WebResource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onlab.metrics.MetricsService;
import org.onlab.osgi.ServiceDirectory;
import org.onlab.osgi.TestServiceDirectory;
import org.onlab.rest.BaseResource;
import org.onosproject.codec.CodecService;
import org.onosproject.codec.impl.CodecManager;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

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
        BaseResource.setServiceDirectory(testDirectory);
    }

    /**
     * Verifies mocks.
     */
    @After
    public void tearDownTest() {
        verify(mockMetricsService);
    }

    /**
     * Tests that a fetch of a non-existent object throws an exception.
     */
    @Test
    public void testBadGet() {
        Counter onosCounter = new Counter();
        onosCounter.inc();

        Meter onosMeter = new Meter();
        onosMeter.mark();

        ImmutableMap<String, Metric> metrics =
                new ImmutableMap.Builder<String, Metric>()
                        .put("onosCounter", onosCounter)
                        .put("onosMeter", onosMeter)
                        .build();

        expect(mockMetricsService.getMetrics())
                .andReturn(metrics)
                .anyTimes();

        replay(mockMetricsService);

        WebResource rs = resource();
        String response = rs.path("metrics").get(String.class);
        assertThat(response, containsString("{\"metrics\":["));

        JsonObject result = JsonObject.readFrom(response);
        assertThat(result, notNullValue());

        JsonArray jsonMetrics = result.get("metrics").asArray();
        assertThat(jsonMetrics, notNullValue());
        assertThat(jsonMetrics.size(), is(2));
    }
}
