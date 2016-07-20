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

import java.util.Iterator;
import java.util.List;
import java.util.LinkedList;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onlab.packet.IpAddress;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.core.DefaultGroupId;
import org.onosproject.incubator.net.tunnel.Tunnel;
import org.onosproject.incubator.net.tunnel.TunnelEndPoint;
import org.onosproject.incubator.net.tunnel.IpTunnelEndPoint;
import org.onosproject.incubator.net.tunnel.TunnelName;
import org.onosproject.incubator.net.tunnel.TunnelId;
import org.onosproject.incubator.net.tunnel.DefaultTunnel;
import org.onosproject.incubator.net.resource.label.LabelResourceId;
import org.onosproject.incubator.net.resource.label.LabelResourceService;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DefaultAnnotations;
import org.onosproject.net.DefaultPath;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.flowobjective.FlowObjectiveService;
import org.onosproject.net.Path;
import org.onosproject.pce.pcestore.api.LspLocalLabelInfo;
import org.onosproject.pce.pcestore.api.PceStore;
import org.onosproject.pce.pcestore.PceccTunnelInfo;
import org.onosproject.net.provider.ProviderId;
import org.onosproject.pce.util.LabelResourceAdapter;
import org.onosproject.pce.util.PceStoreAdapter;
import org.onosproject.net.DefaultLink;
import org.onosproject.net.Link;

/**
 * Unit tests for BasicPceccHandler class.
 */
public class BasicPceccHandlerTest {

    public static final long LOCAL_LABEL_SPACE_MIN = 5122;
    public static final long LOCAL_LABEL_SPACE_MAX = 9217;

    private BasicPceccHandler pceccHandler;
    protected LabelResourceService labelRsrcService;
    protected PceStore pceStore;
    private FlowObjectiveService flowObjectiveService;
    private CoreService coreService;
    private ApplicationId appId;
    private TunnelEndPoint src = IpTunnelEndPoint.ipTunnelPoint(IpAddress.valueOf(23423));
    private TunnelEndPoint dst = IpTunnelEndPoint.ipTunnelPoint(IpAddress.valueOf(32421));
    private DefaultGroupId groupId = new DefaultGroupId(92034);
    private TunnelName tunnelName = TunnelName.tunnelName("TunnelName");
    private TunnelId tunnelId = TunnelId.valueOf("41654654");
    private ProviderId producerName = new ProviderId("producer1", "13");
    private Path path;
    private Tunnel tunnel;
    private PceccTunnelInfo pceccTunnelInfo;
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

    @Before
    public void setUp() throws Exception {
       pceccHandler = BasicPceccHandler.getInstance();
       labelRsrcService = new LabelResourceAdapter();
       pceStore = new PceStoreAdapter();
       flowObjectiveService = new PceManagerTest.MockFlowObjService();
       coreService = new PceManagerTest.MockCoreService();
       appId = coreService.registerApplication("org.onosproject.pce");
       pceccHandler.initialize(labelRsrcService, flowObjectiveService, appId, pceStore);

       // Cretae tunnel test
       // Link
       ProviderId providerId = new ProviderId("of", "foo");
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

       Link l1 = DefaultLink.builder()
                            .providerId(providerId)
                            .annotations(DefaultAnnotations.builder().set("key1", "yahoo").build())
                            .src(new ConnectPoint(deviceId1, port1))
                            .dst(new ConnectPoint(deviceId2, port2))
                            .type(DIRECT)
                            .state(Link.State.ACTIVE)
                            .build();
       linkList.add(l1);
       Link l2 = DefaultLink.builder()
                            .providerId(providerId)
                            .annotations(DefaultAnnotations.builder().set("key2", "yahoo").build())
                            .src(new ConnectPoint(deviceId2, port2))
                            .dst(new ConnectPoint(deviceId3, port3))
                            .type(DIRECT)
                            .state(Link.State.ACTIVE)
                            .build();
       linkList.add(l2);
       Link l3 = DefaultLink.builder()
                            .providerId(providerId)
                            .annotations(DefaultAnnotations.builder().set("key3", "yahoo").build())
                            .src(new ConnectPoint(deviceId3, port3))
                            .dst(new ConnectPoint(deviceId4, port4))
                            .type(DIRECT)
                            .state(Link.State.ACTIVE)
                            .build();
       linkList.add(l3);
       Link l4 = DefaultLink.builder()
                            .providerId(providerId)
                            .annotations(DefaultAnnotations.builder().set("key4", "yahoo").build())
                            .src(new ConnectPoint(deviceId4, port4))
                            .dst(new ConnectPoint(deviceId5, port5))
                            .type(DIRECT)
                            .state(Link.State.ACTIVE)
                            .build();
       linkList.add(l4);

       // Path
       path = new DefaultPath(providerId, linkList, 10);

       // Tunnel
       tunnel = new DefaultTunnel(producerName, src, dst, Tunnel.Type.VXLAN,
                                  Tunnel.State.ACTIVE, groupId, tunnelId,
                                  tunnelName, path);
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
        assertThat(pceccHandler, is(notNullValue()));
    }

    /**
     * Checks the operation of allocateLabel() method.
     */
    @Test
    public void testAllocateLabel() {
       List<LspLocalLabelInfo> lspLocalLabelInfoList;
       Iterator<LspLocalLabelInfo> iterator;
       LspLocalLabelInfo lspLocalLabelInfo;
       DeviceId deviceId;
       LabelResourceId inLabelId;
       LabelResourceId outLabelId;
       PortNumber inPort;
       PortNumber outPort;

       // check allocation result
       assertThat(pceccHandler.allocateLabel(tunnel), is(true));

       // Check list of devices with IN and OUT labels whether stored properly in store
       pceccTunnelInfo = pceStore.getTunnelInfo(tunnel.tunnelId());
       lspLocalLabelInfoList = pceccTunnelInfo.lspLocalLabelInfoList();
       iterator = lspLocalLabelInfoList.iterator();

       // Retrieve values and check device5
       lspLocalLabelInfo = iterator.next();
       deviceId = lspLocalLabelInfo.deviceId();
       inLabelId = lspLocalLabelInfo.inLabelId();
       outLabelId = lspLocalLabelInfo.outLabelId();
       inPort = lspLocalLabelInfo.inPort();
       outPort = lspLocalLabelInfo.outPort();

       assertThat(deviceId, is(deviceId5));
       assertThat(inLabelId, is(notNullValue()));
       assertThat(outLabelId, is(nullValue()));
       assertThat(inPort, is(port5));
       assertThat(outPort, is(nullValue()));

       // Next element check
       // Retrieve values and check device4
       lspLocalLabelInfo = iterator.next();
       deviceId = lspLocalLabelInfo.deviceId();
       inLabelId = lspLocalLabelInfo.inLabelId();
       outLabelId = lspLocalLabelInfo.outLabelId();
       inPort = lspLocalLabelInfo.inPort();
       outPort = lspLocalLabelInfo.outPort();

       assertThat(deviceId, is(deviceId4));
       assertThat(inLabelId, is(notNullValue()));
       assertThat(outLabelId, is(notNullValue()));
       assertThat(inPort, is(port4));
       assertThat(outPort, is(port5));

       // Next element check
       // Retrieve values and check device3
       lspLocalLabelInfo = iterator.next();
       deviceId = lspLocalLabelInfo.deviceId();
       inLabelId = lspLocalLabelInfo.inLabelId();
       outLabelId = lspLocalLabelInfo.outLabelId();
       inPort = lspLocalLabelInfo.inPort();
       outPort = lspLocalLabelInfo.outPort();

       assertThat(deviceId, is(deviceId3));
       assertThat(inLabelId, is(notNullValue()));
       assertThat(outLabelId, is(notNullValue()));
       assertThat(inPort, is(port3));
       assertThat(outPort, is(port4));

       // Next element check
       // Retrieve values and check device2
       lspLocalLabelInfo = iterator.next();
       deviceId = lspLocalLabelInfo.deviceId();
       inLabelId = lspLocalLabelInfo.inLabelId();
       outLabelId = lspLocalLabelInfo.outLabelId();
       inPort = lspLocalLabelInfo.inPort();
       outPort = lspLocalLabelInfo.outPort();

       assertThat(deviceId, is(deviceId2));
       assertThat(inLabelId, is(notNullValue()));
       assertThat(outLabelId, is(notNullValue()));
       assertThat(inPort, is(port2));
       assertThat(outPort, is(port3));

       // Next element check
       // Retrieve values and check device1
       lspLocalLabelInfo = iterator.next();
       deviceId = lspLocalLabelInfo.deviceId();
       inLabelId = lspLocalLabelInfo.inLabelId();
       outLabelId = lspLocalLabelInfo.outLabelId();
       inPort = lspLocalLabelInfo.inPort();
       outPort = lspLocalLabelInfo.outPort();

       assertThat(deviceId, is(deviceId1));
       assertThat(inLabelId, is(nullValue()));
       assertThat(outLabelId, is(notNullValue()));
       assertThat(inPort, is(nullValue()));
       assertThat(outPort, is(port2));
    }

    /**
     * Checks the operation of releaseLabel() method.
     */
    @Test
    public void testReleaseLabel() {
       // Release tunnels
       assertThat(pceccHandler.allocateLabel(tunnel), is(true));
       pceccHandler.releaseLabel(tunnel);

       // Retrieve from store. Store should not contain this tunnel info.
       pceccTunnelInfo = pceStore.getTunnelInfo(tunnel.tunnelId());
       assertThat(pceccTunnelInfo, is(nullValue()));
    }
}
