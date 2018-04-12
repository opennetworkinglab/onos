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

package org.onosproject.pipelines.fabric;

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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

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

    @Before
    public void setup() {
        interpreter = new FabricInterpreter();
    }

    /* Filtering control block */

    /**
     * Map treatment to push_internal_vlan action.
     */
    @Test
    public void testFilteringTreatment1() throws Exception {
        TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                .pushVlan()
                .setVlanId(VLAN_100)
                .build();
        PiAction mappedAction = interpreter.mapTreatment(treatment,
                                                   FabricConstants.TBL_INGRESS_PORT_VLAN_ID);
        PiActionParam param = new PiActionParam(FabricConstants.ACT_PRM_NEW_VLAN_ID_ID,
                                                ImmutableByteSequence.copyFrom(VLAN_100.toShort()));
        PiAction expectedAction = PiAction.builder()
                .withId(FabricConstants.ACT_FABRICINGRESS_FILTERING_PUSH_INTERNAL_VLAN_ID)
                .withParameter(param)
                .build();

        assertEquals(expectedAction, mappedAction);
    }

    /**
     * Map treatment to set_vlan action.
     */
    @Test
    public void testFilteringTreatment2() throws Exception {
        TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                .setVlanId(VLAN_100)
                .build();
        PiAction mappedAction = interpreter.mapTreatment(treatment,
                                                         FabricConstants.TBL_INGRESS_PORT_VLAN_ID);
        PiActionParam param = new PiActionParam(FabricConstants.ACT_PRM_NEW_VLAN_ID_ID,
                                                ImmutableByteSequence.copyFrom(VLAN_100.toShort()));
        PiAction expectedAction = PiAction.builder()
                .withId(FabricConstants.ACT_FABRICINGRESS_FILTERING_SET_VLAN_ID)
                .withParameter(param)
                .build();

        assertEquals(expectedAction, mappedAction);
    }

    /**
     * Map treatment to nop action.
     */
    @Test
    public void testFilteringTreatment3() throws Exception {
        TrafficTreatment treatment = DefaultTrafficTreatment.emptyTreatment();
        PiAction mappedAction = interpreter.mapTreatment(treatment,
                                                         FabricConstants.TBL_INGRESS_PORT_VLAN_ID);
        PiAction expectedAction = PiAction.builder()
                .withId(FabricConstants.ACT_NOP_ID)
                .build();

        assertEquals(expectedAction, mappedAction);
    }

    /* Forwarding control block */

    /**
     * Map treatment to duplicate_to_controller action.
     */
    @Test
    public void testForwardingTreatment1() throws Exception {
        TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                .punt()
                .build();
        PiAction mappedAction = interpreter.mapTreatment(treatment,
                                                         FabricConstants.TBL_ACL_ID);
        PiAction expectedAction = PiAction.builder()
                .withId(FabricConstants.ACT_FABRICINGRESS_FORWARDING_DUPLICATE_TO_CONTROLLER_ID)
                .build();

        assertEquals(expectedAction, mappedAction);
    }

    /**
     * Map empty treatment for forwarding block to nop action.
     */
    @Test
    public void testEmptyForwardingTreatment() throws Exception {
        TrafficTreatment treatment = DefaultTrafficTreatment.emptyTreatment();
        PiAction mappedAction = interpreter.mapTreatment(treatment,
                                                         FabricConstants.TBL_UNICAST_V4_ID);
        assertNull(mappedAction);
    }

    /* Next control block */

    /**
     * Map treatment to output action.
     */
    @Test
    public void testNextTreatment1() throws Exception {
        TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                .setOutput(PORT_1)
                .build();
        PiAction mappedAction = interpreter.mapTreatment(treatment,
                                                         FabricConstants.TBL_SIMPLE_ID);
        short portNumVal = (short) PORT_1.toLong();
        PiActionParam param = new PiActionParam(FabricConstants.ACT_PRM_PORT_NUM_ID,
                                                ImmutableByteSequence.copyFrom(portNumVal));
        PiAction expectedAction = PiAction.builder()
                .withId(FabricConstants.ACT_FABRICINGRESS_NEXT_OUTPUT_ID)
                .withParameter(param)
                .build();

        assertEquals(expectedAction, mappedAction);
    }

    /**
     * Map treatment to output_ecmp action.
     */
    @Test
    public void testNextTreatment2() throws Exception {
        TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                .setEthSrc(SRC_MAC)
                .setEthDst(DST_MAC)
                .setOutput(PORT_1)
                .build();
        PiAction mappedAction = interpreter.mapTreatment(treatment,
                                                         FabricConstants.TBL_HASHED_ID);
        short portNumVal = (short) PORT_1.toLong();
        PiActionParam ethSrcParam = new PiActionParam(FabricConstants.ACT_PRM_SMAC_ID,
                                                      ImmutableByteSequence.copyFrom(SRC_MAC.toBytes()));
        PiActionParam ethDstParam = new PiActionParam(FabricConstants.ACT_PRM_DMAC_ID,
                                                      ImmutableByteSequence.copyFrom(DST_MAC.toBytes()));
        PiActionParam portParam = new PiActionParam(FabricConstants.ACT_PRM_PORT_NUM_ID,
                                                ImmutableByteSequence.copyFrom(portNumVal));
        PiAction expectedAction = PiAction.builder()
                .withId(FabricConstants.ACT_FABRICINGRESS_NEXT_L3_ROUTING_ID)
                .withParameters(ImmutableList.of(ethSrcParam, ethDstParam, portParam))
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
                .setOutput(PORT_1)
                .build();
        PiAction mappedAction = interpreter.mapTreatment(treatment,
                                                         FabricConstants.TBL_SIMPLE_ID);
        short portNumVal = (short) PORT_1.toLong();
        PiActionParam portParam = new PiActionParam(FabricConstants.ACT_PRM_PORT_NUM_ID,
                                                    ImmutableByteSequence.copyFrom(portNumVal));
        short vlanVal = VLAN_100.toShort();
        PiActionParam vlanParam = new PiActionParam(FabricConstants.ACT_PRM_NEW_VLAN_ID_ID,
                                                    ImmutableByteSequence.copyFrom(vlanVal));

        PiAction expectedAction = PiAction.builder()
                .withId(FabricConstants.ACT_FABRICINGRESS_NEXT_SET_VLAN_OUTPUT_ID)
                .withParameters(ImmutableList.of(vlanParam, portParam))
                .build();

        assertEquals(expectedAction, mappedAction);
    }

    @Test
    public void testMplsRoutingTreatment() throws Exception {
        TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                .setEthDst(DST_MAC)
                .setEthSrc(SRC_MAC)
                .pushMpls()
                .copyTtlOut()
                .setMpls(MPLS_10)
                .setOutput(PORT_1)
                .build();
        PiAction mappedAction = interpreter.mapTreatment(treatment,
                                                         FabricConstants.TBL_HASHED_ID);
        short portNumVal = (short) PORT_1.toLong();
        PiActionParam ethSrcParam = new PiActionParam(FabricConstants.ACT_PRM_SMAC_ID,
                                                      ImmutableByteSequence.copyFrom(SRC_MAC.toBytes()));
        PiActionParam ethDstParam = new PiActionParam(FabricConstants.ACT_PRM_DMAC_ID,
                                                      ImmutableByteSequence.copyFrom(DST_MAC.toBytes()));
        PiActionParam portParam = new PiActionParam(FabricConstants.ACT_PRM_PORT_NUM_ID,
                                                    ImmutableByteSequence.copyFrom(portNumVal));
        ImmutableByteSequence mplsVal =
                ImmutableByteSequence.copyFrom(MPLS_10.toInt()).fit(20);
        PiActionParam mplsParam = new PiActionParam(FabricConstants.ACT_PRM_LABEL_ID, mplsVal);
        PiAction expectedAction = PiAction.builder()
                .withId(FabricConstants.ACT_FABRICINGRESS_NEXT_MPLS_ROUTING_V4_ID)
                .withParameters(ImmutableList.of(ethSrcParam, ethDstParam, portParam, mplsParam))
                .build();
        assertEquals(expectedAction, mappedAction);
    }
}
