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

package org.onosproject.pipelines.fabric.impl.behaviour;

import com.google.common.collect.ImmutableList;
import org.junit.Before;
import org.junit.Test;
import org.onlab.packet.MacAddress;
import org.onlab.packet.MplsLabel;
import org.onlab.packet.VlanId;
import org.onlab.util.ImmutableByteSequence;
import org.onosproject.net.PortNumber;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.pi.runtime.PiAction;
import org.onosproject.net.pi.runtime.PiActionParam;

import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;

/**
 * Test for fabric interpreter.
 */
public class FabricInterpreterTest {
    private static final VlanId VLAN_100 = VlanId.vlanId("100");
    private static final PortNumber PORT_1 = PortNumber.portNumber(1);
    private static final MacAddress SRC_MAC = MacAddress.valueOf("00:00:00:00:00:01");
    private static final MacAddress DST_MAC = MacAddress.valueOf("00:00:00:00:00:02");
    private static final MplsLabel MPLS_10 = MplsLabel.mplsLabel(10);

    private FabricInterpreter interpreter;

    FabricCapabilities allCapabilities;

    @Before
    public void setup() {
        allCapabilities = createNiceMock(FabricCapabilities.class);
        expect(allCapabilities.hasHashedTable()).andReturn(true).anyTimes();
        expect(allCapabilities.supportDoubleVlanTerm()).andReturn(true).anyTimes();
        replay(allCapabilities);
        interpreter = new FabricInterpreter(allCapabilities);
    }

    /* Filtering control block */

    /**
     * Map treatment to push_internal_vlan action.
     */
    @Test
    public void testFilteringTreatmentPermitWithInternalVlan() throws Exception {
        TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                .pushVlan()
                .setVlanId(VLAN_100)
                .build();
        PiAction mappedAction = interpreter.mapTreatment(treatment,
                                                         FabricConstants.FABRIC_INGRESS_FILTERING_INGRESS_PORT_VLAN);
        PiActionParam param = new PiActionParam(FabricConstants.VLAN_ID,
                                                ImmutableByteSequence.copyFrom(VLAN_100.toShort()));
        PiAction expectedAction = PiAction.builder()
                .withId(FabricConstants.FABRIC_INGRESS_FILTERING_PERMIT_WITH_INTERNAL_VLAN)
                .withParameter(param)
                .build();

        assertEquals(expectedAction, mappedAction);
    }

    /**
     * Map treatment to permit action.
     */
    @Test
    public void testFilteringTreatmentPermit() throws Exception {
        TrafficTreatment treatment = DefaultTrafficTreatment.emptyTreatment();
        PiAction mappedAction = interpreter.mapTreatment(treatment,
                                                         FabricConstants.FABRIC_INGRESS_FILTERING_INGRESS_PORT_VLAN);
        PiAction expectedAction = PiAction.builder()
                .withId(FabricConstants.FABRIC_INGRESS_FILTERING_PERMIT)
                .build();

        assertEquals(expectedAction, mappedAction);
    }

    /* Forwarding control block */

    /**
     * Map empty treatment for routing v4 table.
     */
    @Test
    public void testRoutingV4TreatmentEmpty() throws Exception {
        TrafficTreatment treatment = DefaultTrafficTreatment.emptyTreatment();
        PiAction mappedAction = interpreter.mapTreatment(
                treatment, FabricConstants.FABRIC_INGRESS_FORWARDING_ROUTING_V4);
        PiAction expectedAction = PiAction.builder()
                .withId(FabricConstants.FABRIC_INGRESS_FORWARDING_NOP_ROUTING_V4)
                .build();
        assertEquals(expectedAction, mappedAction);
    }

    /**
     * Map empty treatment for ACL table.
     */
    @Test
    public void testAclTreatmentEmpty() throws Exception {
        TrafficTreatment treatment = DefaultTrafficTreatment.emptyTreatment();
        PiAction mappedAction = interpreter.mapTreatment(
                treatment, FabricConstants.FABRIC_INGRESS_ACL_ACL);
        PiAction expectedAction = PiAction.builder()
                .withId(FabricConstants.FABRIC_INGRESS_ACL_NOP_ACL)
                .build();
        assertEquals(expectedAction, mappedAction);
    }

    /* Next control block */

    /**
     * Map treatment to output action.
     */
    @Test
    public void testNextTreatmentSimpleOutput() throws Exception {
        TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                .setOutput(PORT_1)
                .build();
        PiAction mappedAction = interpreter.mapTreatment(
                treatment, FabricConstants.FABRIC_INGRESS_NEXT_SIMPLE);
        PiActionParam param = new PiActionParam(FabricConstants.PORT_NUM, PORT_1.toLong());
        PiAction expectedAction = PiAction.builder()
                .withId(FabricConstants.FABRIC_INGRESS_NEXT_OUTPUT_SIMPLE)
                .withParameter(param)
                .build();
        assertEquals(expectedAction, mappedAction);
    }

    /**
     * Map treatment for hashed table to routing v4 action.
     */
    @Test
    public void testNextTreatmentHashedRoutingV4() throws Exception {
        TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                .setEthSrc(SRC_MAC)
                .setEthDst(DST_MAC)
                .setOutput(PORT_1)
                .build();
        PiAction mappedAction = interpreter.mapTreatment(
                treatment, FabricConstants.FABRIC_INGRESS_NEXT_HASHED);
        PiActionParam ethSrcParam = new PiActionParam(FabricConstants.SMAC, SRC_MAC.toBytes());
        PiActionParam ethDstParam = new PiActionParam(FabricConstants.DMAC, DST_MAC.toBytes());
        PiActionParam portParam = new PiActionParam(FabricConstants.PORT_NUM, PORT_1.toLong());
        PiAction expectedAction = PiAction.builder()
                .withId(FabricConstants.FABRIC_INGRESS_NEXT_ROUTING_HASHED)
                .withParameters(ImmutableList.of(ethSrcParam, ethDstParam, portParam))
                .build();
        assertEquals(expectedAction, mappedAction);
    }

    /**
     * Map treatment for hashed table to routing v4 action.
     */
    @Test
    public void testNextTreatmentHashedRoutingMpls() throws Exception {
        TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                .setEthSrc(SRC_MAC)
                .setEthDst(DST_MAC)
                .setOutput(PORT_1)
                .pushMpls()
                .setMpls(MPLS_10)
                .build();
        PiAction mappedAction = interpreter.mapTreatment(
                treatment, FabricConstants.FABRIC_INGRESS_NEXT_HASHED);
        PiActionParam ethSrcParam = new PiActionParam(FabricConstants.SMAC, SRC_MAC.toBytes());
        PiActionParam ethDstParam = new PiActionParam(FabricConstants.DMAC, DST_MAC.toBytes());
        PiActionParam portParam = new PiActionParam(FabricConstants.PORT_NUM, PORT_1.toLong());
        PiActionParam mplsParam = new PiActionParam(FabricConstants.LABEL, MPLS_10.toInt());
        PiAction expectedAction = PiAction.builder()
                .withId(FabricConstants.FABRIC_INGRESS_NEXT_MPLS_ROUTING_HASHED)
                .withParameters(ImmutableList.of(ethSrcParam, ethDstParam, portParam, mplsParam))
                .build();
        assertEquals(expectedAction, mappedAction);
    }

    /**
     * Map treatment to set_vlan_output action.
     */
    @Test
    public void testNextTreatment3() throws Exception {
        TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                .setVlanId(VLAN_100)
                .build();
        PiAction mappedAction = interpreter.mapTreatment(
                treatment, FabricConstants.FABRIC_INGRESS_NEXT_NEXT_VLAN);
        PiActionParam vlanParam = new PiActionParam(
                FabricConstants.VLAN_ID, VLAN_100.toShort());
        PiAction expectedAction = PiAction.builder()
                .withId(FabricConstants.FABRIC_INGRESS_NEXT_SET_VLAN)
                .withParameter(vlanParam)
                .build();
        assertEquals(expectedAction, mappedAction);
    }
}
