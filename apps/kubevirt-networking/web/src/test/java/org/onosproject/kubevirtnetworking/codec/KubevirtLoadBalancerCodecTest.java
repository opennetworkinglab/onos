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
import org.onosproject.codec.CodecContext;
import org.onosproject.codec.JsonCodec;
import org.onosproject.codec.impl.CodecManager;
import org.onosproject.core.CoreService;
import org.onosproject.kubevirtnetworking.api.DefaultKubevirtLoadBalancer;
import org.onosproject.kubevirtnetworking.api.DefaultKubevirtLoadBalancerRule;
import org.onosproject.kubevirtnetworking.api.KubevirtLoadBalancer;
import org.onosproject.kubevirtnetworking.api.KubevirtLoadBalancerRule;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.onosproject.kubevirtnetworking.codec.KubevirtLoadBalancerJsonMatcher.matchesKubevirtLoadBalancer;
import static org.onosproject.net.NetTestTools.APP_ID;

/**
 * Unit tests for KubevirtLoadBalancer codec.
 */
public final class KubevirtLoadBalancerCodecTest {

    MockCodecContext context;

    JsonCodec<KubevirtLoadBalancer> kubevirtLoadBalancerCodec;
    JsonCodec<KubevirtLoadBalancerRule> kubevirtLoadBalancerRuleCodec;

    private static final KubevirtLoadBalancerRule RULE1 = DefaultKubevirtLoadBalancerRule.builder()
            .protocol("tcp")
            .portRangeMax(8000)
            .portRangeMin(7000)
            .build();
    private static final KubevirtLoadBalancerRule RULE2 = DefaultKubevirtLoadBalancerRule.builder()
            .protocol("udp")
            .portRangeMax(9000)
            .portRangeMin(8000)
            .build();

    private static final KubevirtLoadBalancerRule RULE3 = DefaultKubevirtLoadBalancerRule.builder()
            .protocol("icmp")
            .build();

    final CoreService mockCoreService = createMock(CoreService.class);
    private static final String REST_APP_ID = "org.onosproject.rest";

    @Before
    public void setUp() {
        context = new MockCodecContext();
        kubevirtLoadBalancerCodec = new KubevirtLoadBalancerCodec();
        kubevirtLoadBalancerRuleCodec = new KubevirtLoadBalancerRuleCodec();

        assertThat(kubevirtLoadBalancerCodec, notNullValue());
        assertThat(kubevirtLoadBalancerRuleCodec, notNullValue());

        expect(mockCoreService.registerApplication(REST_APP_ID))
                .andReturn(APP_ID).anyTimes();
        replay(mockCoreService);
        context.registerService(CoreService.class, mockCoreService);
    }

    /**
     * Tests the kubevirt load balancer encoding.
     */
    @Test
    public void testKubevirtLoadBalancerEncode() {
        KubevirtLoadBalancer lb = DefaultKubevirtLoadBalancer.builder()
                .name("lb-1")
                .networkId("net-1")
                .vip(IpAddress.valueOf("10.10.10.10"))
                .members(ImmutableSet.of(IpAddress.valueOf("10.10.10.11"),
                        IpAddress.valueOf("10.10.10.12")))
                .rules(ImmutableSet.of(RULE1, RULE2, RULE3))
                .description("network load balancer")
                .build();

        ObjectNode lbJson = kubevirtLoadBalancerCodec.encode(lb, context);
        assertThat(lbJson, matchesKubevirtLoadBalancer(lb));
    }

    /**
     * Tests the kubevirt load balancer decoding.
     */
    @Test
    public void testKubevirtLoadBalancerDecode() throws IOException {
        KubevirtLoadBalancer lb = getKubevirtLoadBalancer("KubevirtLoadBalancer.json");

        assertThat(lb.name(), is("lb-1"));
        assertThat(lb.description(), is("Example Load Balancer"));
        assertThat(lb.networkId(), is("net-1"));
        assertThat(lb.vip(), is(IpAddress.valueOf("10.10.10.10")));

        Set<IpAddress> expectedMembers = ImmutableSet.of(IpAddress.valueOf("10.10.10.11"),
                IpAddress.valueOf("10.10.10.12"));
        Set<IpAddress> realMembers = lb.members();
        assertThat(true, is(expectedMembers.containsAll(realMembers)));
        assertThat(true, is(realMembers.containsAll(expectedMembers)));

        Set<KubevirtLoadBalancerRule> expectedRules = ImmutableSet.of(RULE1, RULE2, RULE3);
        Set<KubevirtLoadBalancerRule> realRules = lb.rules();
        assertThat(true, is(expectedRules.containsAll(realRules)));
        assertThat(true, is(realRules.containsAll(expectedRules)));
    }

    private KubevirtLoadBalancer getKubevirtLoadBalancer(String resourceName) throws IOException {
        InputStream jsonStream = KubevirtLoadBalancerCodecTest.class.getResourceAsStream(resourceName);
        JsonNode json = context.mapper().readTree(jsonStream);
        MatcherAssert.assertThat(json, notNullValue());
        KubevirtLoadBalancer lb = kubevirtLoadBalancerCodec.decode((ObjectNode) json, context);
        assertThat(lb, notNullValue());
        return lb;
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
            if (entityClass == KubevirtLoadBalancerRule.class) {
                return (JsonCodec<T>) kubevirtLoadBalancerRuleCodec;
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
