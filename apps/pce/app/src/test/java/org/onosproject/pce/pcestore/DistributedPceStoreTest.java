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
package org.onosproject.pce.pcestore;

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

import org.onlab.util.DataRateUnit;
import org.onosproject.incubator.net.resource.label.DefaultLabelResource;
import org.onosproject.incubator.net.resource.label.LabelResource;
import org.onosproject.incubator.net.resource.label.LabelResourceId;
import org.onosproject.incubator.net.tunnel.TunnelId;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DefaultAnnotations;
import org.onosproject.net.DefaultLink;
import org.onosproject.net.DeviceId;
import org.onosproject.net.ElementId;
import org.onosproject.net.intent.Constraint;
import org.onosproject.net.intent.constraint.BandwidthConstraint;
import org.onosproject.net.Link;
import org.onosproject.net.PortNumber;
import org.onosproject.net.resource.ResourceConsumer;
import org.onosproject.pce.pceservice.LspType;
import org.onosproject.pce.pceservice.TunnelConsumerId;
import org.onosproject.pce.pcestore.api.LspLocalLabelInfo;
import org.onosproject.net.provider.ProviderId;
import org.onosproject.store.service.TestStorageService;

/**
 * Unit tests for DistributedPceStore class.
 */
public class DistributedPceStoreTest {

    private DistributedPceStore distrPceStore;
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
    private ConnectPoint srcConnectionPoint1 = new ConnectPoint((ElementId) deviceId1, portNumber1);
    private ConnectPoint dstConnectionPoint2 = new ConnectPoint((ElementId) deviceId2, portNumber2);
    private ConnectPoint srcConnectionPoint3 = new ConnectPoint((ElementId) deviceId3, portNumber3);
    private ConnectPoint dstConnectionPoint4 = new ConnectPoint((ElementId) deviceId4, portNumber4);
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
    private PceccTunnelInfo pceccTunnelInfo1;
    private PceccTunnelInfo pceccTunnelInfo2;
    private PcePathInfo failedPathInfo1;
    private PcePathInfo failedPathInfo2;
    private PcePathInfo failedPathInfo3;
    private PcePathInfo failedPathInfo4;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
    }

    @Before
    public void setUp() throws Exception {
       distrPceStore = new DistributedPceStore();

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
       List<LspLocalLabelInfo> lspLocalLabelInfoList1 = new LinkedList<>();
       ResourceConsumer tunnelConsumerId1 = TunnelConsumerId.valueOf(10);

       DeviceId deviceId1 = DeviceId.deviceId("foo");
       LabelResourceId inLabelId1 = LabelResourceId.labelResourceId(1);
       LabelResourceId outLabelId1 = LabelResourceId.labelResourceId(2);

       LspLocalLabelInfo lspLocalLabel1 = DefaultLspLocalLabelInfo.builder()
               .deviceId(deviceId1)
               .inLabelId(inLabelId1)
               .outLabelId(outLabelId1)
               .build();
       lspLocalLabelInfoList1.add(lspLocalLabel1);

       pceccTunnelInfo1 = new PceccTunnelInfo(lspLocalLabelInfoList1, tunnelConsumerId1);

       // Create pceccTunnelInfo2
       List<LspLocalLabelInfo> lspLocalLabelInfoList2 = new LinkedList<>();
       ResourceConsumer tunnelConsumerId2 = TunnelConsumerId.valueOf(20);

       DeviceId deviceId2 = DeviceId.deviceId("foo");
       LabelResourceId inLabelId2 = LabelResourceId.labelResourceId(3);
       LabelResourceId outLabelId2 = LabelResourceId.labelResourceId(4);

       LspLocalLabelInfo lspLocalLabel2 = DefaultLspLocalLabelInfo.builder()
               .deviceId(deviceId2)
               .inLabelId(inLabelId2)
               .outLabelId(outLabelId2)
               .build();
       lspLocalLabelInfoList2.add(lspLocalLabel2);

       pceccTunnelInfo2 = new PceccTunnelInfo(lspLocalLabelInfoList2, tunnelConsumerId2);

       // Creates failedPathInfo1
       DeviceId src1 = DeviceId.deviceId("foo1");
       DeviceId dst1 = DeviceId.deviceId("goo1");
       String name1 = "pcc1";
       LspType lspType1 = LspType.SR_WITHOUT_SIGNALLING;
       List<Constraint> constraints1 = new LinkedList<>();
       Constraint bandwidth1 = BandwidthConstraint.of(200, DataRateUnit.BPS);
       constraints1.add(bandwidth1);

       failedPathInfo1 = new PcePathInfo(src1, dst1, name1, constraints1, lspType1);

       // Creates failedPathInfo2
       DeviceId src2 = DeviceId.deviceId("foo2");
       DeviceId dst2 = DeviceId.deviceId("goo2");
       String name2 = "pcc2";
       LspType lspType2 = LspType.SR_WITHOUT_SIGNALLING;
       List<Constraint> constraints2 = new LinkedList<>();
       Constraint bandwidth2 = BandwidthConstraint.of(400, DataRateUnit.BPS);
       constraints2.add(bandwidth2);

       failedPathInfo2 = new PcePathInfo(src2, dst2, name2, constraints2, lspType2);

       // Creates failedPathInfo3
       DeviceId src3 = DeviceId.deviceId("foo3");
       DeviceId dst3 = DeviceId.deviceId("goo3");
       String name3 = "pcc3";
       LspType lspType3 = LspType.SR_WITHOUT_SIGNALLING;
       List<Constraint> constraints3 = new LinkedList<>();
       Constraint bandwidth3 = BandwidthConstraint.of(500, DataRateUnit.BPS);
       constraints3.add(bandwidth3);

       failedPathInfo3 = new PcePathInfo(src3, dst3, name3, constraints3, lspType3);

       // Creates failedPathInfo4
       DeviceId src4 = DeviceId.deviceId("foo4");
       DeviceId dst4 = DeviceId.deviceId("goo4");
       String name4 = "pcc4";
       LspType lspType4 = LspType.SR_WITHOUT_SIGNALLING;
       List<Constraint> constraints4 = new LinkedList<>();
       Constraint bandwidth4 = BandwidthConstraint.of(600, DataRateUnit.BPS);
       constraints4.add(bandwidth4);

       failedPathInfo4 = new PcePathInfo(src4, dst4, name4, constraints4, lspType4);
    }

    @After
    public void tearDown() throws Exception {
    }

    /**
     * Checks the operation of addGlobalNodeLabel() method.
     */
    @Test
    public void testAddGlobalNodeLabel() {
        // initialization
        distrPceStore.storageService = new TestStorageService();
        distrPceStore.activate();

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
        // initialization
        distrPceStore.storageService = new TestStorageService();
        distrPceStore.activate();

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
        // initialization
        distrPceStore.storageService = new TestStorageService();
        distrPceStore.activate();

        // TunnelId with device label store information
        distrPceStore.addTunnelInfo(tunnelId1, pceccTunnelInfo1);
        assertThat(distrPceStore.existsTunnelInfo(tunnelId1), is(true));
        assertThat(distrPceStore.getTunnelInfo(tunnelId1), is(pceccTunnelInfo1));
        distrPceStore.addTunnelInfo(tunnelId2, pceccTunnelInfo2);
        assertThat(distrPceStore.existsTunnelInfo(tunnelId2), is(true));
        assertThat(distrPceStore.getTunnelInfo(tunnelId2), is(pceccTunnelInfo2));
    }

    /**
     * Checks the operation of addFailedPathInfo() method.
     */
    @Test
    public void testAddFailedPathInfo() {
        // initialization
        distrPceStore.storageService = new TestStorageService();
        distrPceStore.activate();

        // PcePathInfo with pce path input information
        distrPceStore.addFailedPathInfo(failedPathInfo1);
        assertThat(distrPceStore.existsFailedPathInfo(failedPathInfo1), is(true));
        distrPceStore.addFailedPathInfo(failedPathInfo2);
        assertThat(distrPceStore.existsFailedPathInfo(failedPathInfo2), is(true));
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
     * Checks the operation of existsFailedPathInfo() method.
     */
    @Test
    public void testExistsFailedPathInfo() {
        testAddFailedPathInfo();

        assertThat(distrPceStore.existsFailedPathInfo(failedPathInfo1), is(true));
        assertThat(distrPceStore.existsFailedPathInfo(failedPathInfo2), is(true));
        assertThat(distrPceStore.existsFailedPathInfo(failedPathInfo3), is(false));
        assertThat(distrPceStore.existsFailedPathInfo(failedPathInfo4), is(false));
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
     * Checks the operation of getFailedPathInfoCount() method.
     */
    @Test
    public void testGetFailedPathInfoCount() {
        testAddFailedPathInfo();

        assertThat(distrPceStore.getFailedPathInfoCount(), is(2));
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

        Map<TunnelId, PceccTunnelInfo> tunnelInfoMap = distrPceStore.getTunnelInfos();
        assertThat(tunnelInfoMap, is(notNullValue()));
        assertThat(tunnelInfoMap.isEmpty(), is(false));
        assertThat(tunnelInfoMap.size(), is(2));
    }

    /**
     * Checks the operation of getFailedPathInfos() method.
     */
    @Test
    public void testGetFailedPathInfos() {
        testAddFailedPathInfo();

        Iterable<PcePathInfo> failedPathInfoSet = distrPceStore.getFailedPathInfos();
        assertThat(failedPathInfoSet, is(notNullValue()));
        assertThat(failedPathInfoSet.iterator().hasNext(), is(true));
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
        assertThat(distrPceStore.getTunnelInfo(tunnelId1), is(pceccTunnelInfo1));

        // tunnelId2 with device label store info
        assertThat(tunnelId2, is(notNullValue()));
        assertThat(distrPceStore.getTunnelInfo(tunnelId2), is(pceccTunnelInfo2));
    }

    /**
     * Checks the operation of updateTunnelInfo() method.
     */
    @Test
    public void testUpdateTunnelInfo() {
        // add tunnel info
        testAddTunnelInfo();

        // new updates
        // Create pceccTunnelInfo3
        List<LspLocalLabelInfo> lspLocalLabelInfoList3 = new LinkedList<>();
        ResourceConsumer tunnelConsumerId3 = TunnelConsumerId.valueOf(30);

        DeviceId deviceId3 = DeviceId.deviceId("goo");
        LabelResourceId inLabelId3 = LabelResourceId.labelResourceId(3);
        LabelResourceId outLabelId3 = LabelResourceId.labelResourceId(4);

        LspLocalLabelInfo lspLocalLabel3 = DefaultLspLocalLabelInfo.builder()
               .deviceId(deviceId3)
               .inLabelId(inLabelId3)
               .outLabelId(outLabelId3)
               .build();
        lspLocalLabelInfoList3.add(lspLocalLabel3);

        PceccTunnelInfo pceccTunnelInfo3 = new PceccTunnelInfo(lspLocalLabelInfoList3, tunnelConsumerId3);

        // Create pceccTunnelInfo4
        List<LspLocalLabelInfo> lspLocalLabelInfoList4 = new LinkedList<>();
        ResourceConsumer tunnelConsumerId4 = TunnelConsumerId.valueOf(40);

        DeviceId deviceId4 = DeviceId.deviceId("goo");
        LabelResourceId inLabelId4 = LabelResourceId.labelResourceId(4);
        LabelResourceId outLabelId4 = LabelResourceId.labelResourceId(5);

        LspLocalLabelInfo lspLocalLabel4 = DefaultLspLocalLabelInfo.builder()
               .deviceId(deviceId4)
               .inLabelId(inLabelId4)
               .outLabelId(outLabelId4)
               .build();
        lspLocalLabelInfoList4.add(lspLocalLabel4);

        PceccTunnelInfo pceccTunnelInfo4 = new PceccTunnelInfo(lspLocalLabelInfoList4, tunnelConsumerId4);

        // update only lspLocalLabelInfoList
        assertThat(distrPceStore.updateTunnelInfo(tunnelId1, lspLocalLabelInfoList3), is(true));
        assertThat(distrPceStore.updateTunnelInfo(tunnelId2, lspLocalLabelInfoList4), is(true));

        // update only tunnelConsumerId
        assertThat(distrPceStore.updateTunnelInfo(tunnelId1, tunnelConsumerId3), is(true));
        assertThat(distrPceStore.updateTunnelInfo(tunnelId2, tunnelConsumerId4), is(true));

        assertThat(distrPceStore.getTunnelInfo(tunnelId1), is(pceccTunnelInfo3));
        assertThat(distrPceStore.getTunnelInfo(tunnelId2), is(pceccTunnelInfo4));
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

    /**
     * Checks the operation of removeFailedPathInfo() method.
     */
    @Test
    public void testRemoveFailedPathInfo() {
        testAddFailedPathInfo();

        assertThat(distrPceStore.removeFailedPathInfo(failedPathInfo1), is(true));
        assertThat(distrPceStore.removeFailedPathInfo(failedPathInfo2), is(true));
        assertThat(distrPceStore.removeFailedPathInfo(failedPathInfo3), is(false));
        assertThat(distrPceStore.removeFailedPathInfo(failedPathInfo4), is(false));
    }
}
