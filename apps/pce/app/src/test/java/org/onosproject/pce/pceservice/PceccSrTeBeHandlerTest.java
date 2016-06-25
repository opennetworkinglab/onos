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
package org.onosproject.pce.pceservice;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.onosproject.net.Link.Type.DIRECT;
import static org.onosproject.pce.pceservice.PathComputationTest.D1;
import static org.onosproject.pce.pceservice.PathComputationTest.D2;
import static org.onosproject.pce.pceservice.PathComputationTest.D3;
import static org.onosproject.pce.pceservice.PathComputationTest.D4;
import static org.onosproject.pce.pceservice.PathComputationTest.D5;

import java.util.Iterator;
import java.util.List;
import java.util.LinkedList;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onosproject.incubator.net.resource.label.LabelResourceId;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.incubator.net.resource.label.LabelResourceAdminService;
import org.onosproject.incubator.net.resource.label.LabelResourceService;
import org.onosproject.incubator.net.tunnel.LabelStack;
import org.onosproject.net.AnnotationKeys;
import org.onosproject.net.Annotations;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DefaultAnnotations;
import org.onosproject.net.DefaultDevice;
import org.onosproject.net.DefaultPath;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.flowobjective.FlowObjectiveService;
import org.onosproject.net.Path;
import org.onosproject.pce.pceservice.PathComputationTest.MockNetConfigRegistryAdapter;
import org.onosproject.pce.pcestore.api.PceStore;
import org.onosproject.net.provider.ProviderId;
import org.onosproject.pce.util.LabelResourceAdapter;
import org.onosproject.pce.util.PceStoreAdapter;
import org.onosproject.pce.util.MockDeviceService;
import org.onosproject.pcep.api.DeviceCapability;
import org.onosproject.net.DefaultLink;
import org.onosproject.net.Link;

/**
 * Unit tests for PceccSrTeBeHandler class.
 */
public class PceccSrTeBeHandlerTest {

    public static final long GLOBAL_LABEL_SPACE_MIN = 4097;
    public static final long GLOBAL_LABEL_SPACE_MAX = 5121;
    private static final String L3 = "L3";
    private static final String LSRID = "lsrId";

    private PceccSrTeBeHandler srTeHandler;
    private CoreService coreService;
    private LabelResourceAdminService labelRsrcAdminService;
    private LabelResourceService labelRsrcService;
    private PceStore pceStore;
    private MockDeviceService deviceService;
    private FlowObjectiveService flowObjectiveService;
    private MockNetConfigRegistryAdapter netCfgService = new PathComputationTest.MockNetConfigRegistryAdapter();
    private ApplicationId appId;
    private ProviderId providerId;
    private Device deviceD1;
    private Device deviceD2;
    private Device deviceD3;
    private Device deviceD4;
    private Device deviceD5;
    private PortNumber port1;
    private PortNumber port2;
    private PortNumber port3;
    private PortNumber port4;
    private PortNumber port5;
    private Link link1;
    private Link link2;
    private Link link3;
    private Link link4;
    private Path path1;
    LabelResourceId labelId;

    @Before
    public void setUp() throws Exception {
        // Initialization of member variables
        srTeHandler = PceccSrTeBeHandler.getInstance();
        labelRsrcService = new LabelResourceAdapter();
        labelRsrcAdminService = new LabelResourceAdapter();
        flowObjectiveService = new PceManagerTest.MockFlowObjService();
        coreService = new PceManagerTest.MockCoreService();
        appId = coreService.registerApplication("org.onosproject.pce");
        pceStore = new PceStoreAdapter();
        deviceService = new MockDeviceService();
        srTeHandler.initialize(labelRsrcAdminService, labelRsrcService, flowObjectiveService, appId, pceStore,
                               deviceService);

        // Creates path
        // Creates list of links
        providerId = new ProviderId("of", "foo");

        // Devices
        DefaultAnnotations.Builder builderDev1 = DefaultAnnotations.builder();
        DefaultAnnotations.Builder builderDev2 = DefaultAnnotations.builder();
        DefaultAnnotations.Builder builderDev3 = DefaultAnnotations.builder();
        DefaultAnnotations.Builder builderDev4 = DefaultAnnotations.builder();
        DefaultAnnotations.Builder builderDev5 = DefaultAnnotations.builder();

        builderDev1.set(AnnotationKeys.TYPE, L3);
        builderDev1.set(LSRID, "1.1.1.1");

        builderDev2.set(AnnotationKeys.TYPE, L3);
        builderDev2.set(LSRID, "2.2.2.2");

        builderDev3.set(AnnotationKeys.TYPE, L3);
        builderDev3.set(LSRID, "3.3.3.3");

        builderDev4.set(AnnotationKeys.TYPE, L3);
        builderDev4.set(LSRID, "4.4.4.4");

        builderDev5.set(AnnotationKeys.TYPE, L3);
        builderDev5.set(LSRID, "5.5.5.5");

        deviceD1 = new MockDevice(D1.deviceId(), builderDev1.build());
        deviceD2 = new MockDevice(D2.deviceId(), builderDev2.build());
        deviceD3 = new MockDevice(D3.deviceId(), builderDev3.build());
        deviceD4 = new MockDevice(D4.deviceId(), builderDev4.build());
        deviceD5 = new MockDevice(D5.deviceId(), builderDev5.build());

        deviceService.addDevice(deviceD1);
        deviceService.addDevice(deviceD2);
        deviceService.addDevice(deviceD3);
        deviceService.addDevice(deviceD4);
        deviceService.addDevice(deviceD5);

        DeviceCapability device1Cap = netCfgService.addConfig(DeviceId.deviceId("1.1.1.1"), DeviceCapability.class);
        device1Cap.setLabelStackCap(true).setLocalLabelCap(false).setSrCap(true).apply();

        DeviceCapability device2Cap = netCfgService.addConfig(DeviceId.deviceId("2.2.2.2"), DeviceCapability.class);
        device2Cap.setLabelStackCap(true).setLocalLabelCap(false).setSrCap(true).apply();

        DeviceCapability device3Cap = netCfgService.addConfig(DeviceId.deviceId("3.3.3.3"), DeviceCapability.class);
        device3Cap.setLabelStackCap(true).setLocalLabelCap(false).setSrCap(true).apply();

        DeviceCapability device4Cap = netCfgService.addConfig(DeviceId.deviceId("4.4.4.4"), DeviceCapability.class);
        device4Cap.setLabelStackCap(true).setLocalLabelCap(false).setSrCap(true).apply();

        DeviceCapability device5Cap = netCfgService.addConfig(DeviceId.deviceId("5.5.5.5"), DeviceCapability.class);
        device5Cap.setLabelStackCap(true).setLocalLabelCap(false).setSrCap(true).apply();

        // Port Numbers
        port1 = PortNumber.portNumber(1);
        port2 = PortNumber.portNumber(2);
        port3 = PortNumber.portNumber(3);
        port4 = PortNumber.portNumber(4);
        port5 = PortNumber.portNumber(5);
        List<Link> linkList = new LinkedList<>();

        link1 = DefaultLink.builder().providerId(providerId)
                .annotations(DefaultAnnotations.builder().set("key1", "yahoo").build())
                .src(new ConnectPoint(deviceD1.id(), port1)).dst(new ConnectPoint(deviceD2.id(), port2)).type(DIRECT)
                .state(Link.State.ACTIVE).build();
        linkList.add(link1);
        link2 = DefaultLink.builder().providerId(providerId)
                .annotations(DefaultAnnotations.builder().set("key2", "yahoo").build())
                .src(new ConnectPoint(deviceD2.id(), port2)).dst(new ConnectPoint(deviceD3.id(), port3)).type(DIRECT)
                .state(Link.State.ACTIVE).build();
        linkList.add(link2);
        link3 = DefaultLink.builder().providerId(providerId)
                .annotations(DefaultAnnotations.builder().set("key3", "yahoo").build())
                .src(new ConnectPoint(deviceD3.id(), port3)).dst(new ConnectPoint(deviceD4.id(), port4)).type(DIRECT)
                .state(Link.State.ACTIVE).build();
        linkList.add(link3);
        link4 = DefaultLink.builder().providerId(providerId)
                .annotations(DefaultAnnotations.builder().set("key4", "yahoo").build())
                .src(new ConnectPoint(deviceD4.id(), port4)).dst(new ConnectPoint(deviceD5.id(), port5)).type(DIRECT)
                .state(Link.State.ACTIVE).build();
        linkList.add(link4);

        // Path
        path1 = new DefaultPath(providerId, linkList, 10);
    }

    @After
    public void tearDown() throws Exception {
        PceManagerTest.flowsDownloaded = 0;
    }

    /**
     * Checks the operation of getInstance() method.
     */
    @Test
    public void testGetInstance() {
        assertThat(srTeHandler, is(notNullValue()));
    }

    /**
     * Checks the operation of reserveGlobalPool() method.
     */
    @Test
    public void testReserveGlobalPool() {
        assertThat(srTeHandler.reserveGlobalPool(GLOBAL_LABEL_SPACE_MIN, GLOBAL_LABEL_SPACE_MAX), is(true));
    }

    /**
     * Checks the operation of allocateNodeLabel() method on node label.
     */
    @Test
    public void testAllocateNodeLabel() {
        // Specific device D1.deviceId

        //device 1
        String lsrId1 = "11.1.1.1";
        // Allocate node label for specific device D1deviceId
        assertThat(srTeHandler.allocateNodeLabel(D1.deviceId(), lsrId1), is(true));
        // Retrieve label from store
        LabelResourceId labelId = pceStore.getGlobalNodeLabel(D1.deviceId());
        // Check whether label is generated for this device D1.deviceId()
        assertThat(labelId, is(notNullValue()));

        // device 2
        String lsrId2 = "12.1.1.1";
        // Allocate node label for specific device D2.deviceId()
        assertThat(srTeHandler.allocateNodeLabel(D2.deviceId(), lsrId2), is(true));
        // Retrieve label from store
        labelId = pceStore.getGlobalNodeLabel(D2.deviceId());
        // Check whether label is generated for this device D2.deviceId()
        assertThat(labelId, is(notNullValue()));

        // device 3
        String lsrId3 = "13.1.1.1";
        // Allocate node label for specific device D3.deviceId()
        assertThat(srTeHandler.allocateNodeLabel(D3.deviceId(), lsrId3), is(true));
        // Retrieve label from store
        labelId = pceStore.getGlobalNodeLabel(D3.deviceId());
        // Check whether label is generated for this device D3.deviceId()
        assertThat(labelId, is(notNullValue()));

        // device 4
        String lsrId4 = "14.1.1.1";
        // Allocate node label for specific device D4.deviceId()
        assertThat(srTeHandler.allocateNodeLabel(D4.deviceId(), lsrId4), is(true));
        // Retrieve label from store
        labelId = pceStore.getGlobalNodeLabel(D4.deviceId());
        // Check whether label is generated for this device D4.deviceId()
        assertThat(labelId, is(notNullValue()));

        // device 5
        String lsrId5 = "15.1.1.1";
        // Allocate node label for specific device D5.deviceId()
        assertThat(srTeHandler.allocateNodeLabel(D5.deviceId(), lsrId5), is(true));
        // Retrieve label from store
        labelId = pceStore.getGlobalNodeLabel(D5.deviceId());
        // Check whether label is generated for this device D5.deviceId()
        assertThat(labelId, is(notNullValue()));
    }

    /**
     * Checks the operation of releaseNodeLabel() method on node label.
     */
    @Test
    public void testReleaseNodeLabelSuccess() {
        testAllocateNodeLabel();
        // Specific device D1.deviceId()

        //device 1
        String lsrId1 = "11.1.1.1";
        // Check whether successfully released node label
        assertThat(srTeHandler.releaseNodeLabel(D1.deviceId(), lsrId1), is(true));
        // Check whether successfully removed label from store
        LabelResourceId labelId = pceStore.getGlobalNodeLabel(D1.deviceId());
        assertThat(labelId, is(nullValue()));

        //device 2
        String lsrId2 = "12.1.1.1";
        // Check whether successfully released node label
        assertThat(srTeHandler.releaseNodeLabel(D2.deviceId(), lsrId2), is(true));
        // Check whether successfully removed label from store
        labelId = pceStore.getGlobalNodeLabel(D2.deviceId());
        assertThat(labelId, is(nullValue()));

        //device 3
        String lsrId3 = "13.1.1.1";
        // Check whether successfully released node label
        assertThat(srTeHandler.releaseNodeLabel(D3.deviceId(), lsrId3), is(true));
        // Check whether successfully removed label from store
        labelId = pceStore.getGlobalNodeLabel(D3.deviceId());
        assertThat(labelId, is(nullValue()));

        //device 4
        String lsrId4 = "14.1.1.1";
        // Check whether successfully released node label
        assertThat(srTeHandler.releaseNodeLabel(D4.deviceId(), lsrId4), is(true));
        // Check whether successfully removed label from store
        labelId = pceStore.getGlobalNodeLabel(D4.deviceId());
        assertThat(labelId, is(nullValue()));

        //device 5
        String lsrId5 = "15.1.1.1";
        // Check whether successfully released node label
        assertThat(srTeHandler.releaseNodeLabel(D5.deviceId(), lsrId5), is(true));
        // Check whether successfully removed label from store
        labelId = pceStore.getGlobalNodeLabel(D5.deviceId());
        assertThat(labelId, is(nullValue()));
    }

    @Test
    public void testReleaseNodeLabelFailure() {
        testAllocateNodeLabel();

        //device 6
        String lsrId6 = "16.1.1.1";
        // Check whether successfully released node label
        DeviceId deviceId6 = DeviceId.deviceId("foo6");
        assertThat(srTeHandler.releaseNodeLabel(deviceId6, lsrId6), is(false));
    }

    /**
     * Checks the operation of allocateAdjacencyLabel() method on adjacency label.
     */
    @Test
    public void testAllocateAdjacencyLabel() {
        // test link1
        // Check whether adjacency label is allocated successfully.
        assertThat(srTeHandler.allocateAdjacencyLabel(link1), is(true));
        // Retrieve from store and check whether adjacency label is generated successfully for this device.
        LabelResourceId labelId = pceStore.getAdjLabel(link1);
        assertThat(labelId, is(notNullValue()));

        // test link2
        // Check whether adjacency label is allocated successfully.
        assertThat(srTeHandler.allocateAdjacencyLabel(link2), is(true));
        // Retrieve from store and check whether adjacency label is generated successfully for this device.
        labelId = pceStore.getAdjLabel(link2);
        assertThat(labelId, is(notNullValue()));

        // test link3
        // Check whether adjacency label is allocated successfully.
        assertThat(srTeHandler.allocateAdjacencyLabel(link3), is(true));
        // Retrieve from store and check whether adjacency label is generated successfully for this device.
        labelId = pceStore.getAdjLabel(link3);
        assertThat(labelId, is(notNullValue()));

        // test link4
        // Check whether adjacency label is allocated successfully.
        assertThat(srTeHandler.allocateAdjacencyLabel(link4), is(true));
        // Retrieve from store and check whether adjacency label is generated successfully for this device.
        labelId = pceStore.getAdjLabel(link4);
        assertThat(labelId, is(notNullValue()));
    }

    /**
     * Checks the operation of releaseAdjacencyLabel() method on adjacency label.
     */
    @Test
    public void testReleaseAdjacencyLabel() {
        // Test link1
        // Check whether adjacency label is released successfully.
        assertThat(srTeHandler.allocateAdjacencyLabel(link1), is(true));
        assertThat(srTeHandler.releaseAdjacencyLabel(link1), is(true));
        // Retrieve from store and check whether adjacency label is removed successfully for this device.
        LabelResourceId labelId = pceStore.getAdjLabel(link1);
        assertThat(labelId, is(nullValue()));

        // Test link2
        // Check whether adjacency label is released successfully.
        assertThat(srTeHandler.allocateAdjacencyLabel(link2), is(true));
        assertThat(srTeHandler.releaseAdjacencyLabel(link2), is(true));
        // Retrieve from store and check whether adjacency label is removed successfully for this device.
        labelId = pceStore.getAdjLabel(link2);
        assertThat(labelId, is(nullValue()));
    }

    /**
     * Checks the operation of computeLabelStack() method.
     */
    @Test
    public void testComputeLabelStack() {
        // Allocate node labels to each devices
        labelId = LabelResourceId.labelResourceId(4097);
        pceStore.addGlobalNodeLabel(D1.deviceId(), labelId);
        labelId = LabelResourceId.labelResourceId(4098);
        pceStore.addGlobalNodeLabel(D2.deviceId(), labelId);
        labelId = LabelResourceId.labelResourceId(4099);
        pceStore.addGlobalNodeLabel(D3.deviceId(), labelId);
        labelId = LabelResourceId.labelResourceId(4100);
        pceStore.addGlobalNodeLabel(D4.deviceId(), labelId);
        labelId = LabelResourceId.labelResourceId(4101);
        pceStore.addGlobalNodeLabel(D5.deviceId(), labelId);

        // Allocate adjacency labels to each devices
        labelId = LabelResourceId.labelResourceId(5122);
        pceStore.addAdjLabel(link1, labelId);
        labelId = LabelResourceId.labelResourceId(5123);
        pceStore.addAdjLabel(link2, labelId);
        labelId = LabelResourceId.labelResourceId(5124);
        pceStore.addAdjLabel(link3, labelId);
        labelId = LabelResourceId.labelResourceId(5125);
        pceStore.addAdjLabel(link4, labelId);

        // Compute label stack
        LabelStack labelStack = srTeHandler.computeLabelStack(path1);

        List<LabelResourceId> labelList = labelStack.labelResources();
        Iterator<LabelResourceId> iterator = labelList.iterator();

        // check adjacency label of D1.deviceId()
        labelId = iterator.next();
        assertThat(labelId, is(LabelResourceId.labelResourceId(5122)));

        // check node-label of D2.deviceId()
        labelId = iterator.next();
        assertThat(labelId, is(LabelResourceId.labelResourceId(4098)));

        // check adjacency label of D2.deviceId()
        labelId = iterator.next();
        assertThat(labelId, is(LabelResourceId.labelResourceId(5123)));

        // check node-label of D3.deviceId()
        labelId = iterator.next();
        assertThat(labelId, is(LabelResourceId.labelResourceId(4099)));

        // check adjacency label of D3.deviceId()
        labelId = iterator.next();
        assertThat(labelId, is(LabelResourceId.labelResourceId(5124)));

        // check node-label of D4.deviceId()
        labelId = iterator.next();
        assertThat(labelId, is(LabelResourceId.labelResourceId(4100)));

        // check adjacency label of D4.deviceId()
        labelId = iterator.next();
        assertThat(labelId, is(LabelResourceId.labelResourceId(5125)));

        // check node-label of D5.deviceId()
        labelId = iterator.next();
        assertThat(labelId, is(LabelResourceId.labelResourceId(4101)));
    }

    private class MockDevice extends DefaultDevice {
        MockDevice(DeviceId id, Annotations annotations) {
            super(null, id, null, null, null, null, null, null, annotations);
        }
    }
}
