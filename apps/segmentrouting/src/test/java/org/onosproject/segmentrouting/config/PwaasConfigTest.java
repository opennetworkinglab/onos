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

package org.onosproject.segmentrouting.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.onlab.packet.MplsLabel;
import org.onlab.packet.VlanId;
import org.onosproject.TestApplicationId;
import org.onosproject.core.ApplicationId;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.config.Config;
import org.onosproject.net.config.ConfigApplyDelegate;
import org.onosproject.segmentrouting.SegmentRoutingManager;
import org.onosproject.segmentrouting.pwaas.DefaultL2Tunnel;
import org.onosproject.segmentrouting.pwaas.DefaultL2TunnelDescription;
import org.onosproject.segmentrouting.pwaas.DefaultL2TunnelPolicy;
import org.onosproject.segmentrouting.pwaas.L2Mode;

import java.io.InputStream;
import java.util.Set;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;

/**
 * Unit tests for class {@link PwaasConfig}.
 */
public class PwaasConfigTest {

    private static final String TUNNEL_ID_1 = "1";
    private static final String TUNNEL_ID_2 = "20";
    private static final String NOT_PRESENT_TUNNEL_ID = "2";
    private static final ConnectPoint INGRESS_1 = ConnectPoint.deviceConnectPoint("of:0000000000000001/1");
    private static final ConnectPoint INGRESS_2 = ConnectPoint.deviceConnectPoint("of:0000000000000002/1");
    private static final ConnectPoint EGRESS_1 = ConnectPoint.deviceConnectPoint("of:0000000000000011/1");
    private static final ConnectPoint EGRESS_2 = ConnectPoint.deviceConnectPoint("of:0000000000000012/1");
    private static final VlanId INGRESS_INNER_TAG_1 = VlanId.vlanId("10");
    private static final VlanId INGRESS_INNER_TAG_2 = VlanId.vlanId("100");
    private static final VlanId INGRESS_OUTER_TAG_1 = VlanId.vlanId("20");
    private static final VlanId INGRESS_OUTER_TAG_2 = VlanId.vlanId("200");
    private static final VlanId EGRESS_INNER_TAG_1 = VlanId.vlanId("11");
    private static final VlanId EGRESS_INNER_TAG_2 = VlanId.vlanId("110");
    private static final VlanId EGRESS_OUTER_TAG_1 = VlanId.vlanId("21");
    private static final VlanId EGRESS_OUTER_TAG_2 = VlanId.vlanId("210");
    private static final String MODE_1 = "RAW";
    private static final String MODE_2 = "TAGGED";
    private static final boolean ALL_VLAN_1 = true;
    private static final boolean ALL_VLAN_2 = false;
    private static final VlanId SD_TAG_1 = VlanId.vlanId("40");
    private static final VlanId SD_TAG_2 = VlanId.NONE;
    private static final MplsLabel PW_LABEL_1 = MplsLabel.mplsLabel("255");
    private static final MplsLabel PW_LABEL_2 = MplsLabel.mplsLabel("4095");

    private PwaasConfig config;
    private PwaasConfig invalidConfig;

    @Before
    public void setUp() throws Exception {
        InputStream jsonStream = PwaasConfig.class
                .getResourceAsStream("/pwaas.json");
        InputStream invalidJsonStream = PwaasConfig.class
                .getResourceAsStream("/pwaas-invalid.json");

        String key = SegmentRoutingManager.APP_NAME;
        ApplicationId subject = new TestApplicationId(key);
        ObjectMapper mapper = new ObjectMapper();
        JsonNode jsonNode = mapper.readTree(jsonStream);
        JsonNode invalidJsonNode = mapper.readTree(invalidJsonStream);
        ConfigApplyDelegate delegate = new MockDelegate();

        config = new PwaasConfig();
        config.init(subject, key, jsonNode, mapper, delegate);
        invalidConfig = new PwaasConfig();
        invalidConfig.init(subject, key, invalidJsonNode, mapper, delegate);
    }

    /**
     * Tests config validity.
     */
    @Test
    public void testIsValid() {
        assertTrue(config.isValid());
        assertFalse(invalidConfig.isValid());
    }

    /**
     * Tests getPwIds.
     */
    @Test
    public void testGetPwIds() {
        Set<Long> pwIds = config.getPwIds();
        assertThat(pwIds.size(), is(2));
        assertTrue(pwIds.contains(Long.parseLong(TUNNEL_ID_1)));
        assertTrue(pwIds.contains(Long.parseLong(TUNNEL_ID_2)));
        assertFalse(pwIds.contains(Long.parseLong(NOT_PRESENT_TUNNEL_ID)));
    }

    /**
     * Tests getPwDescription.
     */
    @Test
    public void testGetPwDescription() {
        DefaultL2TunnelDescription l2TunnelDescription = null;

        DefaultL2Tunnel l2Tunnel = new DefaultL2Tunnel(
            L2Mode.valueOf(MODE_1),
            SD_TAG_1,
            Long.parseLong(TUNNEL_ID_1),
            PW_LABEL_1
        );
        DefaultL2TunnelPolicy l2TunnelPolicy = new DefaultL2TunnelPolicy(
                Long.parseLong(TUNNEL_ID_1),
                INGRESS_1,
                INGRESS_INNER_TAG_1,
                INGRESS_OUTER_TAG_1,
                EGRESS_1,
                EGRESS_INNER_TAG_1,
                EGRESS_OUTER_TAG_1,
                ALL_VLAN_1
        );
        l2TunnelDescription = config.getPwDescription(Long.parseLong(TUNNEL_ID_1));
        assertThat(l2TunnelDescription.l2Tunnel().pwMode(), is(l2Tunnel.pwMode()));
        assertThat(l2TunnelDescription.l2Tunnel().sdTag(), is(l2Tunnel.sdTag()));
        assertThat(l2TunnelDescription.l2Tunnel().tunnelId(), is(l2Tunnel.tunnelId()));
        assertThat(l2TunnelDescription.l2Tunnel().pwLabel(), is(l2Tunnel.pwLabel()));
        assertThat(l2TunnelDescription.l2TunnelPolicy().tunnelId(), is(l2TunnelPolicy.tunnelId()));
        assertThat(l2TunnelDescription.l2TunnelPolicy().cP1InnerTag(), is(l2TunnelPolicy.cP1InnerTag()));
        assertThat(l2TunnelDescription.l2TunnelPolicy().cP1OuterTag(), is(l2TunnelPolicy.cP1OuterTag()));
        assertThat(l2TunnelDescription.l2TunnelPolicy().cP2InnerTag(), is(l2TunnelPolicy.cP2InnerTag()));
        assertThat(l2TunnelDescription.l2TunnelPolicy().cP2OuterTag(), is(l2TunnelPolicy.cP2OuterTag()));
        assertThat(l2TunnelDescription.l2TunnelPolicy().cP1(), is(l2TunnelPolicy.cP1()));
        assertThat(l2TunnelDescription.l2TunnelPolicy().cP2(), is(l2TunnelPolicy.cP2()));
        assertThat(l2TunnelDescription.l2TunnelPolicy().isAllVlan(), is(l2TunnelPolicy.isAllVlan()));

        l2Tunnel = new DefaultL2Tunnel(
                L2Mode.valueOf(MODE_2),
                SD_TAG_2,
                Long.parseLong(TUNNEL_ID_2),
                PW_LABEL_2
        );
        l2TunnelPolicy = new DefaultL2TunnelPolicy(
                Long.parseLong(TUNNEL_ID_2),
                INGRESS_2,
                INGRESS_INNER_TAG_2,
                INGRESS_OUTER_TAG_2,
                EGRESS_2,
                EGRESS_INNER_TAG_2,
                EGRESS_OUTER_TAG_2,
                ALL_VLAN_2
        );
        l2TunnelDescription = config.getPwDescription(Long.parseLong(TUNNEL_ID_2));
        assertThat(l2TunnelDescription.l2Tunnel().pwMode(), is(l2Tunnel.pwMode()));
        assertThat(l2TunnelDescription.l2Tunnel().sdTag(), is(l2Tunnel.sdTag()));
        assertThat(l2TunnelDescription.l2Tunnel().tunnelId(), is(l2Tunnel.tunnelId()));
        assertThat(l2TunnelDescription.l2Tunnel().pwLabel(), is(l2Tunnel.pwLabel()));
        assertThat(l2TunnelDescription.l2TunnelPolicy().tunnelId(), is(l2TunnelPolicy.tunnelId()));
        assertThat(l2TunnelDescription.l2TunnelPolicy().cP1InnerTag(), is(l2TunnelPolicy.cP1InnerTag()));
        assertThat(l2TunnelDescription.l2TunnelPolicy().cP1OuterTag(), is(l2TunnelPolicy.cP1OuterTag()));
        assertThat(l2TunnelDescription.l2TunnelPolicy().cP2OuterTag(), is(l2TunnelPolicy.cP2OuterTag()));
        assertThat(l2TunnelDescription.l2TunnelPolicy().cP2OuterTag(), is(l2TunnelPolicy.cP2OuterTag()));
        assertThat(l2TunnelDescription.l2TunnelPolicy().cP1(), is(l2TunnelPolicy.cP1()));
        assertThat(l2TunnelDescription.l2TunnelPolicy().cP2(), is(l2TunnelPolicy.cP2()));
        assertThat(l2TunnelDescription.l2TunnelPolicy().isAllVlan(), is(l2TunnelPolicy.isAllVlan()));

    }

    private class MockDelegate implements ConfigApplyDelegate {
        @Override
        public void onApply(Config config) {
        }
    }

}
