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

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.junit.Before;
import org.junit.Test;
import org.onlab.osgi.ServiceDirectory;
import org.onlab.osgi.TestServiceDirectory;
import org.onosproject.codec.CodecService;
import org.onosproject.codec.impl.CodecManager;
import org.onosproject.net.Link;
import org.onosproject.net.link.LinkService;
import org.onosproject.net.statistic.DefaultLoad;
import org.onosproject.net.statistic.StatisticService;

import javax.ws.rs.client.WebTarget;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.stream.IntStream;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.onosproject.net.NetTestTools.connectPoint;
import static org.onosproject.net.NetTestTools.link;

/**
 * Unit tests for statistics REST APIs.
 */
public class StatisticsResourceTest extends ResourceTest {

    Link link1 = link("src1", 1, "dst1", 1);
    Link link2 = link("src2", 2, "dst2", 2);
    Link link3 = link("src3", 3, "dst3", 3);

    LinkService mockLinkService;
    StatisticService mockStatisticService;

    /**
     * Initializes test mocks and environment.
     */
    @Before
    public void setUpTest() {
        mockLinkService = createMock(LinkService.class);
        expect(mockLinkService.getLinks())
                .andReturn(ImmutableList.of(link1, link2, link3));
        expect(mockLinkService.getLinks(connectPoint("0000000000000001", 2)))
                .andReturn(ImmutableSet.of(link3));

        mockStatisticService = createMock(StatisticService.class);
        expect(mockStatisticService.load(link1))
                .andReturn(new DefaultLoad(2, 1, 1));
        expect(mockStatisticService.load(link2))
                .andReturn(new DefaultLoad(22, 11, 1));
        expect(mockStatisticService.load(link3))
                .andReturn(new DefaultLoad(222, 111, 1));

        replay(mockLinkService, mockStatisticService);

        // Register the services needed for the test
        CodecManager codecService = new CodecManager();
        codecService.activate();
        ServiceDirectory testDirectory =
                new TestServiceDirectory()
                        .add(LinkService.class, mockLinkService)
                        .add(StatisticService.class, mockStatisticService)
                        .add(CodecService.class, codecService);

        setServiceDirectory(testDirectory);
    }

    /**
     * Checks that the values in a JSON representation of a Load are
     * correct.
     *
     * @param load JSON for the Loan object
     * @param rate expected vale fo rate
     * @param latest expected value for latest
     * @param valid expected value for valid flag
     * @param device expected device ID
     */
    private void checkValues(JsonObject load, int rate, int latest,
                             boolean valid, String device) throws UnsupportedEncodingException {
        assertThat(load, notNullValue());
        assertThat(load.get("rate").asInt(), is(rate));
        assertThat(load.get("latest").asInt(), is(latest));
        assertThat(load.get("valid").asBoolean(), is(valid));
        assertThat(load.get("time").asLong(),
                lessThanOrEqualTo((System.currentTimeMillis())));
        assertThat(URLDecoder.decode(load.get("link").asString(), "UTF-8"),
                containsString("device=of:" + device));
    }

    /**
     * Tests GET of a single Load statistics object.
     */
    @Test
    public void testSingleLoadGet() throws UnsupportedEncodingException {
        final WebTarget wt = target();
        final String response = wt.path("statistics/flows/link")
                .queryParam("device", "of:0000000000000001")
                .queryParam("port", "2")
                .request()
                .get(String.class);

        final JsonObject result = Json.parse(response).asObject();
        assertThat(result, notNullValue());

        assertThat(result.names(), hasSize(1));
        assertThat(result.names().get(0), is("loads"));

        final JsonArray jsonLoads = result.get("loads").asArray();
        assertThat(jsonLoads, notNullValue());
        assertThat(jsonLoads.size(), is(1));

        JsonObject load1 = jsonLoads.get(0).asObject();
        checkValues(load1, 111, 222, true, "src3");
    }

    /**
     * Tests GET of all Load statistics objects.
     */
    @Test
    public void testLoadsGet() throws UnsupportedEncodingException {
        final WebTarget wt = target();
        final String response = wt.path("statistics/flows/link/").request().get(String.class);

        final JsonObject result = Json.parse(response).asObject();
        assertThat(result, notNullValue());

        assertThat(result.names(), hasSize(1));
        assertThat(result.names().get(0), is("loads"));

        final JsonArray jsonLoads = result.get("loads").asArray();
        assertThat(jsonLoads, notNullValue());
        assertThat(jsonLoads.size(), is(3));

        // Hash the loads by the current field to allow easy lookup if the
        // order changes.
        HashMap<Integer, JsonObject> currentMap = new HashMap<>();
        IntStream.range(0, jsonLoads.size())
                .forEach(index -> currentMap.put(
                        jsonLoads.get(index).asObject().get("latest").asInt(),
                        jsonLoads.get(index).asObject()));

        JsonObject load1 = currentMap.get(2);
        checkValues(load1, 1, 2, true, "src1");

        JsonObject load2 = currentMap.get(22);
        checkValues(load2, 11, 22, true, "src2");

        JsonObject load3 = currentMap.get(222);
        checkValues(load3, 111, 222, true, "src3");

    }
}
