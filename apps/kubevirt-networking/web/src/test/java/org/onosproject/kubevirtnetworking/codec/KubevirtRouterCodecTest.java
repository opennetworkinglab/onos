/*
 * Copyright 2021-present Open Networking Foundation
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
package org.onosproject.kubevirtnetworking.codec;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.hamcrest.MatcherAssert;
import org.junit.Before;
import org.junit.Test;
import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onosproject.codec.JsonCodec;
import org.onosproject.codec.impl.MockCodecContext;
import org.onosproject.core.CoreService;
import org.onosproject.kubevirtnetworking.api.DefaultKubevirtRouter;
import org.onosproject.kubevirtnetworking.api.KubevirtPeerRouter;
import org.onosproject.kubevirtnetworking.api.KubevirtRouter;

import java.io.IOException;
import java.io.InputStream;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.onosproject.kubevirtnetworking.codec.KubevirtRouterJsonMatcher.matchesKubevirtRouter;
import static org.onosproject.net.NetTestTools.APP_ID;

/**
 * Unit tests for KubevirtRouter codec.
 */
public final class KubevirtRouterCodecTest {

    MockCodecContext context;

    JsonCodec<KubevirtRouter> kubevirtRouterCodec;

    final CoreService mockCoreService = createMock(CoreService.class);
    private static final String REST_APP_ID = "org.onosproject.rest";

    @Before
    public void setUp() {
        context = new MockCodecContext();
        kubevirtRouterCodec = new KubevirtRouterCodec();

        assertThat(kubevirtRouterCodec, notNullValue());
        expect(mockCoreService.registerApplication(REST_APP_ID))
                .andReturn(APP_ID).anyTimes();
        replay(mockCoreService);
        context.registerService(CoreService.class, mockCoreService);
    }

    /**
     * Tests the kubevirt router encoding.
     */
    @Test
    public void testKubevirtRouterEncode() {
        KubevirtPeerRouter peerRouter = new KubevirtPeerRouter(IpAddress.valueOf("10.10.10.10"),
                MacAddress.valueOf("11:22:33:44:55:66"));

        KubevirtRouter router = DefaultKubevirtRouter.builder()
                .name("router-1")
                .enableSnat(true)
                .mac(MacAddress.valueOf("11:22:33:44:55:66"))
                .description("router-1")
                .internal(ImmutableSet.of("vlan-1"))
                .external(ImmutableMap.of("10.10.10.20", "flat-1"))
                .peerRouter(peerRouter)
                .electedGateway("gatewayNode")
                .build();

        ObjectNode routerJson = kubevirtRouterCodec.encode(router, context);
        assertThat(routerJson, matchesKubevirtRouter(router));
    }

    @Test
    public void testKubevirtRouterDecode() throws IOException {
        KubevirtRouter router = getKubevirtRouter("KubevirtRouter.json");

        assertEquals("router-1", router.name());
        assertEquals("Example Virtual Router", router.description());
        assertTrue(router.enableSnat());
        assertEquals("11:22:33:44:55:66", router.mac().toString());
        assertEquals("192.168.10.5",
                router.external().keySet().stream().findAny().orElse(null));
        assertEquals("external-network", router.external().get("192.168.10.5"));
        assertTrue(router.internal().contains("vxlan-network-1"));
        assertTrue(router.internal().contains("vxlan-network-2"));
        assertEquals("192.168.10.1", router.peerRouter().ipAddress().toString());
        assertEquals("gatewayNode", router.electedGateway());
    }

    private KubevirtRouter getKubevirtRouter(String resourceName) throws IOException {
        InputStream jsonStream = KubevirtRouterCodecTest.class.getResourceAsStream(resourceName);
        JsonNode json = context.mapper().readTree(jsonStream);
        MatcherAssert.assertThat(json, notNullValue());
        KubevirtRouter router = kubevirtRouterCodec.decode((ObjectNode) json, context);
        assertThat(router, notNullValue());
        return router;
    }
}
