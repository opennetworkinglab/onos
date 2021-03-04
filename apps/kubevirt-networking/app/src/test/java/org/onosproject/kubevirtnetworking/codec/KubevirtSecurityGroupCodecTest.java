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
import org.onlab.packet.IpPrefix;
import org.onosproject.codec.CodecContext;
import org.onosproject.codec.JsonCodec;
import org.onosproject.codec.impl.CodecManager;
import org.onosproject.core.CoreService;
import org.onosproject.kubevirtnetworking.api.DefaultKubevirtSecurityGroup;
import org.onosproject.kubevirtnetworking.api.DefaultKubevirtSecurityGroupRule;
import org.onosproject.kubevirtnetworking.api.KubevirtSecurityGroup;
import org.onosproject.kubevirtnetworking.api.KubevirtSecurityGroupRule;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import static junit.framework.TestCase.assertEquals;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.onosproject.kubevirtnetworking.codec.KubevirtSecurityGroupJsonMatcher.matchesKubevirtSecurityGroup;
import static org.onosproject.net.NetTestTools.APP_ID;

/**
 * Unit tests for KubevirtSecurityGroup codec.
 */
public final class KubevirtSecurityGroupCodecTest {

    MockCodecContext context;

    JsonCodec<KubevirtSecurityGroup> kubevirtSecurityGroupCodec;
    JsonCodec<KubevirtSecurityGroupRule> kubevirtSecurityGroupRuleCodec;

    final CoreService mockCoreService = createMock(CoreService.class);
    private static final String REST_APP_ID = "org.onosproject.rest";

    @Before
    public void setUp() {
        context = new MockCodecContext();
        kubevirtSecurityGroupCodec = new KubevirtSecurityGroupCodec();
        kubevirtSecurityGroupRuleCodec = new KubevirtSecurityGroupRuleCodec();

        assertThat(kubevirtSecurityGroupCodec, notNullValue());
        assertThat(kubevirtSecurityGroupRuleCodec, notNullValue());
        expect(mockCoreService.registerApplication(REST_APP_ID))
                .andReturn(APP_ID).anyTimes();
        replay(mockCoreService);
        context.registerService(CoreService.class, mockCoreService);
    }

    /**
     * Tests the kubevirt security group encoding.
     */
    @Test
    public void testKubevirtSecurityGroupEncode() {
        KubevirtSecurityGroupRule rule = DefaultKubevirtSecurityGroupRule.builder()
                .id("sgr-1")
                .securityGroupId("sg-1")
                .direction("ingress")
                .etherType("IPv4")
                .portRangeMin(0)
                .portRangeMax(80)
                .protocol("tcp")
                .remoteIpPrefix(IpPrefix.valueOf("0.0.0.0/0"))
                .remoteGroupId("g-1")
                .build();

        KubevirtSecurityGroup sg = DefaultKubevirtSecurityGroup.builder()
                .id("sg-1")
                .name("sg")
                .description("example-sg")
                .rules(ImmutableSet.of(rule))
                .build();

        ObjectNode sgJson = kubevirtSecurityGroupCodec.encode(sg, context);
        assertThat(sgJson, matchesKubevirtSecurityGroup(sg));
    }

    /**
     * Tests the kubevirt security group decoding.
     */
    @Test
    public void testKubevirtSecurityGroupDecode() throws IOException {
        KubevirtSecurityGroup sg = getKubevirtSecurityGroup("KubevirtSecurityGroup.json");
        KubevirtSecurityGroupRule rule = sg.rules().stream().findAny().orElse(null);

        assertEquals("sg-1", sg.id());
        assertEquals("sg", sg.name());
        assertEquals("example-sg", sg.description());

        assertEquals("sgr-1", rule.id());
        assertEquals("sg-1", rule.securityGroupId());
        assertEquals("ingress", rule.direction());
        assertEquals("IPv4", rule.etherType());
        assertEquals((Integer) 80, rule.portRangeMax());
        assertEquals((Integer) 0, rule.portRangeMin());
        assertEquals("tcp", rule.protocol());
        assertEquals("0.0.0.0/0", rule.remoteIpPrefix().toString());
        assertEquals("g-1", rule.remoteGroupId());
    }

    private KubevirtSecurityGroup getKubevirtSecurityGroup(String resourceName) throws IOException {
        InputStream jsonStream = KubevirtSecurityGroupCodecTest.class.getResourceAsStream(resourceName);
        JsonNode json = context.mapper().readTree(jsonStream);
        MatcherAssert.assertThat(json, notNullValue());
        KubevirtSecurityGroup sg = kubevirtSecurityGroupCodec.decode((ObjectNode) json, context);
        assertThat(sg, notNullValue());
        return sg;
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
            if (entityClass == KubevirtSecurityGroupRule.class) {
                return (JsonCodec<T>) kubevirtSecurityGroupRuleCodec;
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
