/*
 * Copyright 2016-present Open Networking Laboratory
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
package org.onosproject.vtnweb.resources;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.ws.rs.client.WebTarget;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onlab.osgi.ServiceDirectory;
import org.onlab.osgi.TestServiceDirectory;
import org.onlab.rest.BaseResource;
import org.onosproject.codec.CodecService;
import org.onosproject.vtnrsc.PortPairId;
import org.onosproject.vtnrsc.ServiceFunctionGroup;
import org.onosproject.vtnrsc.portchainsfmap.PortChainSfMapService;
import org.onosproject.vtnweb.web.SfcCodecContext;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import com.google.common.collect.Lists;

/**
 * Unit tests for port chain sf map REST APIs.
 */
public class PortChainSfMapResourceTest extends VtnResourceTest {

    final PortChainSfMapService portChainSfMapService = createMock(PortChainSfMapService.class);

    String name1 = "Firewall";
    String description1 = "Firewall service function";
    Map<PortPairId, Integer> portPairLoadMap1 = new ConcurrentHashMap<>();

    ServiceFunctionGroup serviceFunction1 = new ServiceFunctionGroup(name1, description1,
                                                                     portPairLoadMap1);

    /**
     * Sets up the global values for all the tests.
     */
    @Before
    public void setUpTest() {
        SfcCodecContext context = new SfcCodecContext();
        ServiceDirectory testDirectory = new TestServiceDirectory()
                .add(PortChainSfMapService.class, portChainSfMapService)
                .add(CodecService.class, context.codecManager());
        BaseResource.setServiceDirectory(testDirectory);
    }

    /**
     * Cleans up.
     */
    @After
    public void tearDownTest() {
    }

    /**
     * Tests the result of a rest api GET for port chain id.
     */
    @Test
    public void testGetPortChainId() {

        final List<ServiceFunctionGroup> serviceFunctions = Lists.newArrayList();
        serviceFunctions.add(serviceFunction1);

        expect(portChainSfMapService.getServiceFunctions(anyObject())).andReturn(serviceFunctions).anyTimes();
        replay(portChainSfMapService);

        final WebTarget wt = target();
        final String response = wt.path("portChainSfMap/1278dcd4-459f-62ed-754b-87fc5e4a6751").request()
                .get(String.class);
        final JsonObject result = Json.parse(response).asObject();
        assertThat(result, notNullValue());
        assertThat(result.names().get(0), is("portChainSfMap"));
    }
}
