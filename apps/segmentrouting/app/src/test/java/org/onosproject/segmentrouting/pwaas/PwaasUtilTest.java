/*
 * Copyright 2018-present Open Networking Foundation
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
package org.onosproject.segmentrouting.pwaas;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.onlab.junit.TestUtils;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DefaultPort;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Port;
import org.onosproject.net.PortNumber;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.intf.Interface;
import org.onosproject.net.intf.InterfaceService;
import org.onosproject.segmentrouting.MockDevice;

import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;

public class PwaasUtilTest {
    private static final DeviceId DID1 = DeviceId.deviceId("of:1");
    private static final DeviceId DID2 = DeviceId.deviceId("of:2");
    private static final DeviceId DID99 = DeviceId.deviceId("of:99");
    private static final PortNumber PN1 = PortNumber.portNumber(1);
    private static final PortNumber PN2 = PortNumber.portNumber(2);
    private static final PortNumber PN99 = PortNumber.portNumber(99);
    private static final ConnectPoint CP11 = new ConnectPoint(DID1, PN1);
    private static final ConnectPoint CP12 = new ConnectPoint(DID1, PN2);
    private static final ConnectPoint CP21 = new ConnectPoint(DID2, PN1);
    private static final ConnectPoint CP22 = new ConnectPoint(DID2, PN2);
    private static final VlanId V1 = VlanId.vlanId((short) 1);
    private static final VlanId V2 = VlanId.vlanId((short) 2);
    private static final Device D1 = new MockDevice(DID1, null);
    private static final Device D2 = new MockDevice(DID2, null);
    private static final Port P11 = new DefaultPort(D1, PN1, true);
    private static final Port P12 = new DefaultPort(D1, PN2, true);
    private static final Port P21 = new DefaultPort(D2, PN1, true);
    private static final Port P22 = new DefaultPort(D2, PN2, true);
    private static final Interface I11 = new Interface("I11", CP11, Lists.newArrayList(), MacAddress.NONE,
            VlanId.NONE, VlanId.NONE, Sets.newHashSet(VlanId.NONE), VlanId.NONE);
    private static final Interface I12 = new Interface("I12", CP12, Lists.newArrayList(), MacAddress.NONE,
            VlanId.NONE, VlanId.NONE, Sets.newHashSet(VlanId.NONE), VlanId.NONE);
    private static final Interface I21 = new Interface("I21", CP21, Lists.newArrayList(), MacAddress.NONE,
            VlanId.NONE, VlanId.NONE, Sets.newHashSet(VlanId.NONE), VlanId.NONE);
    private static final Interface I22 = new Interface("I22", CP22, Lists.newArrayList(), MacAddress.NONE,
            VlanId.NONE, VlanId.NONE, Sets.newHashSet(VlanId.NONE), VlanId.NONE);

    private ConnectPoint cp1;
    private ConnectPoint cp2;
    private VlanId ingressInner;
    private VlanId ingressOuter;
    private VlanId egressInner;
    private VlanId egressOuter;
    private static final Long TUNNEL_ID = (long) 1234;

    @Before
    public void setUp() {
        DeviceService deviceService = createNiceMock(DeviceService.class);
        InterfaceService intfService = createNiceMock(InterfaceService.class);
        TestUtils.setField(PwaasUtil.class, "deviceService", deviceService);
        TestUtils.setField(PwaasUtil.class, "intfService", intfService);

        expect(deviceService.getDevice(DID1)).andReturn(D1).anyTimes();
        expect(deviceService.getDevice(DID2)).andReturn(D2).anyTimes();
        expect(deviceService.getPort(CP11)).andReturn(P11).anyTimes();
        expect(deviceService.getPort(CP12)).andReturn(P12).anyTimes();
        expect(deviceService.getPort(CP21)).andReturn(P21).anyTimes();
        expect(deviceService.getPort(CP22)).andReturn(P22).anyTimes();
        expect(intfService.getInterfacesByPort(CP11)).andReturn(Sets.newHashSet(I11)).anyTimes();
        expect(intfService.getInterfacesByPort(CP12)).andReturn(Sets.newHashSet(I12)).anyTimes();
        expect(intfService.getInterfacesByPort(CP21)).andReturn(Sets.newHashSet(I21)).anyTimes();
        expect(intfService.getInterfacesByPort(CP22)).andReturn(Sets.newHashSet(I22)).anyTimes();
        replay(deviceService);
        replay(intfService);
    }

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @Test
    public void verifyPolicy() {
        cp1 = new ConnectPoint(DID1, PN1);
        cp2 = new ConnectPoint(DID2, PN2);
        ingressInner = V1;
        ingressOuter = V2;
        egressInner = V1;
        egressOuter = V2;
        PwaasUtil.verifyPolicy(cp1, cp2, ingressInner, ingressOuter, egressInner, egressOuter, TUNNEL_ID);

        ingressInner = VlanId.NONE;
        ingressOuter = VlanId.NONE;
        egressInner = VlanId.NONE;
        egressOuter = VlanId.NONE;
        PwaasUtil.verifyPolicy(cp1, cp2, ingressInner, ingressOuter, egressInner, egressOuter, TUNNEL_ID);
    }

    @Test
    public void verifyPolicyOnSameDevice() {
        cp1 = new ConnectPoint(DID1, PN1);
        cp2 = new ConnectPoint(DID1, PN2);
        ingressInner = VlanId.NONE;
        ingressOuter = VlanId.NONE;
        egressInner = VlanId.NONE;
        egressOuter = VlanId.NONE;
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage(String.format(PwaasUtil.ERR_SAME_DEV, TUNNEL_ID));
        PwaasUtil.verifyPolicy(cp1, cp2, ingressInner, ingressOuter, egressInner, egressOuter, TUNNEL_ID);
    }

    @Test
    public void verifyPolicyEmptyInnerCp1() {
        cp1 = new ConnectPoint(DID1, PN1);
        cp2 = new ConnectPoint(DID2, PN2);
        ingressInner = VlanId.NONE;
        ingressOuter = V1;
        egressInner = VlanId.NONE;
        egressOuter = VlanId.NONE;
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage(String.format(PwaasUtil.ERR_EMPTY_INNER_WHEN_OUTER_PRESENT, TUNNEL_ID, "cp1"));
        PwaasUtil.verifyPolicy(cp1, cp2, ingressInner, ingressOuter, egressInner, egressOuter, TUNNEL_ID);
    }

    @Test
    public void verifyPolicyEmptyInnerCp2() {
        cp1 = new ConnectPoint(DID1, PN1);
        cp2 = new ConnectPoint(DID2, PN2);
        ingressInner = VlanId.NONE;
        ingressOuter = VlanId.NONE;
        egressInner = VlanId.NONE;
        egressOuter = V1;
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage(String.format(PwaasUtil.ERR_EMPTY_INNER_WHEN_OUTER_PRESENT, TUNNEL_ID, "cp2"));
        PwaasUtil.verifyPolicy(cp1, cp2, ingressInner, ingressOuter, egressInner, egressOuter, TUNNEL_ID);
    }

    @Test
    public void verifyPolicyVlanWildcard() {
        cp1 = new ConnectPoint(DID1, PN1);
        cp2 = new ConnectPoint(DID2, PN2);
        ingressInner = VlanId.ANY;
        ingressOuter = VlanId.NONE;
        egressInner = VlanId.NONE;
        egressOuter = VlanId.NONE;
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage(String.format(PwaasUtil.ERR_WILDCARD_VLAN, TUNNEL_ID));
        PwaasUtil.verifyPolicy(cp1, cp2, ingressInner, ingressOuter, egressInner, egressOuter, TUNNEL_ID);
    }


    @Test
    public void verifyPolicyDeviceServiceNotAvailable() {
        TestUtils.setField(PwaasUtil.class, "deviceService", null);
        cp1 = new ConnectPoint(DID1, PN1);
        cp2 = new ConnectPoint(DID2, PN2);
        ingressInner = V1;
        ingressOuter = V2;
        egressInner = V1;
        egressOuter = V2;
        exception.expect(IllegalStateException.class);
        exception.expectMessage(String.format(PwaasUtil.ERR_SERVICE_UNAVAIL, "DeviceService"));
        PwaasUtil.verifyPolicy(cp1, cp2, ingressInner, ingressOuter, egressInner, egressOuter, TUNNEL_ID);
    }

    @Test
    public void verifyPolicyDoubleToUntagged() {
        cp1 = new ConnectPoint(DID1, PN1);
        cp2 = new ConnectPoint(DID2, PN2);
        ingressInner = V1;
        ingressOuter = V2;
        egressInner = VlanId.NONE;
        egressOuter = VlanId.NONE;
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage(String.format(PwaasUtil.ERR_DOUBLE_TO_UNTAGGED, TUNNEL_ID));
        PwaasUtil.verifyPolicy(cp1, cp2, ingressInner, ingressOuter, egressInner, egressOuter, TUNNEL_ID);
    }

    @Test
    public void verifyPolicyDoubleToSingle() {
        cp1 = new ConnectPoint(DID1, PN1);
        cp2 = new ConnectPoint(DID2, PN2);
        ingressInner = V1;
        ingressOuter = V2;
        egressInner = V1;
        egressOuter = VlanId.NONE;
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage(String.format(PwaasUtil.ERR_DOUBLE_TO_SINGLE, TUNNEL_ID));
        PwaasUtil.verifyPolicy(cp1, cp2, ingressInner, ingressOuter, egressInner, egressOuter, TUNNEL_ID);
    }

    @Test
    public void verifyPolicySingleToUntagged() {
        cp1 = new ConnectPoint(DID1, PN1);
        cp2 = new ConnectPoint(DID2, PN2);
        ingressInner = V1;
        ingressOuter = VlanId.NONE;
        egressInner = VlanId.NONE;
        egressOuter = VlanId.NONE;
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage(String.format(PwaasUtil.ERR_SINGLE_TO_UNTAGGED, TUNNEL_ID));
        PwaasUtil.verifyPolicy(cp1, cp2, ingressInner, ingressOuter, egressInner, egressOuter, TUNNEL_ID);
    }

    @Test
    public void verifyPolicyVlanTranslation() {
        cp1 = new ConnectPoint(DID1, PN1);
        cp2 = new ConnectPoint(DID2, PN2);
        ingressInner = V1;
        ingressOuter = VlanId.NONE;
        egressInner = V2;
        egressOuter = VlanId.NONE;
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage(String.format(PwaasUtil.ERR_VLAN_TRANSLATION, TUNNEL_ID));
        PwaasUtil.verifyPolicy(cp1, cp2, ingressInner, ingressOuter, egressInner, egressOuter, TUNNEL_ID);
    }

    @Test
    public void verifyPolicyDeviceNotFound() {
        cp1 = new ConnectPoint(DID99, PN1);
        cp2 = new ConnectPoint(DID2, PN2);
        ingressInner = V1;
        ingressOuter = V2;
        egressInner = V1;
        egressOuter = V2;
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage(String.format(PwaasUtil.ERR_DEV_NOT_FOUND, DID99, TUNNEL_ID));
        PwaasUtil.verifyPolicy(cp1, cp2, ingressInner, ingressOuter, egressInner, egressOuter, TUNNEL_ID);
    }

    @Test
    public void verifyPolicyPortNotFound() {
        cp1 = new ConnectPoint(DID1, PN99);
        cp2 = new ConnectPoint(DID2, PN2);
        ingressInner = V1;
        ingressOuter = V2;
        egressInner = V1;
        egressOuter = V2;
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage(String.format(PwaasUtil.ERR_PORT_NOT_FOUND, PN99, DID1, TUNNEL_ID));
        PwaasUtil.verifyPolicy(cp1, cp2, ingressInner, ingressOuter, egressInner, egressOuter, TUNNEL_ID);
    }
}