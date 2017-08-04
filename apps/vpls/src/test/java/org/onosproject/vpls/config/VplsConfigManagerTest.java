/*
 * Copyright 2017-present Open Networking Foundation
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
import com.google.common.collect.ImmutableSet;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onosproject.net.EncapsulationType;
import org.onosproject.net.config.ConfigApplyDelegate;
import org.onosproject.net.config.NetworkConfigEvent;
import org.onosproject.net.config.NetworkConfigRegistryAdapter;
import org.onosproject.vpls.VplsTest;
import org.onosproject.vpls.api.VplsData;

import java.io.IOException;
import java.util.Collection;

import static org.junit.Assert.*;
public class VplsConfigManagerTest extends VplsTest {
    VplsConfigManager vplsConfigManager;

    @Before
    public void setup() {
        vplsConfigManager = new VplsConfigManager();
        vplsConfigManager.configService = new TestConfigService();
        vplsConfigManager.coreService = new TestCoreService();
        vplsConfigManager.interfaceService = new TestInterfaceService();
        vplsConfigManager.registry = new NetworkConfigRegistryAdapter();
        vplsConfigManager.vpls = new TestVpls();
        vplsConfigManager.leadershipService = new TestLeadershipService();

        vplsConfigManager.activate();
    }

    @After
    public void tearDown() {
        vplsConfigManager.deactivate();
    }

    /**
     * Reloads network configuration by sending a network config event.
     */
    @Test
    public void testReloadConfig() {
        NetworkConfigEvent event = new NetworkConfigEvent(NetworkConfigEvent.Type.CONFIG_ADDED,
                                                          null,
                                                          VplsAppConfig.class);
        ((TestConfigService) vplsConfigManager.configService).sendEvent(event);

        Collection<VplsData> vplss = vplsConfigManager.vpls.getAllVpls();
        assertEquals(2, vplss.size());

        VplsData expect = VplsData.of(VPLS1);
        expect.addInterfaces(ImmutableSet.of(V100H1, V100H2));
        expect.state(VplsData.VplsState.ADDED);
        assertTrue(vplss.contains(expect));

        expect = VplsData.of(VPLS2, EncapsulationType.VLAN);
        expect.addInterfaces(ImmutableSet.of(V200H1, V200H2));
        expect.state(VplsData.VplsState.ADDED);
        System.out.println(vplss);
        assertTrue(vplss.contains(expect));
    }

    /**
     * Sends a network config event with null network config.
     */
    @Test
    public void testReloadNullConfig() {
        ((TestConfigService) vplsConfigManager.configService).setConfig(null);
        NetworkConfigEvent event = new NetworkConfigEvent(NetworkConfigEvent.Type.CONFIG_ADDED,
                                                          null,
                                                          VplsAppConfig.class);
        ((TestConfigService) vplsConfigManager.configService).sendEvent(event);

        Collection<VplsData> vplss = vplsConfigManager.vpls.getAllVpls();
        assertEquals(0, vplss.size());
    }

    /**
     * Updates VPLSs by sending new VPLS config.
     */
    @Test
    public void testReloadConfigUpdateVpls() {
        ((TestVpls) vplsConfigManager.vpls).initSampleData();

        VplsAppConfig vplsAppConfig = new VplsAppConfig();
        final ObjectMapper mapper = new ObjectMapper();
        final ConfigApplyDelegate delegate = new VplsAppConfigTest.MockCfgDelegate();
        JsonNode tree = null;
        try {
            tree = new ObjectMapper().readTree(TestConfigService.EMPTY_JSON_TREE);
        } catch (IOException e) {
            e.printStackTrace();
        }
        vplsAppConfig.init(APPID, APP_NAME, tree, mapper, delegate);
        VplsConfig vplsConfig = new VplsConfig(VPLS1,
                                               ImmutableSet.of(V100H1.name()),
                                               EncapsulationType.MPLS);
        vplsAppConfig.addVpls(vplsConfig);
        ((TestConfigService) vplsConfigManager.configService).setConfig(vplsAppConfig);
        NetworkConfigEvent event = new NetworkConfigEvent(NetworkConfigEvent.Type.CONFIG_ADDED,
                                                          null,
                                                          VplsAppConfig.class);
        ((TestConfigService) vplsConfigManager.configService).sendEvent(event);

        Collection<VplsData> vplss = vplsConfigManager.vpls.getAllVpls();
        assertEquals(1, vplss.size());

        VplsData expect = VplsData.of(VPLS1, EncapsulationType.MPLS);
        expect.addInterfaces(ImmutableSet.of(V100H1));
        expect.state(VplsData.VplsState.ADDED);
        assertTrue(vplss.contains(expect));
    }

    /**
     * Remvoes all VPLS by sending CONFIG_REMOVED event.
     */
    @Test
    public void testRemoveConfig() {
        NetworkConfigEvent event = new NetworkConfigEvent(NetworkConfigEvent.Type.CONFIG_REMOVED,
                                                          null,
                                                          VplsAppConfig.class);
        ((TestConfigService) vplsConfigManager.configService).sendEvent(event);

        Collection<VplsData> vplss = vplsConfigManager.vpls.getAllVpls();
        assertEquals(0, vplss.size());
    }
}
