/*
 * Copyright 2022-present Open Networking Foundation
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
package org.onosproject.kubevirtnetworking.web;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.google.common.collect.ImmutableSet;
import org.glassfish.jersey.server.ResourceConfig;
import org.junit.Before;
import org.junit.Test;
import org.onlab.osgi.ServiceDirectory;
import org.onlab.osgi.TestServiceDirectory;
import org.onlab.packet.IpAddress;
import org.onosproject.codec.CodecService;
import org.onosproject.codec.impl.CodecManager;
import org.onosproject.kubevirtnetworking.api.DefaultKubevirtNetwork;
import org.onosproject.kubevirtnetworking.api.DefaultKubevirtRouter;
import org.onosproject.kubevirtnetworking.api.KubevirtHostRoute;
import org.onosproject.kubevirtnetworking.api.KubevirtIpPool;
import org.onosproject.kubevirtnetworking.api.KubevirtNetwork;
import org.onosproject.kubevirtnetworking.api.KubevirtNetworkService;
import org.onosproject.kubevirtnetworking.api.KubevirtRouter;
import org.onosproject.kubevirtnetworking.api.KubevirtRouterService;
import org.onosproject.kubevirtnetworking.codec.KubevirtHostRouteCodec;
import org.onosproject.kubevirtnetworking.codec.KubevirtIpPoolCodec;
import org.onosproject.kubevirtnetworking.codec.KubevirtNetworkCodec;
import org.onosproject.kubevirtnode.api.DefaultKubevirtNode;
import org.onosproject.kubevirtnode.api.KubevirtNode;
import org.onosproject.kubevirtnode.api.KubevirtNodeService;
import org.onosproject.kubevirtnode.api.KubevirtNodeState;
import org.onosproject.net.DeviceId;
import org.onosproject.rest.resources.ResourceTest;

import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

public class KubevirtMm5WebResourceTest extends ResourceTest {

    final KubevirtRouterService mockRouterService = createMock(KubevirtRouterService.class);
    final KubevirtNodeService mockNodeService = createMock(KubevirtNodeService.class);
    final KubevirtNetworkService mockNetworkService = createMock(KubevirtNetworkService.class);

    private static final String VR_STATUS_PATH = "api/mm5/v1/status/vr/";
    private static final String NETWORK_PATH = "api/mm5/v1/network";

    private static final String ROUTER_NAME = "router1";
    private static final String HOST_NAME = "hostname1";


    private static final KubevirtNetwork NETWORK_1 = DefaultKubevirtNetwork.builder()
            .networkId("network")
            .name("network")
            .type(KubevirtNetwork.Type.FLAT)
            .cidr("10.10.10.0/24")
            .mtu(1500)
            .gatewayIp(IpAddress.valueOf("10.10.10.1"))
            .defaultRoute(true)
            .ipPool(new KubevirtIpPool(IpAddress.valueOf("10.10.10.100"),
                    IpAddress.valueOf("10.10.10.200")))
            .build();

    private static final KubevirtNode NODE_1 = DefaultKubevirtNode.builder()
            .hostname(HOST_NAME)
            .type(KubevirtNode.Type.GATEWAY)
            .dataIp(IpAddress.valueOf("10.134.34.222"))
            .managementIp(IpAddress.valueOf("10.134.231.30"))
            .intgBridge(DeviceId.deviceId("of:00000000000000a1"))
            .tunBridge(DeviceId.deviceId("of:00000000000000a2"))
            .state(KubevirtNodeState.COMPLETE)
            .build();


    private static final KubevirtRouter ROUTER_1 = DefaultKubevirtRouter.builder()
            .name(ROUTER_NAME).build();

    /**
     * Constructs a kubevirt mm5 resource test instance.
     */
    public KubevirtMm5WebResourceTest() {
        super(ResourceConfig.forApplicationClass(KubevirtNetworkingWebApplication.class));
    }

    /**
     * Sets up the global values for all the tests.
     */
    @Before
    public void setUpTest() {
        final CodecManager codecService = new CodecManager();
        codecService.activate();
        codecService.registerCodec(KubevirtNetwork.class, new KubevirtNetworkCodec());
        codecService.registerCodec(KubevirtHostRoute.class, new KubevirtHostRouteCodec());
        codecService.registerCodec(KubevirtIpPool.class, new KubevirtIpPoolCodec());

        ServiceDirectory testDirectory =
                new TestServiceDirectory()
                        .add(KubevirtNodeService.class, mockNodeService)
                        .add(KubevirtNetworkService.class, mockNetworkService)
                        .add(KubevirtRouterService.class, mockRouterService)
                        .add(CodecService.class, codecService);

        setServiceDirectory(testDirectory);
    }

    @Test
    public void testVrStatusWithExistingRouterOperation() {
        expect(mockRouterService.routers()).andReturn(ImmutableSet.of(ROUTER_1)).anyTimes();
        replay(mockRouterService);

        final WebTarget wt = target();

        final String stringResponse = wt.path(VR_STATUS_PATH + ROUTER_NAME).request().get(String.class);
        final Response response = wt.path(VR_STATUS_PATH + ROUTER_NAME).request().get();

        final JsonObject result = Json.parse(stringResponse).asObject();

        assertThat(result, notNullValue());
        assertThat(result.names(), hasSize(4));
        assertThat(response.getStatus(), is(200));

        verify(mockRouterService);
    }

    @Test
    public void testVrStatusWithNonRouterOperation() {
        expect(mockRouterService.routers()).andReturn(ImmutableSet.of(ROUTER_1)).anyTimes();
        replay(mockRouterService);

        final WebTarget wt = target();

        final Response response = wt.path(VR_STATUS_PATH + "anyRouter").request().get();
        assertThat(response.getStatus(), is(404));

        verify(mockRouterService);
    }

    @Test
    public void testGetNetworkOperation() {
        expect(mockNetworkService.networks()).andReturn(ImmutableSet.of(NETWORK_1)).anyTimes();
        replay(mockNetworkService);

        final WebTarget wt = target();

        final String response = wt.path(NETWORK_PATH).request().get(String.class);
        final JsonObject result = Json.parse(response).asObject();

        assertThat(result, notNullValue());
        assertThat(result.names(), hasSize(1));
        assertThat(result.names().get(0), is("networks"));

        final JsonArray jsonNodes = result.get("networks").asArray();
        assertThat(jsonNodes, notNullValue());
        assertThat(jsonNodes.size(), is(1));

        verify(mockNetworkService);
    }
}
