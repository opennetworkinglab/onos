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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
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
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DefaultAnnotations;
import org.onosproject.net.DefaultPath;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.flowobjective.FlowObjectiveService;
import org.onosproject.net.Path;
import org.onosproject.pce.pcestore.api.PceStore;
import org.onosproject.net.provider.ProviderId;
import org.onosproject.pce.util.LabelResourceAdapter;
import org.onosproject.pce.util.PceStoreAdapter;
import org.onosproject.net.DefaultLink;
import org.onosproject.net.Link;

/**
 * Unit tests for PceccSrTeBeHandler class.
 */
public class PceccSrTeBeHandlerTest {

    public static final long GLOBAL_LABEL_SPACE_MIN = 4097;
    public static final long GLOBAL_LABEL_SPACE_MAX = 5121;

    private PceccSrTeBeHandler srTeHandler;
    protected LabelResourceAdminService labelRsrcAdminService;
    protected LabelResourceService labelRsrcService;
    protected PceStore pceStore;
    private FlowObjectiveService flowObjectiveService;
    private CoreService coreService;
    private ApplicationId appId;
    private ProviderId providerId;
    private DeviceId deviceId1;
    private DeviceId deviceId2;
    private DeviceId deviceId3;
    private DeviceId deviceId4;
    private DeviceId deviceId5;
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
    private LabelResourceId labelId1 = LabelResourceId.labelResourceId(4098);
    private LabelResourceId labelId2 = LabelResourceId.labelResourceId(4099);
    private Map<DeviceId, String> deviceIdLsrIdMap;

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
        srTeHandler.initialize(labelRsrcAdminService, labelRsrcService, flowObjectiveService, appId, pceStore);

        // Creates path
        // Creates list of links
        providerId = new ProviderId("of", "foo");
        deviceId1 = DeviceId.deviceId("of:A");
        deviceId2 = DeviceId.deviceId("of:B");
        deviceId3 = DeviceId.deviceId("of:C");
        deviceId4 = DeviceId.deviceId("of:D");
        deviceId5 = DeviceId.deviceId("of:E");
        port1 = PortNumber.portNumber(1);
        port2 = PortNumber.portNumber(2);
        port3 = PortNumber.portNumber(3);
        port4 = PortNumber.portNumber(4);
        port5 = PortNumber.portNumber(5);
        List<Link> linkList = new LinkedList<>();

        link1 = DefaultLink.builder()
                .providerId(providerId)
                .annotations(DefaultAnnotations.builder().set("key1", "yahoo").build())
                .src(new ConnectPoint(deviceId1, port1))
                .dst(new ConnectPoint(deviceId2, port2))
                .type(DIRECT)
                .state(Link.State.ACTIVE)
                .build();
        linkList.add(link1);
        link2 = DefaultLink.builder()
                .providerId(providerId)
                .annotations(DefaultAnnotations.builder().set("key2", "yahoo").build())
                .src(new ConnectPoint(deviceId2, port2))
                .dst(new ConnectPoint(deviceId3, port3))
                .type(DIRECT)
                .state(Link.State.ACTIVE)
                .build();
        linkList.add(link2);
        link3 = DefaultLink.builder()
                .providerId(providerId)
                .annotations(DefaultAnnotations.builder().set("key3", "yahoo").build())
                .src(new ConnectPoint(deviceId3, port3))
                .dst(new ConnectPoint(deviceId4, port4))
                .type(DIRECT)
                .state(Link.State.ACTIVE)
                .build();
        linkList.add(link3);
        link4 = DefaultLink.builder()
                .providerId(providerId)
                .annotations(DefaultAnnotations.builder().set("key4", "yahoo").build())
                .src(new ConnectPoint(deviceId4, port4))
                .dst(new ConnectPoint(deviceId5, port5))
                .type(DIRECT)
                .state(Link.State.ACTIVE)
                .build();
        linkList.add(link4);

        // Path
        path1 = new DefaultPath(providerId, linkList, 10);
    }

    @After
    public void tearDown() throws Exception {
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
        assertThat(srTeHandler.reserveGlobalPool(GLOBAL_LABEL_SPACE_MIN,
                GLOBAL_LABEL_SPACE_MAX), is(true));
    }

    /**
     * Checks the operation of allocateNodeLabel() method on node label.
     * Considered some nodes are already available before PceManager came up.
     */
    @Test
    public void testAllocateNodeLabel1() {
        // Specific device deviceId1

        // Devices mapping with lsr-id
        deviceIdLsrIdMap = new HashMap<>();

        //device 1
        String lsrId1 = "11.1.1.1";
        deviceIdLsrIdMap.put(deviceId1, lsrId1);

        // device 2
        String lsrId2 = "12.1.1.1";
        deviceIdLsrIdMap.put(deviceId2, lsrId2);

        // device 3
        String lsrId3 = "13.1.1.1";
        deviceIdLsrIdMap.put(deviceId3, lsrId3);

        // device 4
        String lsrId4 = "14.1.1.1";
        deviceIdLsrIdMap.put(deviceId4, lsrId4);

        // device 5
        String lsrId5 = "15.1.1.1";
        deviceIdLsrIdMap.put(deviceId5, lsrId5);

        // Considered devices are stored in deviceIdLsrIdMap.
        // Creating temporary tempDeviceIdLsrIdMap to pass to allocateNodeLabel()
        Map<DeviceId, String> tempDeviceIdLsrIdMap = new HashMap<>();
        for (Map.Entry element : deviceIdLsrIdMap.entrySet()) {
            DeviceId devId = (DeviceId) element.getKey();
            String lsrId = (String) element.getValue();

            // Allocate node label for specific device devId
            assertThat(srTeHandler.allocateNodeLabel(devId, lsrId, tempDeviceIdLsrIdMap), is(true));

            // Retrieve label from store
            LabelResourceId labelId = pceStore.getGlobalNodeLabel(devId);

            // Check whether label is generated for this device devId
            assertThat(labelId, is(notNullValue()));

            //Now add device to tempDeviceIdLsrIdMap
            tempDeviceIdLsrIdMap.put(devId, lsrId);
        }
    }

    /**
     * Checks the operation of allocateNodeLabel() method on node label.
     * Considered initially map is empty and keep on getting new devices from event.
     */
    @Test
    public void testAllocateNodeLabel2() {
        // Specific device deviceId1

        // Device-id mapping with lsr-id
        deviceIdLsrIdMap = new HashMap<>();

        //device 1
        String lsrId1 = "11.1.1.1";
        // Allocate node label for specific device deviceId1
        assertThat(srTeHandler.allocateNodeLabel(deviceId1, lsrId1, deviceIdLsrIdMap), is(true));
        // Retrieve label from store
        LabelResourceId labelId = pceStore.getGlobalNodeLabel(deviceId1);
        // Check whether label is generated for this device deviceId1
        assertThat(labelId, is(notNullValue()));
        //Now add device to deviceIdLsrIdMap
        deviceIdLsrIdMap.put(deviceId1, lsrId1);

        // device 2
        String lsrId2 = "12.1.1.1";
        // Allocate node label for specific device deviceId2
        assertThat(srTeHandler.allocateNodeLabel(deviceId2, lsrId2, deviceIdLsrIdMap), is(true));
        // Retrieve label from store
        labelId = pceStore.getGlobalNodeLabel(deviceId2);
        // Check whether label is generated for this device deviceId2
        assertThat(labelId, is(notNullValue()));
        //Now add device to deviceIdLsrIdMap
        deviceIdLsrIdMap.put(deviceId2, lsrId2);

        // device 3
        String lsrId3 = "13.1.1.1";
        // Allocate node label for specific device deviceId3
        assertThat(srTeHandler.allocateNodeLabel(deviceId3, lsrId3, deviceIdLsrIdMap), is(true));
        // Retrieve label from store
        labelId = pceStore.getGlobalNodeLabel(deviceId3);
        // Check whether label is generated for this device deviceId3
        assertThat(labelId, is(notNullValue()));
        //Now add device to deviceIdLsrIdMap
        deviceIdLsrIdMap.put(deviceId3, lsrId3);

        // device 4
        String lsrId4 = "14.1.1.1";
        // Allocate node label for specific device deviceId4
        assertThat(srTeHandler.allocateNodeLabel(deviceId4, lsrId4, deviceIdLsrIdMap), is(true));
        // Retrieve label from store
        labelId = pceStore.getGlobalNodeLabel(deviceId4);
        // Check whether label is generated for this device deviceId4
        assertThat(labelId, is(notNullValue()));
        //Now add device to deviceIdLsrIdMap
        deviceIdLsrIdMap.put(deviceId4, lsrId4);

        // device 5
        String lsrId5 = "15.1.1.1";
        // Allocate node label for specific device deviceId5
        assertThat(srTeHandler.allocateNodeLabel(deviceId5, lsrId5, deviceIdLsrIdMap), is(true));
        // Retrieve label from store
        labelId = pceStore.getGlobalNodeLabel(deviceId5);
        // Check whether label is generated for this device deviceId5
        assertThat(labelId, is(notNullValue()));
        //Now add device to deviceIdLsrIdMap
        deviceIdLsrIdMap.put(deviceId5, lsrId5);
    }

    /**
     * Checks the operation of releaseNodeLabel() method on node label.
     */
    @Test
    public void testReleaseNodeLabelSuccess() {
        testAllocateNodeLabel2();
        // Specific device deviceId1

        //device 1
        String lsrId1 = "11.1.1.1";
        // Check whether successfully released node label
        assertThat(srTeHandler.releaseNodeLabel(deviceId1, lsrId1, deviceIdLsrIdMap), is(true));
        // Check whether successfully removed label from store
        LabelResourceId labelId = pceStore.getGlobalNodeLabel(deviceId1);
        assertThat(labelId, is(nullValue()));
        // Remove released deviceId1
        deviceIdLsrIdMap.remove(deviceId1);

        //device 2
        String lsrId2 = "12.1.1.1";
        // Check whether successfully released node label
        assertThat(srTeHandler.releaseNodeLabel(deviceId2, lsrId2, deviceIdLsrIdMap), is(true));
        // Check whether successfully removed label from store
        labelId = pceStore.getGlobalNodeLabel(deviceId2);
        assertThat(labelId, is(nullValue()));
        // Remove released deviceId2
        deviceIdLsrIdMap.remove(deviceId2);

        //device 3
        String lsrId3 = "13.1.1.1";
        // Check whether successfully released node label
        assertThat(srTeHandler.releaseNodeLabel(deviceId3, lsrId3, deviceIdLsrIdMap), is(true));
        // Check whether successfully removed label from store
        labelId = pceStore.getGlobalNodeLabel(deviceId3);
        assertThat(labelId, is(nullValue()));
        // Remove released deviceId3
        deviceIdLsrIdMap.remove(deviceId3);

        //device 4
        String lsrId4 = "14.1.1.1";
        // Check whether successfully released node label
        assertThat(srTeHandler.releaseNodeLabel(deviceId4, lsrId4, deviceIdLsrIdMap), is(true));
        // Check whether successfully removed label from store
        labelId = pceStore.getGlobalNodeLabel(deviceId4);
        assertThat(labelId, is(nullValue()));
        // Remove released deviceId4
        deviceIdLsrIdMap.remove(deviceId4);

        //device 5
        String lsrId5 = "15.1.1.1";
        // Check whether successfully released node label
        assertThat(srTeHandler.releaseNodeLabel(deviceId5, lsrId5, deviceIdLsrIdMap), is(true));
        // Check whether successfully removed label from store
        labelId = pceStore.getGlobalNodeLabel(deviceId5);
        assertThat(labelId, is(nullValue()));
        // Remove released deviceId5
        deviceIdLsrIdMap.remove(deviceId5);
    }

    @Test
    public void testReleaseNodeLabelFailure() {
        testAllocateNodeLabel2();

        //device 6
        String lsrId6 = "16.1.1.1";
        // Check whether successfully released node label
        DeviceId deviceId6 = DeviceId.deviceId("foo6");
        assertThat(srTeHandler.releaseNodeLabel(deviceId6, lsrId6, deviceIdLsrIdMap), is(false));
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
        pceStore.addGlobalNodeLabel(deviceId1, labelId);
        labelId = LabelResourceId.labelResourceId(4098);
        pceStore.addGlobalNodeLabel(deviceId2, labelId);
        labelId = LabelResourceId.labelResourceId(4099);
        pceStore.addGlobalNodeLabel(deviceId3, labelId);
        labelId = LabelResourceId.labelResourceId(4100);
        pceStore.addGlobalNodeLabel(deviceId4, labelId);
        labelId = LabelResourceId.labelResourceId(4101);
        pceStore.addGlobalNodeLabel(deviceId5, labelId);

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

        // check node-label of deviceId1
        List<LabelResourceId> labelList = labelStack.labelResources();
        Iterator<LabelResourceId> iterator = labelList.iterator();
        labelId = iterator.next();
        assertThat(labelId, is(LabelResourceId.labelResourceId(4097)));

        // check adjacency label of deviceId1
        labelId = iterator.next();
        assertThat(labelId, is(LabelResourceId.labelResourceId(5122)));

        // check node-label of deviceId2
        labelId = iterator.next();
        assertThat(labelId, is(LabelResourceId.labelResourceId(4098)));

        // check adjacency label of deviceId2
        labelId = iterator.next();
        assertThat(labelId, is(LabelResourceId.labelResourceId(5123)));

        // check node-label of deviceId3
        labelId = iterator.next();
        assertThat(labelId, is(LabelResourceId.labelResourceId(4099)));

        // check adjacency label of deviceId3
        labelId = iterator.next();
        assertThat(labelId, is(LabelResourceId.labelResourceId(5124)));

        // check node-label of deviceId4
        labelId = iterator.next();
        assertThat(labelId, is(LabelResourceId.labelResourceId(4100)));

        // check adjacency label of deviceId4
        labelId = iterator.next();
        assertThat(labelId, is(LabelResourceId.labelResourceId(5125)));

        // check node-label of deviceId5
        labelId = iterator.next();
        assertThat(labelId, is(LabelResourceId.labelResourceId(4101)));
    }
}
