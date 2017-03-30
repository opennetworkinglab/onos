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
import org.onosproject.net.EncapsulationType;
import org.onosproject.net.config.Config;
import org.onosproject.net.config.ConfigApplyDelegate;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.*;

/**
 * Tests for the {@link VplsAppConfig} class.
 */
public class VplsAppConfigTest {
    private static final String APP_NAME = "org.onosproject.vpls";
    private static final ApplicationId APP_ID = new TestApplicationId(APP_NAME);
    private static final String VPLS = "vplsList";
    private static final String NAME = "name";
    private static final String INTERFACE = "interfaces";
    private static final String ENCAPSULATION = "encapsulation";
    private static final String VPLS1 = "vpls1";
    private static final String VPLS2 = "vpls2";
    private static final String NEWVPLS = "newvpls";

    private static final String IF1 = "sw5-4-100";
    private static final String IF2 = "sw5-4-200";
    private static final String IF3 = "sw5-5-100";
    private static final String IF4 = "sw6-5-100";
    private static final String IF5 = "sw6-5-400";
    private static final String IF_NON_EXIST = "sw7-5-100";

    private static final String JSON_TREE = "{\"" + VPLS + "\" : [{\"" + NAME +
            "\" : \"" + VPLS1 + "\"," + "\"" + INTERFACE + "\" : [" + "\"" +
            IF1 + "\",\"" + IF2 + "\",\"" + IF3 + "\"]" + ",\"" +
            ENCAPSULATION + "\" : \"none\"}]}";

    private static final String EMPTY_JSON_TREE = "{}";

    private final ObjectMapper mapper = new ObjectMapper();
    private final ConfigApplyDelegate delegate = new MockCfgDelegate();
    private final VplsConfig initialVpls = createInitialVpls();

    private Set<VplsConfig> vplss = new HashSet<>();
    private VplsAppConfig vplsAppConfig = new VplsAppConfig();
    private VplsAppConfig emptyVplsAppConfig = new VplsAppConfig();

    @Before
    public void setUp() throws Exception {
        JsonNode tree = new ObjectMapper().readTree(JSON_TREE);
        vplsAppConfig.init(APP_ID, APP_NAME, tree, mapper, delegate);
        JsonNode emptyTree = new ObjectMapper().readTree(EMPTY_JSON_TREE);
        emptyVplsAppConfig.init(APP_ID, APP_NAME, emptyTree, mapper, delegate);
        vplss.add(initialVpls);
    }

    /**
     * Tests if a VPLS configuration can be retrieved from JSON.
     */
    @Test
    public void vplss() {
        assertEquals("Cannot load VPLS configuration or unexpected configuration" +
                             "loaded", vplss, vplsAppConfig.vplss());
    }

    /**
     * Tests an empty VPLS application configuration is retrieved from JSON.
     */
    @Test
    public void emptyVplss() {
        assertTrue("Configuration retrieved from JSON was not empty",
                   emptyVplsAppConfig.vplss().isEmpty());
    }

    /**
     * Tests if a VPLS can be found by name. Tries also to find a VPLS that
     * does not exist in the configuration.
     */
    @Test
    public void getVplsWithName() {
        assertNotNull("Configuration for VPLS not found",
                      vplsAppConfig.getVplsWithName(VPLS1));
        assertNull("Unexpected configuration for VPLS found",
                   vplsAppConfig.getVplsWithName(VPLS2));
    }

    /**
     * Tests the addition of a new VPLS.
     */
    @Test
    public void addVpls() {
        int initialSize = vplsAppConfig.vplss().size();
        VplsConfig newVpls = createNewVpls();
        vplsAppConfig.addVpls(newVpls);
        assertEquals("The new VPLS has not been added correctly to the list of" +
                             "existing VPLSs",
                     initialSize + 1,
                     vplsAppConfig.vplss().size());
        vplss.add(newVpls);
    }

    /**
     * Tests the addition of new VPLS to an empty configuration.
     */
    @Test
    public void addVplsToEmpty() {
        VplsConfig newVpls = createNewVpls();
        emptyVplsAppConfig.addVpls(newVpls);

        assertFalse("The new VPLS has not been added correctly",
                    emptyVplsAppConfig.vplss().isEmpty());
    }

    /**
     * Tests the removal of an existing VPLS from the configuration.
     */
    @Test
    public void removeExistingVpls() {
        int initialSize = vplsAppConfig.vplss().size();
        vplsAppConfig.removeVpls(VPLS1);

        assertEquals("The VPLS has not been removed correctly",
                     initialSize - 1, vplsAppConfig.vplss().size());
    }

    /**
     * Tests the removal of a non-existing VPLS from the configuration.
     */
    @Test
    public void removeInexistingVpls() {
        int initialSize = vplsAppConfig.vplss().size();
        vplsAppConfig.removeVpls(VPLS2);

        assertEquals("Non-configured VPLS has been unexpectedly removed",
                     initialSize, vplsAppConfig.vplss().size());
    }

    /**
     * Tests the addition of a new interface.
     */
    @Test
    public void addInterfaceToVpls() {
        int initialSize = vplsAppConfig.getVplsWithName(VPLS1).ifaces().size();
        vplsAppConfig.addIface(VPLS1, IF4);

        assertEquals("The interface has not been added to the VPLS",
                     initialSize + 1,
                     vplsAppConfig.getVplsWithName(VPLS1).ifaces().size());
    }

    /**
     * Tests the addition of a new interface when it already exists.
     */
    @Test
    public void addExistingInterfaceToVpls() {
        int initialSize = vplsAppConfig.getVplsWithName(VPLS1).ifaces().size();
        vplsAppConfig.addIface(VPLS1, IF1);

        assertEquals("The interface has been unexpectedly added twice to the" +
                             "same VPLS",
                     initialSize,
                     vplsAppConfig.getVplsWithName(VPLS1).ifaces().size());
    }

    /**
     * Tests the retrieval of a VPLS, given an interface name.
     */
    @Test
    public void getVplsFromInterface() {
        assertNotNull("VPLS not found", vplsAppConfig.vplsFromIface(IF1));
        assertNull("VPLS unexpectedly found",
                   vplsAppConfig.vplsFromIface(IF_NON_EXIST));
    }

    /**
     * Tests the removal of an interface.
     */
    @Test
    public void removeExistingInterfaceFromVpls() {
        int initialSize = vplsAppConfig.getVplsWithName(VPLS1).ifaces().size();
        vplsAppConfig.removeIface(initialVpls, IF1);

        assertEquals("Interface has not been removed correctly from the VPLS",
                     initialSize - 1,
                     vplsAppConfig.getVplsWithName(VPLS1).ifaces().size());
    }

    /**
     * Tests the removal of an interface from a VPLS when it does not exist.
     */
    @Test
    public void removeNonExistingInterfaceFromVpls() {
        int initialSize = vplsAppConfig.getVplsWithName(VPLS1).ifaces().size();
        vplsAppConfig.removeIface(initialVpls, IF_NON_EXIST);

        assertEquals("Interface unexpectedly removed from the VPLS",
                     initialSize, vplsAppConfig.getVplsWithName(VPLS1).ifaces().size());
    }

    /**
     * Tests if the two interfaces are attached to the VPLS, while one of them
     * is also attached and another.
     */
    @Test
    public void isAttached() {
        VplsConfig vpls = createNewVpls();

        assertTrue("Interface not correctly attached to the VPLS",
                   vpls.isAttached(IF4));
        assertTrue("Interface not correctly attached to the VPLS",
                   vpls.isAttached(IF4));
        assertFalse("Unexpected interface attached to the VPLS",
                    vpls.isAttached(IF_NON_EXIST));
    }

    /**
     * Tests if encapsulation is set correctly.
     */
    @Test
    public void encap() {
        vplsAppConfig.setEncap(VPLS1, EncapsulationType.VLAN);
        assertEquals("Wrong encapsulation type found",
                     EncapsulationType.VLAN,
                     vplsAppConfig.getVplsWithName(VPLS1).encap());
    }

    public static class MockCfgDelegate implements ConfigApplyDelegate {
        @Override
        public void onApply(@SuppressWarnings("rawtypes") Config config) {
            config.apply();
        }
    }

    /**
     * Creates a basic VPLS config.
     *
     * @return the VPLS config
     */
    private VplsConfig createInitialVpls() {
        Set<String> ifaces = new HashSet<>(Arrays.asList(IF1, IF2, IF3));
        return new VplsConfig(VPLS1, ifaces, EncapsulationType.NONE);
    }

    /**
     * Creates another basic VPLS config.
     *
     * @return the VPLS config
     */
    private VplsConfig createNewVpls() {
        Set<String> ifaces = new HashSet<>(Arrays.asList(IF4, IF5));
        return new VplsConfig(NEWVPLS, ifaces, EncapsulationType.NONE);
    }
}
