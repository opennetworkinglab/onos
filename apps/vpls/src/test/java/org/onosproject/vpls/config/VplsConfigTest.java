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
package org.onosproject.vpls.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.onosproject.TestApplicationId;
import org.onosproject.core.ApplicationId;
import org.onosproject.net.config.Config;
import org.onosproject.net.config.ConfigApplyDelegate;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Tests for the {@link VplsConfig} class.
 */
public class VplsConfigTest {
    private static final String APP_NAME = "org.onosproject.vpls";
    private static final ApplicationId APP_ID = new TestApplicationId(APP_NAME);
    private static final String VPLS = "vplsNetworks";
    private static final String NAME = "name";
    private static final String INTERFACE = "interfaces";
    private static final String NET1 = "net1";
    private static final String NET2 = "net2";
    private static final String NEWNET = "newnet";

    private static final String IF1 = "sw5-4-100";
    private static final String IF2 = "sw5-4-200";
    private static final String IF3 = "sw5-5-100";
    private static final String IF4 = "sw6-5-100";
    private static final String IF5 = "sw6-5-400";
    private static final String IF_NON_EXIST = "sw7-5-100";

    private static final String JSON_TREE = "{\"" + VPLS +
            "\" : [{\"" + NAME + "\" : \"net1\"," +
            "\"" + INTERFACE + "\" : [" +
            "\"sw5-4-100\",\"sw5-4-200\",\"sw5-5-100\"]}]}";
    private static final String EMPTY_JSON_TREE = "{}";

    private final ObjectMapper mapper = new ObjectMapper();
    private final ConfigApplyDelegate delegate = new MockCfgDelegate();
    private final VplsNetworkConfig initialNetwork = createInitialNetwork();

    private Set<VplsNetworkConfig> networks = new HashSet<>();
    private VplsConfig vplsConfig = new VplsConfig();
    private VplsConfig emptyVplsConfig = new VplsConfig();

    @Before
    public void setUp() throws Exception {
        JsonNode tree = new ObjectMapper().readTree(JSON_TREE);
        vplsConfig.init(APP_ID, APP_NAME, tree, mapper, delegate);
        JsonNode emptyTree = new ObjectMapper().readTree(EMPTY_JSON_TREE);
        emptyVplsConfig.init(APP_ID, APP_NAME, emptyTree, mapper, delegate);
        networks.add(initialNetwork);
    }

    /**
     * Tests if a VPLS configuration can be retrieved from JSON.
     */
    @Test
    public void testVplsNetworks() {
        assertEquals(networks, vplsConfig.vplsNetworks());
    }

    /**
     * Tests an empty VPLS application configuration is retrieved from JSON.
     */
    @Test
    public void testEmptyVplsNetworks() {
        assertTrue(emptyVplsConfig.vplsNetworks().isEmpty());
    }

    /**
     * Tests if a VPLS can be found by name.
     */
    @Test
    public void testGetVplsWithName() {
        assertNotNull(vplsConfig.getVplsWithName(NET1));
        assertNull(vplsConfig.getVplsWithName(NET2));
    }

    /**
     * Tests the addition of a new VPLS.
     */
    @Test
    public void testAddNetwork() {
        int initialSize = vplsConfig.vplsNetworks().size();
        VplsNetworkConfig newNetwork = createNewNetwork();
        vplsConfig.addVpls(newNetwork);
        assertEquals(initialSize + 1, vplsConfig.vplsNetworks().size());
        networks.add(newNetwork);
        assertEquals(networks, vplsConfig.vplsNetworks());
    }

    /**
     * Tests the addition of new VPLS to an empty configuration.
     */
    @Test
    public void testAddNetworkToEmpty() {
        VplsNetworkConfig newNetwork = createNewNetwork();
        emptyVplsConfig.addVpls(newNetwork);

        assertFalse(emptyVplsConfig.vplsNetworks().isEmpty());
    }

    /**
     * Tests the removal of an existing VPLS from the configuration.
     */
    @Test
    public void testRemoveExistingNetwork() {
        int initialSize = vplsConfig.vplsNetworks().size();
        vplsConfig.removeVpls(NET1);

        assertEquals(initialSize - 1, vplsConfig.vplsNetworks().size());
    }

    /**
     * Tests the removal of a non-existing VPLS from the configuration.
     */
    @Test
    public void testRemoveInexistingNetwork() {
        int initialSize = vplsConfig.vplsNetworks().size();
        vplsConfig.removeVpls(NET2);

        assertEquals(initialSize, vplsConfig.vplsNetworks().size());
    }

    /**
     * Tests the addition of a new interface.
     */
    @Test
    public void testAddInterfaceToNetwork() {
        int initialSize = vplsConfig.getVplsWithName(NET1).ifaces().size();
        vplsConfig.addInterfaceToVpls(NET1, IF4);

        assertEquals(initialSize + 1, vplsConfig.getVplsWithName(NET1).ifaces().size());
    }

    /**
     * Tests the addition of a new interface when it already exists.
     */
    @Test
    public void testAddExistingInterfaceToNetwork() {
        int initialSize = vplsConfig.getVplsWithName(NET1).ifaces().size();
        vplsConfig.addInterfaceToVpls(NET1, IF1);

        assertEquals(initialSize, vplsConfig.getVplsWithName(NET1).ifaces().size());
    }

    /**
     * Tests the retrieval of a VPLS, given an interface name.
     */
    @Test
    public void testgetNetworkFromInterface() {
        assertNotNull(vplsConfig.getVplsFromInterface(IF1));
        assertNull(vplsConfig.getVplsFromInterface(IF_NON_EXIST));
    }

    /**
     * Tests the removal of an interface.
     */
    @Test
    public void testRemoveExistingInterfaceFromNetwork() {
        int initialSize = vplsConfig.getVplsWithName(NET1).ifaces().size();
        vplsConfig.removeInterfaceFromVpls(initialNetwork, IF1);

        assertEquals(initialSize - 1, vplsConfig.getVplsWithName(NET1).ifaces().size());
    }

    /**
     * Tests the removal of an interface from a VPLS when it does not exist.
     */
    @Test
    public void testRemoveNonExistingInterfaceFromNetwork() {
        int initialSize = vplsConfig.getVplsWithName(NET1).ifaces().size();
        vplsConfig.removeInterfaceFromVpls(initialNetwork, IF_NON_EXIST);

        assertEquals(initialSize, vplsConfig.getVplsWithName(NET1).ifaces().size());
    }

    /**
     * Tests if the two interfaces are attached to the network
     * while one of the interface is attached and another one is not.
     */
    @Test
    public void testIsAttached() {
        VplsNetworkConfig network = createNewNetwork();

        assertTrue(network.isAttached(IF4));
        assertFalse(network.isAttached(IF_NON_EXIST));
    }

    private class MockCfgDelegate implements ConfigApplyDelegate {

        @Override
        public void onApply(@SuppressWarnings("rawtypes") Config config) {
            config.apply();
        }

    }

    private VplsNetworkConfig createInitialNetwork() {
        Set<String> ifaces = new HashSet<>(Arrays.asList(IF1, IF2, IF3));

        return new VplsNetworkConfig(NET1, ifaces);
    }

    private VplsNetworkConfig createNewNetwork() {
        Set<String> ifaces = new HashSet<>(Arrays.asList(IF4, IF5));

        return new VplsNetworkConfig(NEWNET, ifaces);
    }
}
