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
package org.onosproject.vtnweb.resources;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import javax.ws.rs.core.MediaType;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onlab.osgi.ServiceDirectory;
import org.onlab.osgi.TestServiceDirectory;
import org.onlab.packet.IpPrefix;
import org.onlab.rest.BaseResource;
import org.onosproject.codec.CodecService;
import org.onosproject.vtnrsc.FlowClassifier;
import org.onosproject.vtnrsc.FlowClassifierId;
import org.onosproject.vtnrsc.TenantId;
import org.onosproject.vtnrsc.VirtualPortId;
import org.onosproject.vtnrsc.flowclassifier.FlowClassifierService;
import org.onosproject.vtnweb.web.SfcCodecContext;

import com.eclipsesource.json.JsonObject;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.client.WebResource;
/**
 * Unit tests for flow classifier REST APIs.
 */
public class FlowClassifierResourceTest extends VtnResourceTest {

    final FlowClassifierService flowClassifierService = createMock(FlowClassifierService.class);

    FlowClassifierId flowClassifierId1 = FlowClassifierId.of("4a334cd4-fe9c-4fae-af4b-321c5e2eb051");
    TenantId tenantId1 = TenantId.tenantId("1814726e2d22407b8ca76db5e567dcf1");
    VirtualPortId srcPortId1 = VirtualPortId.portId("dace4513-24fc-4fae-af4b-321c5e2eb3d1");
    VirtualPortId dstPortId1 = VirtualPortId.portId("aef3478a-4a56-2a6e-cd3a-9dee4e2ec345");

    final MockFlowClassifier flowClassifier1 = new MockFlowClassifier(flowClassifierId1, tenantId1, "flowClassifier1",
                                                                      "Mock flow classifier", "IPv4", "IP", 1001, 1500,
                                                                      5001, 6000, IpPrefix.valueOf("1.1.1.1/16"),
                                                                      IpPrefix.valueOf("22.12.34.45/16"),
                                                                      srcPortId1, dstPortId1);

    /**
     * Mock class for a flow classifier.
     */
    private static class MockFlowClassifier implements FlowClassifier {

        private final FlowClassifierId flowClassifierId;
        private final TenantId tenantId;
        private final String name;
        private final String description;
        private final String etherType;
        private final String protocol;
        private final int minSrcPortRange;
        private final int maxSrcPortRange;
        private final int minDstPortRange;
        private final int maxDstPortRange;
        private final IpPrefix srcIpPrefix;
        private final IpPrefix dstIpPrefix;
        private final VirtualPortId srcPort;
        private final VirtualPortId dstPort;

        public MockFlowClassifier(FlowClassifierId flowClassifierId, TenantId tenantId, String name,
                                  String description, String etherType, String protocol, int minSrcPortRange,
                                  int maxSrcPortRange, int minDstPortRange, int maxDstPortRange, IpPrefix srcIpPrefix,
                                  IpPrefix dstIpPrefix, VirtualPortId srcPort, VirtualPortId dstPort) {
            this.flowClassifierId = flowClassifierId;
            this.tenantId = tenantId;
            this.name = name;
            this.description = description;
            this.etherType = etherType;
            this.protocol = protocol;
            this.minSrcPortRange = minSrcPortRange;
            this.maxSrcPortRange = maxSrcPortRange;
            this.minDstPortRange = minDstPortRange;
            this.maxDstPortRange = maxDstPortRange;
            this.srcIpPrefix = srcIpPrefix;
            this.dstIpPrefix = dstIpPrefix;
            this.srcPort = srcPort;
            this.dstPort = dstPort;
        }


        @Override
        public FlowClassifierId flowClassifierId() {
            return flowClassifierId;
        }

        @Override
        public TenantId tenantId() {
            return tenantId;
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public String description() {
            return description;
        }

        @Override
        public String etherType() {
            return etherType;
        }

        @Override
        public String protocol() {
            return protocol;
        }

        @Override
        public int minSrcPortRange() {
            return minSrcPortRange;
        }

        @Override
        public int maxSrcPortRange() {
            return maxSrcPortRange;
        }

        @Override
        public int minDstPortRange() {
            return minDstPortRange;
        }

        @Override
        public int maxDstPortRange() {
            return maxDstPortRange;
        }

        @Override
        public IpPrefix srcIpPrefix() {
            return srcIpPrefix;
        }

        @Override
        public IpPrefix dstIpPrefix() {
            return dstIpPrefix;
        }

        @Override
        public VirtualPortId srcPort() {
            return srcPort;
        }

        @Override
        public VirtualPortId dstPort() {
            return dstPort;
        }

        @Override
        public boolean exactMatch(FlowClassifier flowClassifier) {
            return this.equals(flowClassifier) &&
                    Objects.equals(this.flowClassifierId, flowClassifier.flowClassifierId()) &&
                    Objects.equals(this.tenantId, flowClassifier.tenantId());
        }
    }

    /**
     * Sets up the global values for all the tests.
     */
    @Before
    public void setUpTest() {
        SfcCodecContext context = new SfcCodecContext();

        ServiceDirectory testDirectory = new TestServiceDirectory()
        .add(FlowClassifierService.class, flowClassifierService)
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
     * Tests the result of the rest api GET when there are no flow classifiers.
     */
    @Test
    public void testFlowClassifiersEmpty() {

        expect(flowClassifierService.getFlowClassifiers()).andReturn(null).anyTimes();
        replay(flowClassifierService);
        final WebResource rs = resource();
        final String response = rs.path("flow_classifiers").get(String.class);
        assertThat(response, is("{\"flow_classifiers\":[]}"));
    }

    /**
     * Tests the result of a rest api GET for flow classifier id.
     */
    @Test
    public void testGetFlowClassifierId() {

        final Set<FlowClassifier> flowClassifiers = new HashSet<>();
        flowClassifiers.add(flowClassifier1);

        expect(flowClassifierService.exists(anyObject())).andReturn(true).anyTimes();
        expect(flowClassifierService.getFlowClassifier(anyObject())).andReturn(flowClassifier1).anyTimes();
        replay(flowClassifierService);

        final WebResource rs = resource();
        final String response = rs.path("flow_classifiers/4a334cd4-fe9c-4fae-af4b-321c5e2eb051").get(String.class);
        final JsonObject result = JsonObject.readFrom(response);
        assertThat(result, notNullValue());
    }

    /**
     * Tests that a fetch of a non-existent flow classifier object throws an exception.
     */
    @Test
    public void testBadGet() {
        expect(flowClassifierService.getFlowClassifier(anyObject()))
        .andReturn(null).anyTimes();
        replay(flowClassifierService);
        WebResource rs = resource();
        try {
            rs.path("flow_classifiers/78dcd363-fc23-aeb6-f44b-56dc5aafb3ae").get(String.class);
            fail("Fetch of non-existent flow classifier did not throw an exception");
        } catch (UniformInterfaceException ex) {
            assertThat(ex.getMessage(),
                       containsString("returned a response status of"));
        }
    }

    /**
     * Tests creating a flow classifier with POST.
     */
    @Test
    public void testPost() {

        expect(flowClassifierService.createFlowClassifier(anyObject()))
        .andReturn(true).anyTimes();
        replay(flowClassifierService);

        WebResource rs = resource();
        InputStream jsonStream = FlowClassifierResourceTest.class.getResourceAsStream("post-FlowClassifier.json");

        ClientResponse response = rs.path("flow_classifiers")
                .type(MediaType.APPLICATION_JSON_TYPE)
                .post(ClientResponse.class, jsonStream);
        assertThat(response.getStatus(), is(HttpURLConnection.HTTP_OK));
    }

    /**
     * Tests deleting a flow classifier.
     */
    @Test
    public void testDelete() {
        expect(flowClassifierService.removeFlowClassifier(anyObject()))
        .andReturn(true).anyTimes();
        replay(flowClassifierService);

        WebResource rs = resource();

        String location = "flow_classifiers/4a334cd4-fe9c-4fae-af4b-321c5e2eb051";

        ClientResponse deleteResponse = rs.path(location)
                .type(MediaType.APPLICATION_JSON_TYPE)
                .delete(ClientResponse.class);
        assertThat(deleteResponse.getStatus(),
                   is(HttpURLConnection.HTTP_NO_CONTENT));
    }
}
