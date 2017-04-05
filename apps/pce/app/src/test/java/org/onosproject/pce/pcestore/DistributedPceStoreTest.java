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

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.onlab.util.DataRateUnit;
import org.onosproject.incubator.net.resource.label.LabelResourceId;
import org.onosproject.incubator.net.tunnel.TunnelId;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DefaultAnnotations;
import org.onosproject.net.DefaultLink;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Link;
import org.onosproject.net.PortNumber;
import org.onosproject.net.intent.Constraint;
import org.onosproject.net.intent.constraint.BandwidthConstraint;
import org.onosproject.net.provider.ProviderId;
import org.onosproject.net.resource.ResourceConsumer;
import org.onosproject.pce.pceservice.ExplicitPathInfo;
import org.onosproject.pce.pceservice.LspType;
import org.onosproject.pce.pceservice.TunnelConsumerId;
import org.onosproject.store.service.TestStorageService;

import java.util.LinkedList;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

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
    private ConnectPoint srcConnectionPoint1 = new ConnectPoint(deviceId1, portNumber1);
    private ConnectPoint dstConnectionPoint2 = new ConnectPoint(deviceId2, portNumber2);
    private ConnectPoint srcConnectionPoint3 = new ConnectPoint(deviceId3, portNumber3);
    private ConnectPoint dstConnectionPoint4 = new ConnectPoint(deviceId4, portNumber4);
    private Link link1;
    private Link link2;
    private TunnelId tunnelId1 = TunnelId.valueOf("1");
    private TunnelId tunnelId2 = TunnelId.valueOf("2");
    private TunnelId tunnelId3 = TunnelId.valueOf("3");
    private TunnelId tunnelId4 = TunnelId.valueOf("4");
    private ResourceConsumer tunnelConsumerId1 = TunnelConsumerId.valueOf(10);
    private ResourceConsumer tunnelConsumerId2 = TunnelConsumerId.valueOf(20);
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

       // Creates failedPathInfo1
       DeviceId src1 = DeviceId.deviceId("foo1");
       DeviceId dst1 = DeviceId.deviceId("goo1");
       String name1 = "pcc1";
       LspType lspType1 = LspType.SR_WITHOUT_SIGNALLING;
       List<Constraint> constraints1 = new LinkedList<>();
       Constraint bandwidth1 = BandwidthConstraint.of(200, DataRateUnit.BPS);
       constraints1.add(bandwidth1);

       failedPathInfo1 = new PcePathInfo(src1, dst1, name1, constraints1, lspType1, null, false);

       // Creates failedPathInfo2
       DeviceId src2 = DeviceId.deviceId("foo2");
       DeviceId dst2 = DeviceId.deviceId("goo2");
       String name2 = "pcc2";
       LspType lspType2 = LspType.SR_WITHOUT_SIGNALLING;
       List<Constraint> constraints2 = new LinkedList<>();
       Constraint bandwidth2 = BandwidthConstraint.of(400, DataRateUnit.BPS);
       constraints2.add(bandwidth2);

       failedPathInfo2 = new PcePathInfo(src2, dst2, name2, constraints2, lspType2, null, false);

       // Creates failedPathInfo3
       DeviceId src3 = DeviceId.deviceId("foo3");
       DeviceId dst3 = DeviceId.deviceId("goo3");
       String name3 = "pcc3";
       LspType lspType3 = LspType.SR_WITHOUT_SIGNALLING;
       List<Constraint> constraints3 = new LinkedList<>();
       Constraint bandwidth3 = BandwidthConstraint.of(500, DataRateUnit.BPS);
       constraints3.add(bandwidth3);

       failedPathInfo3 = new PcePathInfo(src3, dst3, name3, constraints3, lspType3, null, false);

       // Creates failedPathInfo4
       DeviceId src4 = DeviceId.deviceId("foo4");
       DeviceId dst4 = DeviceId.deviceId("goo4");
       String name4 = "pcc4";
       LspType lspType4 = LspType.SR_WITHOUT_SIGNALLING;
       List<Constraint> constraints4 = new LinkedList<>();
       Constraint bandwidth4 = BandwidthConstraint.of(600, DataRateUnit.BPS);
       constraints4.add(bandwidth4);

       failedPathInfo4 = new PcePathInfo(src4, dst4, name4, constraints4, lspType4, null, false);
    }

    @After
    public void tearDown() throws Exception {
    }

    /**
     * Checks the operation of addTunnelInfo() method.
     */
    @Test
    public void testAddExplicitPathInfo() {
        // initialization
        distrPceStore.storageService = new TestStorageService();
        distrPceStore.activate();

        List<ExplicitPathInfo> infoList = new LinkedList<>();
        ExplicitPathInfo info1 = new ExplicitPathInfo(ExplicitPathInfo.Type.LOOSE, DeviceId.deviceId("D1"));
        infoList.add(info1);
        distrPceStore.tunnelNameExplicitPathInfoMap("Sample1", infoList);
        assertThat(distrPceStore.getTunnelNameExplicitPathInfoMap("Sample1"), is(infoList));
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
     * Checks the operation of getFailedPathInfoCount() method.
     */
    @Test
    public void testGetFailedPathInfoCount() {
        testAddFailedPathInfo();

        assertThat(distrPceStore.getFailedPathInfoCount(), is(2));
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
