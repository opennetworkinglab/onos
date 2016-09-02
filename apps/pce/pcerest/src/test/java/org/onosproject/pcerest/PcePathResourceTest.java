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
package org.onosproject.pcerest;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.onosproject.net.Link.Type.DIRECT;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.LinkedList;
import java.util.List;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.NotFoundException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onlab.osgi.ServiceDirectory;
import org.onlab.osgi.TestServiceDirectory;
import org.onlab.packet.IpAddress;
import org.onlab.rest.BaseResource;
import org.onosproject.codec.CodecService;
import org.onosproject.core.DefaultGroupId;
import org.onosproject.incubator.net.tunnel.DefaultTunnel;
import org.onosproject.incubator.net.tunnel.IpTunnelEndPoint;
import org.onosproject.incubator.net.tunnel.Tunnel;
import org.onosproject.incubator.net.tunnel.TunnelId;
import org.onosproject.incubator.net.tunnel.TunnelEndPoint;
import org.onosproject.incubator.net.tunnel.TunnelName;
import org.onosproject.incubator.net.tunnel.TunnelService;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DefaultAnnotations;
import org.onosproject.net.DefaultLink;
import org.onosproject.net.DefaultPath;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Link;
import org.onosproject.pce.pceservice.api.PceService;
import org.onosproject.pce.pceservice.PcepAnnotationKeys;
import org.onosproject.pce.pcestore.api.PceStore;
import org.onosproject.net.Path;
import org.onosproject.net.PortNumber;
import org.onosproject.net.provider.ProviderId;

/**
 * Unit tests for pce path REST APIs.
 */
public class PcePathResourceTest extends PceResourceTest {
    private final PceService pceService = createMock(PceService.class);
    private final PceStore pceStore = createMock(PceStore.class);
    private final TunnelService tunnelService = createMock(TunnelService.class);
    private final TunnelEndPoint src = IpTunnelEndPoint.ipTunnelPoint(IpAddress.valueOf(23423));
    private final TunnelEndPoint dst = IpTunnelEndPoint.ipTunnelPoint(IpAddress.valueOf(32421));
    private final DefaultGroupId groupId = new DefaultGroupId(92034);
    private final TunnelName tunnelName = TunnelName.tunnelName("TunnelName");
    private final TunnelId tunnelId = TunnelId.valueOf("41654654");
    private final ProviderId producerName = new ProviderId("producer1", "13");
    private Path path;
    private Tunnel tunnel;
    private DeviceId deviceId1;
    private DeviceId deviceId2;
    private DeviceId deviceId3;
    private DeviceId deviceId4;
    private DeviceId deviceId5;
    private PortNumber port1;
    private PortNumber port2;
    private PortNumber port3;
    private PortNumber port4;
    private PortNumber port5;

    /**
     * Sets up the global values for all the tests.
     */
    @Before
    public void setUpTest() {
       // Mock environment setup
       MockPceCodecContext context = new MockPceCodecContext();
       ServiceDirectory testDirectory = new TestServiceDirectory().add(PceService.class, pceService)
                                                                  .add(TunnelService.class, tunnelService)
                                                                  .add(PceStore.class, pceStore)
                                                                  .add(CodecService.class, context.codecManager());
       BaseResource.setServiceDirectory(testDirectory);

       // Tunnel creation
       // Links
       ProviderId providerId = new ProviderId("of", "foo");
       deviceId1 = DeviceId.deviceId("of:A");
       deviceId2 = DeviceId.deviceId("of:B");
       deviceId3 = DeviceId.deviceId("of:C");
       deviceId4 = DeviceId.deviceId("of:D");
       deviceId5 = DeviceId.deviceId("of:E");
       port1 = PortNumber.portNumber(1);
       port2 = PortNumber.portNumber(2);
       port3 = PortNumber.portNumber(3);
       port4 = PortNumber.portNumber(4);
       port5 = PortNumber.portNumber(5);
       List<Link> linkList = new LinkedList<>();

       Link l1 = DefaultLink.builder()
                            .providerId(providerId)
                            .annotations(DefaultAnnotations.builder().set("key1", "yahoo").build())
                            .src(new ConnectPoint(deviceId1, port1))
                            .dst(new ConnectPoint(deviceId2, port2))
                            .type(DIRECT)
                            .state(Link.State.ACTIVE)
                            .build();
       linkList.add(l1);
       Link l2 = DefaultLink.builder()
                            .providerId(providerId)
                            .annotations(DefaultAnnotations.builder().set("key2", "yahoo").build())
                            .src(new ConnectPoint(deviceId2, port2))
                            .dst(new ConnectPoint(deviceId3, port3))
                            .type(DIRECT)
                            .state(Link.State.ACTIVE)
                            .build();
       linkList.add(l2);
       Link l3 = DefaultLink.builder()
                            .providerId(providerId)
                            .annotations(DefaultAnnotations.builder().set("key3", "yahoo").build())
                            .src(new ConnectPoint(deviceId3, port3))
                            .dst(new ConnectPoint(deviceId4, port4))
                            .type(DIRECT)
                            .state(Link.State.ACTIVE)
                            .build();
       linkList.add(l3);
       Link l4 = DefaultLink.builder()
                            .providerId(providerId)
                            .annotations(DefaultAnnotations.builder().set("key4", "yahoo").build())
                            .src(new ConnectPoint(deviceId4, port4))
                            .dst(new ConnectPoint(deviceId5, port5))
                            .type(DIRECT)
                            .state(Link.State.ACTIVE)
                            .build();
       linkList.add(l4);

       // Path
       path = new DefaultPath(providerId, linkList, 10);

       // Annotations
       DefaultAnnotations.Builder builderAnn = DefaultAnnotations.builder();
       builderAnn.set(PcepAnnotationKeys.LSP_SIG_TYPE, "WITH_SIGNALLING");
       builderAnn.set(PcepAnnotationKeys.COST_TYPE, "COST");
       builderAnn.set(PcepAnnotationKeys.BANDWIDTH, "200");

       // Tunnel
       tunnel = new DefaultTunnel(producerName, src, dst, Tunnel.Type.VXLAN,
                                  Tunnel.State.ACTIVE, groupId, tunnelId,
                                  tunnelName, path, builderAnn.build());
    }

    /**
     * Cleans up.
     */
    @After
    public void tearDownTest() {
    }

    /**
     * Tests the result of the rest api GET when there are no pce paths.
     */
    @Test
    public void testPcePathsEmpty() {
        expect(pceService.queryAllPath())
                         .andReturn(null)
                         .anyTimes();
        replay(pceService);
        WebTarget wt = target();
        String response = wt.path("path").request().get(String.class);
        assertThat(response, is("{\"paths\":[]}"));
    }

    /**
     * Tests the result of a rest api GET for pce path id.
     */
    @Test
    public void testGetTunnelId() {
        expect(pceService.queryPath(anyObject()))
                         .andReturn(tunnel)
                         .anyTimes();
        replay(pceService);

        WebTarget wt = target();
        String response = wt.path("path/1").request().get(String.class);
        JsonObject result = Json.parse(response).asObject();
        assertThat(result, notNullValue());
    }

    /**
     * Tests that a fetch of a non-existent pce path object throws an exception.
     */
    @Test
    public void testBadGet() {
        expect(pceService.queryPath(anyObject()))
                         .andReturn(null)
                         .anyTimes();
        replay(pceService);

        WebTarget wt = target();
        try {
            wt.path("path/1").request().get(String.class);
            fail("Fetch of non-existent pce path did not throw an exception");
        } catch (NotFoundException ex) {
            assertThat(ex.getMessage(), containsString("HTTP 404 Not Found"));
        }
    }

    /**
     * Tests creating a pce path with POST.
     */
    @Test
    public void testPost() {
        expect(pceService.setupPath(anyObject(), anyObject(), anyObject(), anyObject(), anyObject(), anyObject()))
                         .andReturn(true)
                         .anyTimes();
        replay(pceService);

        WebTarget wt = target();
        InputStream jsonStream = PcePathResourceTest.class.getResourceAsStream("post-PcePath.json");

        Response response = wt.path("path")
                              .request(MediaType.APPLICATION_JSON_TYPE)
                              .post(Entity.json(jsonStream));
        assertThat(response.getStatus(), is(HttpURLConnection.HTTP_OK));
    }

    /**
     * Tests creating a pce path with PUT.
     */
    @Test
    public void testPut() {
        expect(pceService.updatePath(anyObject(), anyObject()))
                         .andReturn(true)
                         .anyTimes();
        replay(pceService);

        WebTarget wt = target();
        InputStream jsonStream = PcePathResourceTest.class.getResourceAsStream("post-PcePath.json");

        Response response = wt.path("path/1")
                              .request(MediaType.APPLICATION_JSON_TYPE)
                              .put(Entity.json(jsonStream));
        assertThat(response.getStatus(), is(HttpURLConnection.HTTP_OK));
    }

    /**
     * Tests deleting a pce path.
     */
    @Test
    public void testDelete() {
        expect(pceService.releasePath(anyObject()))
                         .andReturn(true)
                         .anyTimes();
        replay(pceService);

        WebTarget wt = target();

        String location = "path/1";

        Response deleteResponse = wt.path(location)
                                    .request(MediaType.APPLICATION_JSON_TYPE)
                                    .delete();
        assertThat(deleteResponse.getStatus(), is(HttpURLConnection.HTTP_OK));
    }
}
