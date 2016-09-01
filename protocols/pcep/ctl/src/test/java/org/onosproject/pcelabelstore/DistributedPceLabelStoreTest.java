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
package org.onosproject.pcelabelstore;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import org.onosproject.incubator.net.resource.label.DefaultLabelResource;
import org.onosproject.incubator.net.resource.label.LabelResource;
import org.onosproject.incubator.net.resource.label.LabelResourceId;
import org.onosproject.incubator.net.tunnel.TunnelId;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DefaultAnnotations;
import org.onosproject.net.DefaultLink;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Link;
import org.onosproject.net.PortNumber;
import org.onosproject.net.provider.ProviderId;
import org.onosproject.pcelabelstore.api.LspLocalLabelInfo;
import org.onosproject.pcelabelstore.util.TestStorageService;

/**
 * Unit tests for DistributedPceStore class.
 */
public class DistributedPceLabelStoreTest {

    private DistributedPceLabelStore distrPceStore;
    private DeviceId deviceId1 = DeviceId.deviceId("foo");
    private DeviceId deviceId2 = DeviceId.deviceId("goo");
    private DeviceId deviceId3 = DeviceId.deviceId("yaa");
    private DeviceId deviceId4 = DeviceId.deviceId("zoo");
    private LabelResourceId labelId1 = LabelResourceId.labelResourceId(1);
    private LabelResourceId labelId2 = LabelResourceId.labelResourceId(2);
    private LabelResourceId labelId3 = LabelResourceId.labelResourceId(3);
    private LabelResourceId labelId4 = LabelResourceId.labelResourceId(4);
    private PortNumber portNumber1 = PortNumber.portNumber(1);
    private PortNumber portNumber2 = PortNumber.portNumber(2);
    private PortNumber portNumber3 = PortNumber.portNumber(3);
    private PortNumber portNumber4 = PortNumber.portNumber(4);
    private ConnectPoint srcConnectionPoint1 = new ConnectPoint(deviceId1, portNumber1);
    private ConnectPoint dstConnectionPoint2 = new ConnectPoint(deviceId2, portNumber2);
    private ConnectPoint srcConnectionPoint3 = new ConnectPoint(deviceId3, portNumber3);
    private ConnectPoint dstConnectionPoint4 = new ConnectPoint(deviceId4, portNumber4);
    private LabelResource labelResource1 = new DefaultLabelResource(deviceId1, labelId1);
    private LabelResource labelResource2 = new DefaultLabelResource(deviceId2, labelId2);
    private LabelResource labelResource3 = new DefaultLabelResource(deviceId3, labelId3);
    private LabelResource labelResource4 = new DefaultLabelResource(deviceId4, labelId4);
    private Link link1;
    private Link link2;
    private List<LabelResource> labelList1 = new LinkedList<>();
    private List<LabelResource> labelList2 = new LinkedList<>();
    private TunnelId tunnelId1 = TunnelId.valueOf("1");
    private TunnelId tunnelId2 = TunnelId.valueOf("2");
    private TunnelId tunnelId3 = TunnelId.valueOf("3");
    private TunnelId tunnelId4 = TunnelId.valueOf("4");

    List<LspLocalLabelInfo> lspLocalLabelInfoList1 = new LinkedList<>();
    List<LspLocalLabelInfo> lspLocalLabelInfoList2 = new LinkedList<>();

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
    }

    @Before
    public void setUp() throws Exception {
       distrPceStore = new DistributedPceLabelStore();
       // initialization
       distrPceStore.storageService = new TestStorageService();
       distrPceStore.activate();

       // Initialization of member variables
       link1 = DefaultLink.builder()
                          .providerId(new ProviderId("eth", "1"))
                          .annotations(DefaultAnnotations.builder().set("key1", "yahoo").build())
                          .src(srcConnectionPoint1)
                          .dst(dstConnectionPoint2)
                          .type(Link.Type.DIRECT)
                          .state(Link.State.ACTIVE)
                          .build();
       link2 = DefaultLink.builder()
                          .providerId(new ProviderId("mac", "2"))
                          .annotations(DefaultAnnotations.builder().set("key2", "google").build())
                          .src(srcConnectionPoint3)
                          .dst(dstConnectionPoint4)
                          .type(Link.Type.DIRECT)
                          .state(Link.State.ACTIVE)
                          .build();
       labelList1.add(labelResource1);
       labelList1.add(labelResource2);
       labelList2.add(labelResource3);
       labelList2.add(labelResource4);

       // Create pceccTunnelInfo1
       DeviceId deviceId1 = DeviceId.deviceId("foo");
       LabelResourceId inLabelId1 = LabelResourceId.labelResourceId(1);
       LabelResourceId outLabelId1 = LabelResourceId.labelResourceId(2);

       LspLocalLabelInfo lspLocalLabel1 = DefaultLspLocalLabelInfo.builder()
               .deviceId(deviceId1)
               .inLabelId(inLabelId1)
               .outLabelId(outLabelId1)
               .build();
       lspLocalLabelInfoList1.add(lspLocalLabel1);
       distrPceStore.addTunnelInfo(tunnelId1, lspLocalLabelInfoList1);

       // Create pceccTunnelInfo2
       DeviceId deviceId2 = DeviceId.deviceId("foo");
       LabelResourceId inLabelId2 = LabelResourceId.labelResourceId(3);
       LabelResourceId outLabelId2 = LabelResourceId.labelResourceId(4);

       LspLocalLabelInfo lspLocalLabel2 = DefaultLspLocalLabelInfo.builder()
               .deviceId(deviceId2)
               .inLabelId(inLabelId2)
               .outLabelId(outLabelId2)
               .build();
       lspLocalLabelInfoList2.add(lspLocalLabel2);
       distrPceStore.addTunnelInfo(tunnelId2, lspLocalLabelInfoList2);
    }

    @After
    public void tearDown() throws Exception {
    }

    /**
     * Checks the operation of addGlobalNodeLabel() method.
     */
    @Test
    public void testAddGlobalNodeLabel() {
        // add device with label
        distrPceStore.addGlobalNodeLabel(deviceId1, labelId1);
        assertThat(distrPceStore.existsGlobalNodeLabel(deviceId1), is(true));
        assertThat(distrPceStore.getGlobalNodeLabel(deviceId1), is(labelId1));
        distrPceStore.addGlobalNodeLabel(deviceId2, labelId2);
        assertThat(distrPceStore.existsGlobalNodeLabel(deviceId2), is(true));
        assertThat(distrPceStore.getGlobalNodeLabel(deviceId2), is(labelId2));
    }

    /**
     * Checks the operation of addAdjLabel() method.
     */
    @Test
    public void testAddAdjLabel() {
        // link with list of labels
        distrPceStore.addAdjLabel(link1, labelId1);
        assertThat(distrPceStore.existsAdjLabel(link1), is(true));
        assertThat(distrPceStore.getAdjLabel(link1), is(labelId1));
        distrPceStore.addAdjLabel(link2, labelId2);
        assertThat(distrPceStore.existsAdjLabel(link2), is(true));
        assertThat(distrPceStore.getAdjLabel(link2), is(labelId2));
    }

    /**
     * Checks the operation of addTunnelInfo() method.
     */
    @Test
    public void testAddTunnelInfo() {
        // TunnelId with device label store information
        distrPceStore.addTunnelInfo(tunnelId1, lspLocalLabelInfoList1);
        assertThat(distrPceStore.existsTunnelInfo(tunnelId1), is(true));
        assertThat(distrPceStore.getTunnelInfo(tunnelId1), is(lspLocalLabelInfoList1));
        distrPceStore.addTunnelInfo(tunnelId2, lspLocalLabelInfoList2);
        assertThat(distrPceStore.existsTunnelInfo(tunnelId2), is(true));
        assertThat(distrPceStore.getTunnelInfo(tunnelId2), is(lspLocalLabelInfoList2));
    }

    /**
     * Checks the operation of existsGlobalNodeLabel() method.
     */
    @Test
    public void testExistsGlobalNodeLabel() {
        testAddGlobalNodeLabel();

        assertThat(distrPceStore.existsGlobalNodeLabel(deviceId1), is(true));
        assertThat(distrPceStore.existsGlobalNodeLabel(deviceId2), is(true));
        assertThat(distrPceStore.existsGlobalNodeLabel(deviceId3), is(false));
        assertThat(distrPceStore.existsGlobalNodeLabel(deviceId4), is(false));
    }

    /**
     * Checks the operation of existsAdjLabel() method.
     */
    @Test
    public void testExistsAdjLabel() {
        testAddAdjLabel();

        assertThat(distrPceStore.existsAdjLabel(link1), is(true));
        assertThat(distrPceStore.existsAdjLabel(link2), is(true));
    }

    /**
     * Checks the operation of existsTunnelInfo() method.
     */
    @Test
    public void testExistsTunnelInfo() {
        testAddTunnelInfo();

        assertThat(distrPceStore.existsTunnelInfo(tunnelId1), is(true));
        assertThat(distrPceStore.existsTunnelInfo(tunnelId2), is(true));
        assertThat(distrPceStore.existsTunnelInfo(tunnelId3), is(false));
        assertThat(distrPceStore.existsTunnelInfo(tunnelId4), is(false));
    }

    /**
     * Checks the operation of getGlobalNodeLabelCount() method.
     */
    @Test
    public void testGetGlobalNodeLabelCount() {
        testAddGlobalNodeLabel();

        assertThat(distrPceStore.getGlobalNodeLabelCount(), is(2));
    }

    /**
     * Checks the operation of getAdjLabelCount() method.
     */
    @Test
    public void testGetAdjLabelCount() {
        testAddAdjLabel();

        assertThat(distrPceStore.getAdjLabelCount(), is(2));
    }

    /**
     * Checks the operation of getTunnelInfoCount() method.
     */
    @Test
    public void testGetTunnelInfoCount() {
        testAddTunnelInfo();

        assertThat(distrPceStore.getTunnelInfoCount(), is(2));
    }

    /**
     * Checks the operation of getGlobalNodeLabels() method.
     */
    @Test
    public void testGetGlobalNodeLabels() {
        testAddGlobalNodeLabel();

        Map<DeviceId, LabelResourceId> nodeLabelMap = distrPceStore.getGlobalNodeLabels();
        assertThat(nodeLabelMap, is(notNullValue()));
        assertThat(nodeLabelMap.isEmpty(), is(false));
        assertThat(nodeLabelMap.size(), is(2));
    }

    /**
     * Checks the operation of getAdjLabels() method.
     */
    @Test
    public void testGetAdjLabels() {
        testAddAdjLabel();

        Map<Link, LabelResourceId> adjLabelMap = distrPceStore.getAdjLabels();
        assertThat(adjLabelMap, is(notNullValue()));
        assertThat(adjLabelMap.isEmpty(), is(false));
        assertThat(adjLabelMap.size(), is(2));
    }

    /**
     * Checks the operation of getTunnelInfos() method.
     */
    @Test
    public void testGetTunnelInfos() {
        testAddTunnelInfo();

        Map<TunnelId, List<LspLocalLabelInfo>> tunnelInfoMap = distrPceStore.getTunnelInfos();
        assertThat(tunnelInfoMap, is(notNullValue()));
        assertThat(tunnelInfoMap.isEmpty(), is(false));
        assertThat(tunnelInfoMap.size(), is(2));
    }

    /**
     * Checks the operation of getGlobalNodeLabel() method.
     */
    @Test
    public void testGetGlobalNodeLabel() {
        testAddGlobalNodeLabel();

        // deviceId1 with labelId1
        assertThat(deviceId1, is(notNullValue()));
        assertThat(distrPceStore.getGlobalNodeLabel(deviceId1), is(labelId1));

        // deviceId2 with labelId2
        assertThat(deviceId2, is(notNullValue()));
        assertThat(distrPceStore.getGlobalNodeLabel(deviceId2), is(labelId2));
    }

    /**
     * Checks the operation of getAdjLabel() method.
     */
    @Test
    public void testGetAdjLabel() {
        testAddAdjLabel();

        // link1 with labels
        assertThat(link1, is(notNullValue()));
        assertThat(distrPceStore.getAdjLabel(link1), is(labelId1));

        // link2 with labels
        assertThat(link2, is(notNullValue()));
        assertThat(distrPceStore.getAdjLabel(link2), is(labelId2));
    }

    /**
     * Checks the operation of getTunnelInfo() method.
     */
    @Test
    public void testGetTunnelInfo() {
        testAddTunnelInfo();

        // tunnelId1 with device label store info
        assertThat(tunnelId1, is(notNullValue()));
        assertThat(distrPceStore.getTunnelInfo(tunnelId1), is(lspLocalLabelInfoList1));

        // tunnelId2 with device label store info
        assertThat(tunnelId2, is(notNullValue()));
        assertThat(distrPceStore.getTunnelInfo(tunnelId2), is(lspLocalLabelInfoList2));
    }

    /**
     * Checks the operation of removeGlobalNodeLabel() method.
     */
    @Test
    public void testRemoveGlobalNodeLabel() {
        testAddGlobalNodeLabel();

        assertThat(distrPceStore.removeGlobalNodeLabel(deviceId1), is(true));
        assertThat(distrPceStore.removeGlobalNodeLabel(deviceId2), is(true));
    }

    /**
     * Checks the operation of removeAdjLabel() method.
     */
    @Test
    public void testRemoveAdjLabel() {
        testAddAdjLabel();

        assertThat(distrPceStore.removeAdjLabel(link1), is(true));
        assertThat(distrPceStore.removeAdjLabel(link2), is(true));
    }

    /**
     * Checks the operation of removeTunnelInfo() method.
     */
    @Test
    public void testRemoveTunnelInfo() {
        testAddTunnelInfo();

        assertThat(distrPceStore.removeTunnelInfo(tunnelId1), is(true));
        assertThat(distrPceStore.removeTunnelInfo(tunnelId2), is(true));
    }
}
