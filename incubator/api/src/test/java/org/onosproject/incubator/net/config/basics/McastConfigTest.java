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

package org.onosproject.incubator.net.config.basics;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.annotations.Beta;
import org.junit.Before;
import org.junit.Test;
import org.onlab.packet.VlanId;
import org.onosproject.TestApplicationId;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.config.Config;
import org.onosproject.net.config.ConfigApplyDelegate;

import java.io.InputStream;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;

/**
 * Tests for class {@link McastConfig}.
 */
@Beta
public class McastConfigTest {
    private static final TestApplicationId APP_ID =
            new TestApplicationId(CoreService.CORE_APP_NAME);
    private McastConfig config;
    private McastConfig invalidConfig;

    private static final VlanId INGRESS_VLAN_1 = VlanId.NONE;
    private static final VlanId EGRESS_VLAN_1 = VlanId.NONE;
    private static final VlanId INGRESS_VLAN_2 = VlanId.vlanId((short) 100);
    private static final VlanId EGRESS_VLAN_2 = VlanId.vlanId((short) 100);

    /**
     * Initialize test related variables.
     *
     * @throws Exception
     */
    @Before
    public void setUp() throws Exception {
        InputStream jsonStream = McastConfigTest.class
                .getResourceAsStream("/mcast-config.json");
        InputStream invalidJsonStream = McastConfigTest.class
                .getResourceAsStream("/mcast-config-invalid.json");

        ApplicationId subject = APP_ID;
        String key = CoreService.CORE_APP_NAME;
        ObjectMapper mapper = new ObjectMapper();
        JsonNode jsonNode = mapper.readTree(jsonStream);
        JsonNode invalidJsonNode = mapper.readTree(invalidJsonStream);
        ConfigApplyDelegate delegate = new MockDelegate();

        config = new McastConfig();
        config.init(subject, key, jsonNode, mapper, delegate);
        invalidConfig = new McastConfig();
        invalidConfig.init(subject, key, invalidJsonNode, mapper, delegate);
    }

    /**
     * Tests config validity.
     *
     * @throws Exception
     */
    @Test
    public void isValid() throws Exception {
        assertTrue(config.isValid());
        assertFalse(invalidConfig.isValid());
    }

    /**
     * Tests ingress VLAN getter.
     *
     * @throws Exception
     */
    @Test
    public void ingressVlan() throws Exception {
        VlanId ingressVlan = config.ingressVlan();
        assertNotNull("ingressVlan should not be null", ingressVlan);
        assertThat(ingressVlan, is(INGRESS_VLAN_1));
    }

    /**
     * Tests ingress VLAN setter.
     *
     * @throws Exception
     */
    @Test
    public void setIngressVlan() throws Exception {
        config.setIngressVlan(INGRESS_VLAN_2);

        VlanId ingressVlan = config.ingressVlan();
        assertNotNull("ingressVlan should not be null", ingressVlan);
        assertThat(ingressVlan, is(INGRESS_VLAN_2));
    }

    /**
     * Tests egress VLAN getter.
     *
     * @throws Exception
     */
    @Test
    public void egressVlan() throws Exception {
        VlanId egressVlan = config.egressVlan();
        assertNotNull("egressVlan should not be null", egressVlan);
        assertThat(egressVlan, is(EGRESS_VLAN_1));
    }

    /**
     * Tests egress VLAN setter.
     *
     * @throws Exception
     */
    @Test
    public void setEgressVlan() throws Exception {
        config.setEgressVlan(EGRESS_VLAN_2);

        VlanId egressVlan = config.egressVlan();
        assertNotNull("egressVlan should not be null", egressVlan);
        assertThat(egressVlan, is(EGRESS_VLAN_2));
    }

    private class MockDelegate implements ConfigApplyDelegate {
        @Override
        public void onApply(Config config) {
        }
    }
}