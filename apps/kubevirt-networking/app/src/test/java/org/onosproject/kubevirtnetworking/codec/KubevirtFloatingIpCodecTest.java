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
import org.hamcrest.MatcherAssert;
import org.junit.Before;
import org.junit.Test;
import org.onlab.packet.IpAddress;
import org.onosproject.codec.JsonCodec;
import org.onosproject.codec.impl.MockCodecContext;
import org.onosproject.core.CoreService;
import org.onosproject.kubevirtnetworking.api.DefaultKubevirtFloatingIp;
import org.onosproject.kubevirtnetworking.api.KubevirtFloatingIp;

import java.io.IOException;
import java.io.InputStream;

import static junit.framework.TestCase.assertEquals;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.onosproject.kubevirtnetworking.codec.KubevirtFloatingIpJsonMatcher.matchesKubevirtFloatingIp;
import static org.onosproject.net.NetTestTools.APP_ID;

/**
 * Unit tests for KubevirtFloatingIp codec.
 */
public final class KubevirtFloatingIpCodecTest {

    MockCodecContext context;

    JsonCodec<KubevirtFloatingIp> kubevirtFloatingIpCodec;

    final CoreService mockCoreService = createMock(CoreService.class);
    private static final String REST_APP_ID = "org.onosproject.rest";

    @Before
    public void setUp() {
        context = new MockCodecContext();
        kubevirtFloatingIpCodec = new KubevirtFloatingIpCodec();

        assertThat(kubevirtFloatingIpCodec, notNullValue());
        expect(mockCoreService.registerApplication(REST_APP_ID))
                .andReturn(APP_ID).anyTimes();
        replay(mockCoreService);
        context.registerService(CoreService.class, mockCoreService);
    }

    /**
     * Tests the kubevirt floating IP encoding.
     */
    @Test
    public void testKubevirtFloatingIpEncode() {
        KubevirtFloatingIp floatingIp = DefaultKubevirtFloatingIp.builder()
                .id("fip-id")
                .routerName("router-1")
                .networkName("flat-1")
                .floatingIp(IpAddress.valueOf("10.10.10.10"))
                .podName("pod-1")
                .vmName("vm-1")
                .fixedIp(IpAddress.valueOf("20.20.20.20"))
                .build();

        ObjectNode floatingIpJson = kubevirtFloatingIpCodec.encode(floatingIp, context);
        assertThat(floatingIpJson, matchesKubevirtFloatingIp(floatingIp));
    }

    @Test
    public void testKubevirtFloatingIpDecode() throws IOException {
        KubevirtFloatingIp floatingIp = getKubevirtFloatingIp("KubevirtFloatingIp.json");

        assertEquals("fip-1", floatingIp.id());
        assertEquals("router-1", floatingIp.routerName());
        assertEquals("flat-1", floatingIp.networkName());
        assertEquals("10.10.10.10", floatingIp.floatingIp().toString());
        assertEquals("pod-1", floatingIp.podName());
        assertEquals("vm-1", floatingIp.vmName());
        assertEquals("20.20.20.20", floatingIp.fixedIp().toString());
    }

    private KubevirtFloatingIp getKubevirtFloatingIp(String resourceName) throws IOException {
        InputStream jsonStream = KubevirtFloatingIpCodecTest.class.getResourceAsStream(resourceName);
        JsonNode json = context.mapper().readTree(jsonStream);
        MatcherAssert.assertThat(json, notNullValue());
        KubevirtFloatingIp fip = kubevirtFloatingIpCodec.decode((ObjectNode) json, context);
        assertThat(fip, notNullValue());
        return fip;
    }
}
