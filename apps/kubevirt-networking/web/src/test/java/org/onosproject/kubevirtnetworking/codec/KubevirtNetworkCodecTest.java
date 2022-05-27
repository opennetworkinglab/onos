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
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableSet;
import org.hamcrest.MatcherAssert;
import org.junit.Before;
import org.junit.Test;
import org.onlab.packet.IpAddress;
import org.onlab.packet.IpPrefix;
import org.onosproject.codec.CodecContext;
import org.onosproject.codec.JsonCodec;
import org.onosproject.codec.impl.CodecManager;
import org.onosproject.core.CoreService;
import org.onosproject.kubevirtnetworking.api.DefaultKubevirtNetwork;
import org.onosproject.kubevirtnetworking.api.KubevirtHostRoute;
import org.onosproject.kubevirtnetworking.api.KubevirtIpPool;
import org.onosproject.kubevirtnetworking.api.KubevirtNetwork;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.onosproject.kubevirtnetworking.codec.KubevirtNetworkJsonMatcher.matchesKubevirtNetwork;
import static org.onosproject.net.NetTestTools.APP_ID;

/**
 * Unit tests for KubevirtNetwork codec.
 */
public final class KubevirtNetworkCodecTest {

    MockCodecContext context;

    JsonCodec<KubevirtNetwork> kubevirtNetworkCodec;
    JsonCodec<KubevirtHostRoute> kubevirtHostRouteCodec;
    JsonCodec<KubevirtIpPool> kubevirtIpPoolCodec;

    final CoreService mockCoreService = createMock(CoreService.class);
    private static final String REST_APP_ID = "org.onosproject.rest";

    @Before
    public void setUp() {
        context = new MockCodecContext();
        kubevirtNetworkCodec = new KubevirtNetworkCodec();
        kubevirtHostRouteCodec = new KubevirtHostRouteCodec();
        kubevirtIpPoolCodec = new KubevirtIpPoolCodec();

        assertThat(kubevirtNetworkCodec, notNullValue());
        assertThat(kubevirtHostRouteCodec, notNullValue());
        assertThat(kubevirtIpPoolCodec, notNullValue());

        expect(mockCoreService.registerApplication(REST_APP_ID))
                .andReturn(APP_ID).anyTimes();
        replay(mockCoreService);
        context.registerService(CoreService.class, mockCoreService);
    }

    /**
     * Tests the kubevirt network encoding.
     */
    @Test
    public void testKubevirtNetworkEncode() {
        KubevirtHostRoute hostRoute1 = new KubevirtHostRoute(IpPrefix.valueOf("10.10.10.0/24"),
                IpAddress.valueOf("20.20.20.1"));
        KubevirtHostRoute hostRoute2 = new KubevirtHostRoute(IpPrefix.valueOf("20.20.20.0/24"),
                IpAddress.valueOf("10.10.10.1"));

        KubevirtIpPool ipPool = new KubevirtIpPool(IpAddress.valueOf("10.10.10.100"),
                IpAddress.valueOf("10.10.10.200"));

        KubevirtNetwork network = DefaultKubevirtNetwork.builder()
                .networkId("net-1")
                .name("net-1")
                .type(KubevirtNetwork.Type.FLAT)
                .gatewayIp(IpAddress.valueOf("10.10.10.1"))
                .defaultRoute(true)
                .mtu(1500)
                .cidr("10.10.10.0/24")
                .hostRoutes(ImmutableSet.of(hostRoute1, hostRoute2))
                .ipPool(ipPool)
                .dnses(ImmutableSet.of(IpAddress.valueOf("8.8.8.8")))
                .build();

        ObjectNode networkJson = kubevirtNetworkCodec.encode(network, context);
        assertThat(networkJson, matchesKubevirtNetwork(network));
    }

    /**
     * Tests the kubevirt network decoding.
     *
     * @throws IOException io exception
     */
    @Test
    public void testKubevirtNetworkDecode() throws IOException {
        KubevirtNetwork network = getKubevirtNetwork("KubevirtNetwork.json");

        assertThat(network.networkId(), is("network-1"));
        assertThat(network.name(), is("network-1"));
        assertThat(network.type().name(), is("FLAT"));
        assertThat(network.cidr(), is("10.10.0.0/24"));
        assertThat(network.gatewayIp().toString(), is("10.10.0.1"));
        assertThat(network.defaultRoute(), is(true));
        assertThat(network.ipPool().start().toString(), is("10.10.10.100"));
        assertThat(network.ipPool().end().toString(), is("10.10.10.200"));
        assertThat(network.dnses().size(), is(1));
        KubevirtHostRoute route = network.hostRoutes().stream().findFirst().orElse(null);
        assertThat(route, is(new KubevirtHostRoute(IpPrefix.valueOf("10.10.10.0/24"),
                IpAddress.valueOf("10.10.10.1"))));
    }

    private KubevirtNetwork getKubevirtNetwork(String resourceName) throws IOException {
        InputStream jsonStream = KubevirtNetworkCodecTest.class.getResourceAsStream(resourceName);
        JsonNode json = context.mapper().readTree(jsonStream);
        MatcherAssert.assertThat(json, notNullValue());
        KubevirtNetwork network = kubevirtNetworkCodec.decode((ObjectNode) json, context);
        assertThat(network, notNullValue());
        return network;
    }

    private class MockCodecContext implements CodecContext {

        private final ObjectMapper mapper = new ObjectMapper();
        private final CodecManager manager = new CodecManager();
        private final Map<Class<?>, Object> services = new HashMap<>();

        /**
         * Constructs a new mock codec context.
         */
        public MockCodecContext() {
            manager.activate();
        }

        @Override
        public ObjectMapper mapper() {
            return mapper;
        }

        @Override
        public <T> JsonCodec<T> codec(Class<T> entityClass) {
            if (entityClass == KubevirtHostRoute.class) {
                return (JsonCodec<T>) kubevirtHostRouteCodec;
            }

            if (entityClass == KubevirtIpPool.class) {
                return (JsonCodec<T>) kubevirtIpPoolCodec;
            }

            return manager.getCodec(entityClass);
        }

        @Override
        public <T> T getService(Class<T> serviceClass) {
            return (T) services.get(serviceClass);
        }

        // for registering mock services
        public <T> void registerService(Class<T> serviceClass, T impl) {
            services.put(serviceClass, impl);
        }
    }
}
