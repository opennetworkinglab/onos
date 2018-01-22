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
import com.eclipsesource.json.JsonObject;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onlab.osgi.ServiceDirectory;
import org.onlab.osgi.TestServiceDirectory;
import org.onosproject.codec.CodecService;
import org.onosproject.codec.impl.CodecManager;
import org.onosproject.core.CoreService;
import org.onosproject.net.NetTestTools;
import org.onosproject.net.flowobjective.FlowObjectiveService;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.InputStream;
import java.net.HttpURLConnection;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.anyShort;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.onosproject.net.NetTestTools.APP_ID;

/**
 * Unit tests for flow objectives REST APIs.
 */
public class FlowObjectiveResourceTest extends ResourceTest {
    final FlowObjectiveService mockFlowObjectiveService = createMock(FlowObjectiveService.class);
    CoreService mockCoreService = createMock(CoreService.class);
    public static final String REST_APP_ID = "org.onosproject.rest";

    /**
     * Sets up the global values for all the tests.
     */
    @Before
    public void setUpTest() {
        // Mock Core Service
        expect(mockCoreService.getAppId(anyShort()))
                .andReturn(NetTestTools.APP_ID).anyTimes();
        expect(mockCoreService.registerApplication(REST_APP_ID))
                .andReturn(APP_ID).anyTimes();
        replay(mockCoreService);

        // Register the services needed for the test
        final CodecManager codecService = new CodecManager();
        codecService.activate();
        ServiceDirectory testDirectory =
                new TestServiceDirectory()
                        .add(FlowObjectiveService.class, mockFlowObjectiveService)
                        .add(CodecService.class, codecService)
                        .add(CoreService.class, mockCoreService);

        setServiceDirectory(testDirectory);
    }

    /**
     * Cleans up and verifies the mocks.
     */
    @After
    public void tearDownTest() {
        verify(mockFlowObjectiveService);
        verify(mockCoreService);
    }

    /**
     * Tests creating a filtering objective with POST.
     */
    @Test
    public void testFilteringObjectivePost() {
        mockFlowObjectiveService.filter(anyObject(), anyObject());
        prepareService();
        testObjectiveCreation("post-filter-objective.json", "of:0000000000000001", "filter");
    }

    /**
     * Tests creating a forwarding objective with POST.
     */
    @Test
    public void testForwardingObjectivePost() {
        mockFlowObjectiveService.forward(anyObject(), anyObject());
        prepareService();
        testObjectiveCreation("post-forward-objective.json", "of:0000000000000001", "forward");
    }

    /**
     * Tests creating a next objective with POST.
     */
    @Test
    public void testNextObjectivePost() {
        mockFlowObjectiveService.next(anyObject(), anyObject());
        prepareService();
        testObjectiveCreation("post-next-objective.json", "of:0000000000000001", "next");
    }

    /**
     * Tests obtaining a global unique nextId with GET.
     */
    @Test
    public void testNextId() {
        expect(mockFlowObjectiveService.allocateNextId()).andReturn(10).anyTimes();
        prepareService();

        WebTarget wt = target();
        final String response = wt.path("flowobjectives/next").request().get(String.class);
        final JsonObject result = Json.parse(response).asObject();
        assertThat(result, notNullValue());

        assertThat(result.names(), hasSize(1));
        assertThat(result.names().get(0), is("nextId"));
        final int jsonNextId = result.get("nextId").asInt();
        assertThat(jsonNextId, is(10));
    }

    private void prepareService() {
        expectLastCall();
        replay(mockFlowObjectiveService);
    }

    /**
     * A base class for testing various objective creation.
     *
     * @param jsonFile json file path
     * @param deviceId device id in string format
     * @param method objective method
     */
    private void testObjectiveCreation(String jsonFile, String deviceId, String method) {
        WebTarget wt = target();
        InputStream jsonStream = FlowsResourceTest.class
                .getResourceAsStream(jsonFile);

        StringBuilder sb = new StringBuilder();
        sb.append("flowobjectives");
        sb.append("/");
        sb.append(deviceId);
        sb.append("/");
        sb.append(method);

        Response response = wt.path(sb.toString())
                .request(MediaType.APPLICATION_JSON_TYPE)
                .post(Entity.json(jsonStream));
        assertThat(response.getStatus(), is(HttpURLConnection.HTTP_CREATED));
        String location = response.getLocation().getPath();
        assertThat(location, Matchers.startsWith("/" + sb.toString()));
    }
}
