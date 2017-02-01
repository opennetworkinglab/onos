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
package org.onosproject.pcelabelstore.label;

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
import org.onosproject.core.GroupId;
import org.onosproject.incubator.net.tunnel.Tunnel;
import org.onosproject.incubator.net.tunnel.TunnelEndPoint;
import org.onosproject.incubator.net.tunnel.IpTunnelEndPoint;
import org.onosproject.incubator.net.tunnel.TunnelName;
import org.onosproject.incubator.net.tunnel.TunnelId;
import org.onosproject.incubator.net.tunnel.DefaultTunnel;
import org.onosproject.incubator.net.resource.label.LabelResourceId;
import org.onosproject.incubator.net.resource.label.LabelResourceService;
import org.onosproject.net.AnnotationKeys;
import org.onosproject.net.Annotations;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DefaultAnnotations;
import org.onosproject.net.DefaultDevice;
import org.onosproject.net.DefaultPath;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.Path;
import org.onosproject.net.provider.ProviderId;
import org.onosproject.pcelabelstore.api.LspLocalLabelInfo;
import org.onosproject.pcelabelstore.api.PceLabelStore;
import org.onosproject.pcelabelstore.util.LabelResourceAdapter;
import org.onosproject.pcelabelstore.util.MockDeviceService;
import org.onosproject.pcelabelstore.util.PceLabelStoreAdapter;
import org.onosproject.pcep.controller.impl.BasicPceccHandler;
import org.onosproject.pcep.controller.impl.PcepClientControllerImpl;
import org.onosproject.net.DefaultLink;
import org.onosproject.net.Link;

/**
 * Unit tests for BasicPceccHandler class.
 */
public class BasicPceccHandlerTest {

    public static final long LOCAL_LABEL_SPACE_MIN = 5122;
    public static final long LOCAL_LABEL_SPACE_MAX = 9217;
    private static final String L3 = "L3";
    private static final String LSRID = "lsrId";

    private BasicPceccHandler pceccHandler;
    protected LabelResourceService labelRsrcService;
    protected MockDeviceService deviceService;
    protected PceLabelStore pceStore;
    private TunnelEndPoint src = IpTunnelEndPoint.ipTunnelPoint(IpAddress.valueOf(23423));
    private TunnelEndPoint dst = IpTunnelEndPoint.ipTunnelPoint(IpAddress.valueOf(32421));
    private GroupId groupId = new GroupId(92034);
    private TunnelName tunnelName = TunnelName.tunnelName("TunnelName");
    private TunnelId tunnelId = TunnelId.valueOf("41654654");
    private ProviderId producerName = new ProviderId("producer1", "13");
    private Path path;
    private Tunnel tunnel;
    List<LspLocalLabelInfo> lspLocalLabelInfoList;
    private Device deviceD1, deviceD2, deviceD3, deviceD4, deviceD5;
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
       pceStore = new PceLabelStoreAdapter();
       deviceService = new MockDeviceService();
       pceccHandler.initialize(labelRsrcService,
                              deviceService,
                              pceStore,
                              new PcepClientControllerImpl());

       // Create tunnel test
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

       // Making L3 devices
       DefaultAnnotations.Builder builderDev1 = DefaultAnnotations.builder();
       builderDev1.set(AnnotationKeys.TYPE, L3);
       builderDev1.set(LSRID, "1.1.1.1");
       deviceD1 = new MockDevice(deviceId1, builderDev1.build());
       deviceService.addDevice(deviceD1);

       // Making L3 devices
       DefaultAnnotations.Builder builderDev2 = DefaultAnnotations.builder();
       builderDev2.set(AnnotationKeys.TYPE, L3);
       builderDev2.set(LSRID, "2.2.2.2");
       deviceD2 = new MockDevice(deviceId2, builderDev2.build());
       deviceService.addDevice(deviceD2);

       // Making L3 devices
       DefaultAnnotations.Builder builderDev3 = DefaultAnnotations.builder();
       builderDev3.set(AnnotationKeys.TYPE, L3);
       builderDev3.set(LSRID, "3.3.3.3");
       deviceD3 = new MockDevice(deviceId3, builderDev3.build());
       deviceService.addDevice(deviceD3);

       // Making L3 devices
       DefaultAnnotations.Builder builderDev4 = DefaultAnnotations.builder();
       builderDev4.set(AnnotationKeys.TYPE, L3);
       builderDev4.set(LSRID, "4.4.4.4");
       deviceD4 = new MockDevice(deviceId4, builderDev4.build());
       deviceService.addDevice(deviceD4);

       // Making L3 devices
       DefaultAnnotations.Builder builderDev5 = DefaultAnnotations.builder();
       builderDev5.set(AnnotationKeys.TYPE, L3);
       builderDev5.set(LSRID, "5.5.5.5");
       deviceD5 = new MockDevice(deviceId5, builderDev5.build());
       deviceService.addDevice(deviceD5);

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
       lspLocalLabelInfoList = pceStore.getTunnelInfo(tunnel.tunnelId());
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
       lspLocalLabelInfoList = pceStore.getTunnelInfo(tunnel.tunnelId());
       assertThat(lspLocalLabelInfoList, is(nullValue()));
    }

    private class MockDevice extends DefaultDevice {
        MockDevice(DeviceId id, Annotations annotations) {
            super(null, id, null, null, null, null, null, null, annotations);
        }
    }
}
