/*
 * Copyright 2020-present Open Networking Foundation
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
package org.onosproject.inbandtelemetry.impl;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.google.common.collect.ImmutableList;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onlab.packet.IpAddress;
import org.onlab.packet.TpPort;
import org.onosproject.TestApplicationId;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.mastership.MastershipService;
import org.onosproject.net.behaviour.inbandtelemetry.IntReportConfig;
import org.onosproject.net.behaviour.inbandtelemetry.IntDeviceConfig;
import org.onosproject.net.config.ConfigApplyDelegate;
import org.onosproject.net.config.NetworkConfigEvent;
import org.onosproject.net.config.NetworkConfigListener;
import org.onosproject.net.config.NetworkConfigRegistry;
import org.onosproject.net.config.NetworkConfigService;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.host.HostService;
import org.onosproject.store.service.StorageService;
import org.onosproject.store.service.TestStorageService;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.newCapture;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;
import static org.onlab.junit.TestUtils.setField;

public class SimpleIntManagerTest {
    private static final String APP_NAME = "org.onosproject.inbandtelemetry";
    private static final ApplicationId APP_ID = new TestApplicationId(APP_NAME);
    private static final IpAddress COLLECTOR_IP = IpAddress.valueOf("10.0.0.1");
    private static final TpPort COLLECTOR_PORT = TpPort.tpPort(5500);
    private static final int MIN_FLOW_HOP_LATENCY_CHANGE_NS = 16;

    private SimpleIntManager manager;
    private StorageService storageService;
    private MastershipService mastershipService;
    private CoreService coreService;
    private HostService hostService;
    private DeviceService deviceService;
    private NetworkConfigRegistry networkConfigRegistry;
    private NetworkConfigService networkConfigService;
    private NetworkConfigListener networkConfigListener;


    @Before
    public void setup() {
        storageService = new TestStorageService();
        mastershipService = createNiceMock(MastershipService.class);
        coreService = createNiceMock(CoreService.class);
        hostService = createNiceMock(HostService.class);
        deviceService = createNiceMock(DeviceService.class);
        expect(deviceService.getDevices()).andReturn(ImmutableList.of()).anyTimes();
        networkConfigRegistry = createNiceMock(NetworkConfigRegistry.class);
        networkConfigService = createNiceMock(NetworkConfigService.class);

        manager = new SimpleIntManager();
        setField(manager, "coreService", coreService);
        setField(manager, "deviceService", deviceService);
        setField(manager, "storageService", storageService);
        setField(manager, "mastershipService", mastershipService);
        setField(manager, "hostService", hostService);
        setField(manager, "netcfgService", networkConfigService);
        setField(manager, "netcfgRegistry", networkConfigRegistry);

        expect(coreService.registerApplication(APP_NAME)).andReturn(APP_ID).once();
        networkConfigRegistry.registerConfigFactory(anyObject());
        expectLastCall().once();

        Capture<NetworkConfigListener> capture = newCapture();
        networkConfigService.addListener(EasyMock.capture(capture));
        expectLastCall().once();
        replay(mastershipService, deviceService, coreService,
                hostService, networkConfigRegistry, networkConfigService);
        manager.activate();
        networkConfigListener = capture.getValue();
    }

    @After
    public void teardown() {
        manager.deactivate();
    }

    @Test
    public void testPushIntAppConfig() {
        IntReportConfig config = new IntReportConfig();
        ObjectMapper mapper = new ObjectMapper();
        ConfigApplyDelegate delegate = configApply -> { };
        config.init(APP_ID, "report", JsonNodeFactory.instance.objectNode(), mapper, delegate);
        config.setCollectorIp(COLLECTOR_IP)
                .setCollectorPort(COLLECTOR_PORT)
                .setMinFlowHopLatencyChangeNs(MIN_FLOW_HOP_LATENCY_CHANGE_NS);

        NetworkConfigEvent event =
                new NetworkConfigEvent(NetworkConfigEvent.Type.CONFIG_ADDED, APP_ID,
                        config, null, IntReportConfig.class);
        networkConfigListener.event(event);

        // We expected that the manager will store the device config which
        // converted from the app config.
        IntDeviceConfig deviceConfig = manager.getConfig();
        assertEquals(COLLECTOR_IP, deviceConfig.collectorIp());
        assertEquals(COLLECTOR_PORT, deviceConfig.collectorPort());
        assertEquals(MIN_FLOW_HOP_LATENCY_CHANGE_NS, deviceConfig.minFlowHopLatencyChangeNs());
    }
}
