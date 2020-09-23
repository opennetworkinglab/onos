/*
 * Copyright 2014-present Open Networking Foundation
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
package org.onosproject.net.flow.instructions;

import com.google.common.testing.EqualsTester;
import org.junit.Test;
import org.onlab.packet.EthType;
import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onlab.packet.MplsLabel;
import org.onlab.packet.TpPort;
import org.onlab.packet.VlanId;
import org.onlab.util.ImmutableByteSequence;
import org.onosproject.core.GroupId;
import org.onosproject.net.ChannelSpacing;
import org.onosproject.net.DeviceId;
import org.onosproject.net.GridType;
import org.onosproject.net.Lambda;
import org.onosproject.net.OduSignalId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.flow.StatTriggerField;
import org.onosproject.net.flow.StatTriggerFlag;
import org.onosproject.net.meter.MeterId;
import org.onosproject.net.pi.model.PiActionId;
import org.onosproject.net.pi.model.PiActionParamId;
import org.onosproject.net.pi.runtime.PiAction;
import org.onosproject.net.pi.runtime.PiActionParam;
import org.onosproject.net.pi.runtime.PiTableAction;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.onlab.junit.ImmutableClassChecker.assertThatClassIsImmutable;
import static org.onlab.junit.UtilityClassChecker.assertThatClassIsUtility;
import static org.onosproject.net.OduSignalId.oduSignalId;
import static org.onosproject.net.PortNumber.portNumber;

/**
 * Unit tests for the Instructions class.
 */
public class InstructionsTest {

    /**
     * Checks that an Instruction object has the proper type, and then converts
     * it to the proper type.
     *
     * @param instruction Instruction object to convert
     * @param type Enumerated type value for the Criterion class
     * @param clazz Desired Criterion class
     * @param <T> The type the caller wants returned
     * @return converted object
     */
    @SuppressWarnings("unchecked")
    private <T> T checkAndConvert(Instruction instruction, Instruction.Type type, Class clazz) {
        assertThat(instruction, is(notNullValue()));
        assertThat(instruction.type(), is(equalTo(type)));
        assertThat(instruction, instanceOf(clazz));
        return (T) instruction;
    }

    /**
     * Checks the equals() and toString() methods of a Instruction class.
     *
     * @param c1 first object to compare
     * @param c1match object that should be equal to the first
     * @param c2 object that should be not equal to the first
     * @param <T> type of the arguments
     */
    private <T extends Instruction> void checkEqualsAndToString(T c1, T c1match,
                                                                T c2) {

        new EqualsTester()
                .addEqualityGroup(c1, c1match)
                .addEqualityGroup(c2)
                .testEquals();
    }

    /**
     * Checks that Instructions is a proper utility class.
     */
    @Test
    public void testInstructionsUtilityClass() {
        assertThatClassIsUtility(Instructions.class);
    }

    /**
     * Checks that the Instruction class implementations are immutable.
     */
    @Test
    public void testImmutabilityOfInstructions() {
        assertThatClassIsImmutable(Instructions.OutputInstruction.class);
        assertThatClassIsImmutable(L0ModificationInstruction.ModOchSignalInstruction.class);
        assertThatClassIsImmutable(L1ModificationInstruction.ModOduSignalIdInstruction.class);
        assertThatClassIsImmutable(L2ModificationInstruction.ModEtherInstruction.class);
        assertThatClassIsImmutable(L2ModificationInstruction.ModVlanIdInstruction.class);
        assertThatClassIsImmutable(L2ModificationInstruction.ModVlanPcpInstruction.class);
        assertThatClassIsImmutable(L3ModificationInstruction.ModIPInstruction.class);
        assertThatClassIsImmutable(L3ModificationInstruction.ModIPv6FlowLabelInstruction.class);
        assertThatClassIsImmutable(L2ModificationInstruction.ModMplsLabelInstruction.class);
        assertThatClassIsImmutable(L2ModificationInstruction.ModMplsHeaderInstruction.class);
        assertThatClassIsImmutable(L2ModificationInstruction.ModMplsBosInstruction.class);
        assertThatClassIsImmutable(L2ModificationInstruction.ModMplsTtlInstruction.class);
        assertThatClassIsImmutable(L2ModificationInstruction.ModTunnelIdInstruction.class);
        assertThatClassIsImmutable(PiInstruction.class);
    }

    //  NoActionInstruction

    private final Instructions.NoActionInstruction noAction1 = Instructions.createNoAction();
    private final Instructions.NoActionInstruction noAction2 = Instructions.createNoAction();

    /**
     * Test the createNoAction method.
     */
    @Test
    public void testCreateNoActionMethod() {
        Instructions.NoActionInstruction instruction = Instructions.createNoAction();
        checkAndConvert(instruction,
                        Instruction.Type.NOACTION,
                        Instructions.NoActionInstruction.class);
    }

    /**
     * Test the equals() method of the NoActionInstruction class.
     */

    @Test
    public void testNoActionInstructionEquals() throws Exception {
        new EqualsTester()
                .addEqualityGroup(noAction1, noAction2)
                .testEquals();
    }

    /**
     * Test the hashCode() method of the NoActionInstruction class.
     */

    @Test
    public void testNoActionInstructionHashCode() {
        assertThat(noAction1.hashCode(), is(equalTo(noAction2.hashCode())));
    }

    //  OutputInstruction

    private final PortNumber port1 = portNumber(1);
    private final PortNumber port2 = portNumber(2);
    private final Instructions.OutputInstruction output1 = Instructions.createOutput(port1);
    private final Instructions.OutputInstruction sameAsOutput1 = Instructions.createOutput(port1);
    private final Instructions.OutputInstruction output2 = Instructions.createOutput(port2);

    /**
     * Test the createOutput method.
     */
    @Test
    public void testCreateOutputMethod() {
        final Instruction instruction = Instructions.createOutput(port2);
        final Instructions.OutputInstruction outputInstruction =
                checkAndConvert(instruction,
                        Instruction.Type.OUTPUT,
                        Instructions.OutputInstruction.class);
        assertThat(outputInstruction.port(), is(equalTo(port2)));
    }


    /**
     * Test the equals() method of the OutputInstruction class.
     */

    @Test
    public void testOutputInstructionEquals() throws Exception {
        checkEqualsAndToString(output1, sameAsOutput1, output2);
    }

    /**
     * Test the hashCode() method of the OutputInstruction class.
     */

    @Test
    public void testOutputInstructionHashCode() {
        assertThat(output1.hashCode(), is(equalTo(sameAsOutput1.hashCode())));
        assertThat(output1.hashCode(), is(not(equalTo(output2.hashCode()))));
    }

    //  ModLambdaInstruction

    private final Lambda och1 = Lambda.ochSignal(GridType.DWDM, ChannelSpacing.CHL_100GHZ, 4, 8);
    private final Lambda och2 = Lambda.ochSignal(GridType.CWDM, ChannelSpacing.CHL_100GHZ, 4, 8);
    private final Instruction ochInstruction1 = Instructions.modL0Lambda(och1);
    private final Instruction sameAsOchInstruction1 = Instructions.modL0Lambda(och1);
    private final Instruction ochInstruction2 = Instructions.modL0Lambda(och2);

    /**
     * Test the modL0Lambda().
     */
    @Test
    public void testModL0LambdaMethod() {
        Instruction instruction = Instructions.modL0Lambda(och1);
        L0ModificationInstruction.ModOchSignalInstruction ochInstruction =
                checkAndConvert(instruction, Instruction.Type.L0MODIFICATION,
                        L0ModificationInstruction.ModOchSignalInstruction.class);
        assertThat(ochInstruction.lambda(), is(och1));
    }

    /**
     * Test the equals() method of the ModOchSignalInstruction class.
     */
    @Test
    public void testModOchSignalInstructionEquals() {
        checkEqualsAndToString(ochInstruction1, sameAsOchInstruction1, ochInstruction2);
    }

    /**
     * Test the hashCode() method of the ModOchSignalInstruction class.
     */
    @Test
    public void testModOchSignalInstructionHashCode() {
        assertThat(ochInstruction1.hashCode(), is(sameAsOchInstruction1.hashCode()));
        assertThat(ochInstruction1.hashCode(), is(not(ochInstruction2.hashCode())));
    }

    //  ModOduSignalIdInstruction

    private final OduSignalId odu1 = oduSignalId(1, 80, new byte[] {8, 7, 6, 5, 7, 6, 5, 7, 6, 5});
    private final OduSignalId odu2 = oduSignalId(2, 80, new byte[] {1, 1, 2, 2, 1, 2, 2, 1, 2, 2});
    private final Instruction oduInstruction1 = Instructions.modL1OduSignalId(odu1);
    private final Instruction sameAsOduInstruction1 = Instructions.modL1OduSignalId(odu1);
    private final Instruction oduInstruction2 = Instructions.modL1OduSignalId(odu2);

    /**
     * Test the modL1OduSignalId().
     */
    @Test
    public void testModL1OduSignalIdMethod() {
        Instruction instruction = Instructions.modL1OduSignalId(odu1);
        L1ModificationInstruction.ModOduSignalIdInstruction oduInstruction =
                checkAndConvert(instruction, Instruction.Type.L1MODIFICATION,
                        L1ModificationInstruction.ModOduSignalIdInstruction.class);
        assertThat(oduInstruction.oduSignalId(), is(odu1));
    }

    /**
     * Test the equals() method of the ModOduSignalInstruction class.
     */
    @Test
    public void testModOduSignalIdInstructionEquals() {
        checkEqualsAndToString(oduInstruction1, sameAsOduInstruction1, oduInstruction2);
    }

    /**
     * Test the hashCode() method of the ModOduSignalInstruction class.
     */
    @Test
    public void testModOduSignalIdInstructionHashCode() {
        assertThat(oduInstruction1.hashCode(), is(sameAsOduInstruction1.hashCode()));
        assertThat(oduInstruction1.hashCode(), is(not(oduInstruction2.hashCode())));
    }


    //  ModEtherInstruction

    private static final String MAC1 = "00:00:00:00:00:01";
    private static final String MAC2 = "00:00:00:00:00:02";
    private final MacAddress mac1 = MacAddress.valueOf(MAC1);
    private final MacAddress mac2 = MacAddress.valueOf(MAC2);
    private final Instruction modEtherInstruction1 = Instructions.modL2Src(mac1);
    private final Instruction sameAsModEtherInstruction1 = Instructions.modL2Src(mac1);
    private final Instruction modEtherInstruction2 = Instructions.modL2Src(mac2);

    /**
     * Test the modL2Src method.
     */
    @Test
    public void testModL2SrcMethod() {
        final Instruction instruction = Instructions.modL2Src(mac1);
        final L2ModificationInstruction.ModEtherInstruction modEtherInstruction =
                checkAndConvert(instruction,
                        Instruction.Type.L2MODIFICATION,
                        L2ModificationInstruction.ModEtherInstruction.class);
        assertThat(modEtherInstruction.mac(), is(equalTo(mac1)));
        assertThat(modEtherInstruction.subtype(),
                is(equalTo(L2ModificationInstruction.L2SubType.ETH_SRC)));
    }

    /**
     * Test the modL2Dst method.
     */
    @Test
    public void testModL2DstMethod() {
        final Instruction instruction = Instructions.modL2Dst(mac1);
        final L2ModificationInstruction.ModEtherInstruction modEtherInstruction =
                checkAndConvert(instruction,
                        Instruction.Type.L2MODIFICATION,
                        L2ModificationInstruction.ModEtherInstruction.class);
        assertThat(modEtherInstruction.mac(), is(equalTo(mac1)));
        assertThat(modEtherInstruction.subtype(),
                is(equalTo(L2ModificationInstruction.L2SubType.ETH_DST)));
    }

    /**
     * Test the equals() method of the ModEtherInstruction class.
     */

    @Test
    public void testModEtherInstructionEquals() throws Exception {
        checkEqualsAndToString(modEtherInstruction1,
                               sameAsModEtherInstruction1,
                               modEtherInstruction2);
    }

    /**
     * Test the hashCode() method of the ModEtherInstruction class.
     */

    @Test
    public void testModEtherInstructionHashCode() {
        assertThat(modEtherInstruction1.hashCode(),
                   is(equalTo(sameAsModEtherInstruction1.hashCode())));
        assertThat(modEtherInstruction1.hashCode(),
                   is(not(equalTo(modEtherInstruction2.hashCode()))));
    }


    //  ModVlanIdInstruction

    private final short vlan1 = 1;
    private final short vlan2 = 2;
    private final VlanId vlanId1 = VlanId.vlanId(vlan1);
    private final VlanId vlanId2 = VlanId.vlanId(vlan2);
    private final Instruction modVlanId1 = Instructions.modVlanId(vlanId1);
    private final Instruction sameAsModVlanId1 = Instructions.modVlanId(vlanId1);
    private final Instruction modVlanId2 = Instructions.modVlanId(vlanId2);

    /**
     * Test the modVlanId method.
     */
    @Test
    public void testModVlanIdMethod() {
        final Instruction instruction = Instructions.modVlanId(vlanId1);
        final L2ModificationInstruction.ModVlanIdInstruction modEtherInstruction =
                checkAndConvert(instruction,
                        Instruction.Type.L2MODIFICATION,
                        L2ModificationInstruction.ModVlanIdInstruction.class);
        assertThat(modEtherInstruction.vlanId(), is(equalTo(vlanId1)));
        assertThat(modEtherInstruction.subtype(),
                is(equalTo(L2ModificationInstruction.L2SubType.VLAN_ID)));
    }

    /**
     * Test the equals() method of the ModVlanIdInstruction class.
     */

    @Test
    public void testModVlanIdInstructionEquals() throws Exception {
        checkEqualsAndToString(modVlanId1,
                               sameAsModVlanId1,
                               modVlanId2);
    }

    /**
     * Test the hashCode() method of the ModEtherInstruction class.
     */

    @Test
    public void testModVlanIdInstructionHashCode() {
        assertThat(modVlanId1.hashCode(),
                is(equalTo(sameAsModVlanId1.hashCode())));
        assertThat(modVlanId1.hashCode(),
                is(not(equalTo(modVlanId2.hashCode()))));
    }


    //  ModVlanPcpInstruction

    private final byte vlanPcp1 = 1;
    private final byte vlanPcp2 = 2;
    private final Instruction modVlanPcp1 = Instructions.modVlanPcp(vlanPcp1);
    private final Instruction sameAsModVlanPcp1 = Instructions.modVlanPcp(vlanPcp1);
    private final Instruction modVlanPcp2 = Instructions.modVlanPcp(vlanPcp2);

    /**
     * Test the modVlanPcp method.
     */
    @Test
    public void testModVlanPcpMethod() {
        final Instruction instruction = Instructions.modVlanPcp(vlanPcp1);
        final L2ModificationInstruction.ModVlanPcpInstruction modEtherInstruction =
                checkAndConvert(instruction,
                        Instruction.Type.L2MODIFICATION,
                        L2ModificationInstruction.ModVlanPcpInstruction.class);
        assertThat(modEtherInstruction.vlanPcp(), is(equalTo(vlanPcp1)));
        assertThat(modEtherInstruction.subtype(),
                is(equalTo(L2ModificationInstruction.L2SubType.VLAN_PCP)));
    }

    /**
     * Test the equals() method of the ModVlanPcpInstruction class.
     */

    @Test
    public void testModVlanPcpInstructionEquals() throws Exception {
        checkEqualsAndToString(modVlanPcp1,
                               sameAsModVlanPcp1,
                               modVlanPcp2);
    }

    /**
     * Test the hashCode() method of the ModVlanPcp class.
     */

    @Test
    public void testModVlanPcpInstructionHashCode() {
        assertThat(modVlanPcp1.hashCode(),
                is(equalTo(sameAsModVlanPcp1.hashCode())));
        assertThat(modVlanPcp1.hashCode(),
                is(not(equalTo(modVlanPcp2.hashCode()))));
    }

    //  ModIPInstruction

    private static final String IP41 = "1.2.3.4";
    private static final String IP42 = "5.6.7.8";
    private IpAddress ip41 = IpAddress.valueOf(IP41);
    private IpAddress ip42 = IpAddress.valueOf(IP42);
    private final Instruction modIPInstruction1 = Instructions.modL3Src(ip41);
    private final Instruction sameAsModIPInstruction1 = Instructions.modL3Src(ip41);
    private final Instruction modIPInstruction2 = Instructions.modL3Src(ip42);

    private static final String IP61 = "1111::2222";
    private static final String IP62 = "3333::4444";
    private IpAddress ip61 = IpAddress.valueOf(IP61);
    private IpAddress ip62 = IpAddress.valueOf(IP62);
    private final Instruction modIPv6Instruction1 =
        Instructions.modL3IPv6Src(ip61);
    private final Instruction sameAsModIPv6Instruction1 =
        Instructions.modL3IPv6Src(ip61);
    private final Instruction modIPv6Instruction2 =
        Instructions.modL3IPv6Src(ip62);

    /**
     * Test the modL3Src method.
     */
    @Test
    public void testModL3SrcMethod() {
        final Instruction instruction = Instructions.modL3Src(ip41);
        final L3ModificationInstruction.ModIPInstruction modIPInstruction =
                checkAndConvert(instruction,
                        Instruction.Type.L3MODIFICATION,
                        L3ModificationInstruction.ModIPInstruction.class);
        assertThat(modIPInstruction.ip(), is(equalTo(ip41)));
        assertThat(modIPInstruction.subtype(),
                is(equalTo(L3ModificationInstruction.L3SubType.IPV4_SRC)));
    }

    /**
     * Test the modL3Dst method.
     */
    @Test
    public void testModL3DstMethod() {
        final Instruction instruction = Instructions.modL3Dst(ip41);
        final L3ModificationInstruction.ModIPInstruction modIPInstruction =
                checkAndConvert(instruction,
                        Instruction.Type.L3MODIFICATION,
                        L3ModificationInstruction.ModIPInstruction.class);
        assertThat(modIPInstruction.ip(), is(equalTo(ip41)));
        assertThat(modIPInstruction.subtype(),
                is(equalTo(L3ModificationInstruction.L3SubType.IPV4_DST)));
    }

    /**
     * Test the modL3IPv6Src method.
     */
    @Test
    public void testModL3IPv6SrcMethod() {
        final Instruction instruction = Instructions.modL3IPv6Src(ip61);
        final L3ModificationInstruction.ModIPInstruction modIPInstruction =
                checkAndConvert(instruction,
                        Instruction.Type.L3MODIFICATION,
                        L3ModificationInstruction.ModIPInstruction.class);
        assertThat(modIPInstruction.ip(), is(equalTo(ip61)));
        assertThat(modIPInstruction.subtype(),
                is(equalTo(L3ModificationInstruction.L3SubType.IPV6_SRC)));
    }

    /**
     * Test the modL3IPv6Dst method.
     */
    @Test
    public void testModL3IPv6DstMethod() {
        final Instruction instruction = Instructions.modL3IPv6Dst(ip61);
        final L3ModificationInstruction.ModIPInstruction modIPInstruction =
                checkAndConvert(instruction,
                        Instruction.Type.L3MODIFICATION,
                        L3ModificationInstruction.ModIPInstruction.class);
        assertThat(modIPInstruction.ip(), is(equalTo(ip61)));
        assertThat(modIPInstruction.subtype(),
                is(equalTo(L3ModificationInstruction.L3SubType.IPV6_DST)));
    }

    /**
     * Test the equals() method of the ModIPInstruction class.
     */
    @Test
    public void testModIPInstructionEquals() throws Exception {
        checkEqualsAndToString(modIPInstruction1,
                               sameAsModIPInstruction1,
                               modIPInstruction2);
    }

    /**
     * Test the hashCode() method of the ModIPInstruction class.
     */
    @Test
    public void testModIPInstructionHashCode() {
        assertThat(modIPInstruction1.hashCode(),
                   is(equalTo(sameAsModIPInstruction1.hashCode())));
        assertThat(modIPInstruction1.hashCode(),
                   is(not(equalTo(modIPInstruction2.hashCode()))));
    }

    private final int flowLabel1 = 0x11111;
    private final int flowLabel2 = 0x22222;
    private final Instruction modIPv6FlowLabelInstruction1 =
        Instructions.modL3IPv6FlowLabel(flowLabel1);
    private final Instruction sameAsModIPv6FlowLabelInstruction1 =
        Instructions.modL3IPv6FlowLabel(flowLabel1);
    private final Instruction modIPv6FlowLabelInstruction2 =
        Instructions.modL3IPv6FlowLabel(flowLabel2);

    /**
     * Test the modL3IPv6FlowLabel method.
     */
    @Test
    public void testModL3IPv6FlowLabelMethod() {
        final Instruction instruction =
            Instructions.modL3IPv6FlowLabel(flowLabel1);
        final L3ModificationInstruction.ModIPv6FlowLabelInstruction
            modIPv6FlowLabelInstruction =
                checkAndConvert(instruction,
                        Instruction.Type.L3MODIFICATION,
                        L3ModificationInstruction.ModIPv6FlowLabelInstruction.class);
        assertThat(modIPv6FlowLabelInstruction.flowLabel(),
                   is(equalTo(flowLabel1)));
        assertThat(modIPv6FlowLabelInstruction.subtype(),
                is(equalTo(L3ModificationInstruction.L3SubType.IPV6_FLABEL)));
    }

    /**
     * Test the equals() method of the ModIPv6FlowLabelInstruction class.
     */
    @Test
    public void testModIPv6FlowLabelInstructionEquals() throws Exception {
        checkEqualsAndToString(modIPv6FlowLabelInstruction1,
                               sameAsModIPv6FlowLabelInstruction1,
                               modIPv6FlowLabelInstruction2);
    }

    /**
     * Test the hashCode() method of the ModIPv6FlowLabelInstruction class.
     */
    @Test
    public void testModIPv6FlowLabelInstructionHashCode() {
        assertThat(modIPv6FlowLabelInstruction1.hashCode(),
                   is(equalTo(sameAsModIPv6FlowLabelInstruction1.hashCode())));
        assertThat(modIPv6FlowLabelInstruction1.hashCode(),
                   is(not(equalTo(modIPv6FlowLabelInstruction2.hashCode()))));
    }

    private Instruction modMplsLabelInstruction1 = Instructions.modMplsLabel(MplsLabel.mplsLabel(1));
    private Instruction sameAsModMplsLabelInstruction1 = Instructions.modMplsLabel(MplsLabel.mplsLabel(1));
    private Instruction modMplsLabelInstruction2 = Instructions.modMplsLabel(MplsLabel.mplsLabel(2));

    /**
     * Test the modMplsLabel method.
     */
    @Test
    public void testModMplsMethod() {
        final MplsLabel mplsLabel = MplsLabel.mplsLabel(33);
        final Instruction instruction = Instructions.modMplsLabel(mplsLabel);
        final L2ModificationInstruction.ModMplsLabelInstruction modMplsLabelInstruction =
                checkAndConvert(instruction,
                        Instruction.Type.L2MODIFICATION,
                        L2ModificationInstruction.ModMplsLabelInstruction.class);
        assertThat(modMplsLabelInstruction.label(), is(equalTo(mplsLabel)));
        assertThat(modMplsLabelInstruction.subtype(),
                is(equalTo(L2ModificationInstruction.L2SubType.MPLS_LABEL)));
    }

    /**
     * Test the equals(), hashCode() and toString() methods of the
     * ModMplsLabelInstruction class.
     */
    @Test
    public void testModMplsLabelInstructionEquals() throws Exception {
        checkEqualsAndToString(modMplsLabelInstruction1,
                sameAsModMplsLabelInstruction1,
                modMplsLabelInstruction2);
    }

    // ModTunnelIdInstruction

    private final long tunnelId1 = 1L;
    private final long tunnelId2 = 2L;
    private final Instruction modTunnelId1 = Instructions.modTunnelId(tunnelId1);
    private final Instruction sameAsModTunnelId1 = Instructions.modTunnelId(tunnelId1);
    private final Instruction modTunnelId2 = Instructions.modTunnelId(tunnelId2);

    /**
     * Test the modTunnelId method.
     */
    @Test
    public void testModTunnelIdMethod() {
        final Instruction instruction = Instructions.modTunnelId(tunnelId1);
        final L2ModificationInstruction.ModTunnelIdInstruction modTunnelIdInstruction =
                checkAndConvert(instruction, Instruction.Type.L2MODIFICATION,
                                L2ModificationInstruction.ModTunnelIdInstruction.class);
        assertThat(modTunnelIdInstruction.tunnelId(), is(equalTo(tunnelId1)));
        assertThat(modTunnelIdInstruction.subtype(),
                   is(equalTo(L2ModificationInstruction.L2SubType.TUNNEL_ID)));
    }

    /***
     * Test the equals() method of the ModTunnelIdInstruction class.
     */
    @Test
    public void testModTunnelIdInstructionEquals() throws Exception {
        checkEqualsAndToString(modTunnelId1, sameAsModTunnelId1, modTunnelId2);
    }

    /**
     * Test the hashCode() method of the ModTunnelIdInstruction class.
     */
    @Test
    public void testModTunnelIdInstructionHashCode() {
        assertThat(modTunnelId1.hashCode(), is(equalTo(sameAsModTunnelId1.hashCode())));
        assertThat(modTunnelId1.hashCode(), is(not(equalTo(modTunnelId2.hashCode()))));
    }

    // ModTransportPortInstruction

    private final TpPort tpPort1 = TpPort.tpPort(1);
    private final TpPort tpPort2 = TpPort.tpPort(2);
    private final Instruction modTransportPortInstruction1 = Instructions.modTcpSrc(tpPort1);
    private final Instruction sameAsModTransportPortInstruction1 = Instructions.modTcpSrc(tpPort1);
    private final Instruction modTransportPortInstruction2 = Instructions.modTcpSrc(tpPort2);

    /**
     * Test the modTcpSrc() method.
     */
    @Test
    public void testModTcpSrcMethod() {
        final Instruction instruction = Instructions.modTcpSrc(tpPort1);
        final L4ModificationInstruction.ModTransportPortInstruction modTransportPortInstruction =
                checkAndConvert(instruction, Instruction.Type.L4MODIFICATION,
                                L4ModificationInstruction.ModTransportPortInstruction.class);
        assertThat(modTransportPortInstruction.port(), is(equalTo(tpPort1)));
        assertThat(modTransportPortInstruction.subtype(),
                   is(equalTo(L4ModificationInstruction.L4SubType.TCP_SRC)));
    }

    /**
     * Test the modTcpDst() method.
     */
    @Test
    public void testModTcpDstMethod() {
        final Instruction instruction = Instructions.modTcpDst(tpPort1);
        final L4ModificationInstruction.ModTransportPortInstruction modTransportPortInstruction =
                checkAndConvert(instruction, Instruction.Type.L4MODIFICATION,
                                L4ModificationInstruction.ModTransportPortInstruction.class);
        assertThat(modTransportPortInstruction.port(), is(equalTo(tpPort1)));
        assertThat(modTransportPortInstruction.subtype(),
                   is(equalTo(L4ModificationInstruction.L4SubType.TCP_DST)));
    }

    /**
     * Test the modUdpSrc() method.
     */
    @Test
    public void testModUdpSrcMethod() {
        final Instruction instruction = Instructions.modUdpSrc(tpPort1);
        final L4ModificationInstruction.ModTransportPortInstruction modTransportPortInstruction =
                checkAndConvert(instruction, Instruction.Type.L4MODIFICATION,
                                L4ModificationInstruction.ModTransportPortInstruction.class);
        assertThat(modTransportPortInstruction.port(), is(equalTo(tpPort1)));
        assertThat(modTransportPortInstruction.subtype(),
                   is(equalTo(L4ModificationInstruction.L4SubType.UDP_SRC)));
    }

    /**
     * Test the modUdpDst() method.
     */
    @Test
    public void testModUdpDstMethod() {
        final Instruction instruction = Instructions.modUdpDst(tpPort1);
        final L4ModificationInstruction.ModTransportPortInstruction modTransportPortInstruction =
                checkAndConvert(instruction, Instruction.Type.L4MODIFICATION,
                                L4ModificationInstruction.ModTransportPortInstruction.class);
        assertThat(modTransportPortInstruction.port(), is(equalTo(tpPort1)));
        assertThat(modTransportPortInstruction.subtype(),
                   is(equalTo(L4ModificationInstruction.L4SubType.UDP_DST)));
    }

    /**
     * Test the equals() method of the ModTransportPortInstruction class.
     */
    @Test
    public void testModTransportPortInstructionEquals() throws Exception {
        checkEqualsAndToString(modTransportPortInstruction1,
                               sameAsModTransportPortInstruction1,
                               modTransportPortInstruction2);
    }

    /**
     * Test the hashCode() method of the ModTransportPortInstruction class.
     */
    @Test
    public void testModTransportPortInstructionHashCode() {
        assertThat(modTransportPortInstruction1.hashCode(),
                   is(equalTo(sameAsModTransportPortInstruction1.hashCode())));
        assertThat(modTransportPortInstruction1.hashCode(),
                   is(not(equalTo(modTransportPortInstruction2.hashCode()))));
    }

    //  GroupInstruction

    private final GroupId groupId1 = new GroupId(1);
    private final GroupId groupId2 = new GroupId(2);
    private final Instruction groupInstruction1 = Instructions.createGroup(groupId1);
    private final Instruction sameAsGroupInstruction1 = Instructions.createGroup(groupId1);
    private final Instruction groupInstruction2 = Instructions.createGroup(groupId2);

    /**
     * Test the create group method.
     */
    @Test
    public void testCreateGroupMethod() {
        final Instruction instruction = Instructions.createGroup(groupId1);
        final Instructions.GroupInstruction groupInstruction =
                checkAndConvert(instruction,
                                Instruction.Type.GROUP,
                                Instructions.GroupInstruction.class);
        assertThat(groupInstruction.groupId(), is(equalTo(groupId1)));
    }

    /**
     * Test the equals() method of the GroupInstruction class.
     */

    @Test
    public void testGroupInstructionEquals() {
        checkEqualsAndToString(groupInstruction1,
                               sameAsGroupInstruction1,
                               groupInstruction2);
    }

    //  SetQueueInstruction

    private final Instruction setQueueInstruction1 = Instructions.setQueue(1, port1);
    private final Instruction sameAsSetQueueInstruction1 = Instructions.setQueue(1, port1);
    private final Instruction setQueueInstruction2 = Instructions.setQueue(1, port2);

    /**
     * Test the set queue method.
     */
    @Test
    public void testSetQueueMethod() {
        final Instruction instruction = Instructions.setQueue(2, port2);
        final Instructions.SetQueueInstruction setQueueInstruction =
                checkAndConvert(instruction,
                                Instruction.Type.QUEUE,
                                Instructions.SetQueueInstruction.class);
        assertThat(setQueueInstruction.queueId(), is(2L));
        assertThat(setQueueInstruction.port(), is(port2));
    }

    /**
     * Test the equals() method of the SetQueueInstruction class.
     */
    @Test
    public void testSetQueueInstructionEquals() {
        checkEqualsAndToString(setQueueInstruction1,
                               sameAsSetQueueInstruction1,
                               setQueueInstruction2);
    }

    //  MeterInstruction

    MeterId meterId1 = MeterId.meterId(1);
    MeterId meterId2 = MeterId.meterId(2);
    private final Instruction meterInstruction1 = Instructions.meterTraffic(meterId1);
    private final Instruction sameAsMeterInstruction1 = Instructions.meterTraffic(meterId1);
    private final Instruction meterInstruction2 = Instructions.meterTraffic(meterId2);

    /**
     * Test the meter traffic method.
     */
    @Test
    public void testMeterTrafficMethod() {
        final Instruction instruction = Instructions.meterTraffic(meterId1);
        final Instructions.MeterInstruction meterInstruction =
                checkAndConvert(instruction,
                                Instruction.Type.METER,
                                Instructions.MeterInstruction.class);
        assertThat(meterInstruction.meterId(), is(meterId1));
    }

    /**
     * Test the equals() method of the MeterInstruction class.
     */
    @Test
    public void testMeterTrafficInstructionEquals() {
        checkEqualsAndToString(meterInstruction1,
                               sameAsMeterInstruction1,
                               meterInstruction2);
    }

    private long packetCountValue1 = 5L;
    private long byteCountValue1 = 10L;
    private long packetCountValue2 = 10L;
    private long byteCountValue2 = 5L;
    private StatTriggerFlag flag1 = StatTriggerFlag.ONLY_FIRST;
    private StatTriggerFlag flag2 = StatTriggerFlag.PERIODIC;
    Map<StatTriggerField, Long> statTriggerFieldMap1 = new EnumMap<StatTriggerField, Long>(StatTriggerField.class) {
        {
            put(StatTriggerField.BYTE_COUNT, packetCountValue1);
            put(StatTriggerField.PACKET_COUNT, byteCountValue1);
        }
    };
    Map<StatTriggerField, Long> statTriggerFieldMap2 = new EnumMap<StatTriggerField, Long>(StatTriggerField.class) {
        {
            put(StatTriggerField.BYTE_COUNT, packetCountValue2);
            put(StatTriggerField.PACKET_COUNT, byteCountValue2);
        }
    };

    final Instruction statInstruction1 = Instructions.statTrigger(statTriggerFieldMap1, flag1);
    final Instruction statInstruction1Same = Instructions.statTrigger(statTriggerFieldMap1, flag1);
    final Instruction statInstruction2 = Instructions.statTrigger(statTriggerFieldMap2, flag2);

    @Test
    public void testStatTriggerTrafficMethod() {
        final Instruction instruction = Instructions.statTrigger(statTriggerFieldMap1, flag1);
        final Instructions.StatTriggerInstruction statTriggerInstruction =
                checkAndConvert(instruction,
                        Instruction.Type.STAT_TRIGGER,
                        Instructions.StatTriggerInstruction.class);
        assertThat(statTriggerInstruction.getStatTriggerFieldMap(), is(equalTo(statTriggerFieldMap1)));
        assertThat(statTriggerInstruction.getStatTriggerFlag(), is(equalTo(flag1)));
        assertThat(statTriggerInstruction.getStatTriggerFieldMap(), is(not(equalTo(statTriggerFieldMap2))));
        assertThat(statTriggerInstruction.getStatTriggerFlag(), is(not(equalTo(flag2))));
    }

    @Test
    public void testStatTriggerTrafficInstructionEquals() {
        checkEqualsAndToString(statInstruction1,
                statInstruction1Same,
                statInstruction2);
    }

    //  TableTypeTransition

    private final Instruction transitionInstruction1 = Instructions.transition(1);
    private final Instruction sameAsTransitionInstruction1 = Instructions.transition(1);
    private final Instruction transitionInstruction2 = Instructions.transition(2);

    /**
     * Test the transition method.
     */
    @Test
    public void testTransitionMethod() {
        final Instruction instruction = Instructions.transition(1);
        final Instructions.TableTypeTransition tableInstruction =
                checkAndConvert(instruction,
                                Instruction.Type.TABLE,
                                Instructions.TableTypeTransition.class);
        assertThat(tableInstruction.tableId(), is(1));
    }

    /**
     * Test the equals() method of the TableTypeTransition class.
     */
    @Test
    public void testTableTypeTransitionInstructionEquals() {
        checkEqualsAndToString(transitionInstruction1,
                               sameAsTransitionInstruction1,
                               transitionInstruction2);
    }

    //  MetadataInstruction

    long metadata1 = 111L;
    long metadataMask1 = 222L;
    long metadata2 = 333L;
    long metadataMask2 = 444L;

    private final Instruction metadataInstruction1 =
            Instructions.writeMetadata(metadata1, metadataMask1);
    private final Instruction sameAsMetadataInstruction1 =
            Instructions.writeMetadata(metadata1, metadataMask1);
    private final Instruction metadataInstruction2 =
            Instructions.writeMetadata(metadata2, metadataMask2);

    /**
     * Test the write metadata method.
     */
    @Test
    public void testWriteMetadataMethod() {
        final Instruction instruction =
                Instructions.writeMetadata(metadata1, metadataMask1);
        final Instructions.MetadataInstruction metadataInstruction =
                checkAndConvert(instruction,
                                Instruction.Type.METADATA,
                                Instructions.MetadataInstruction.class);
        assertThat(metadataInstruction.metadata(), is(metadata1));
        assertThat(metadataInstruction.metadataMask(), is(metadataMask1));
    }

    /**
     * Test the equals() method of the MetadataInstruction class.
     */
    @Test
    public void testInstructionEquals() {
        checkEqualsAndToString(metadataInstruction1,
                               sameAsMetadataInstruction1,
                               metadataInstruction2);
    }

    //  ExtensionInstructionWrapper

    class MockExtensionTreatment implements ExtensionTreatment {
        int type;

        MockExtensionTreatment(int type) {
            this.type = type;
        }
        @Override
        public ExtensionTreatmentType type() {
            return new ExtensionTreatmentType(type);
        }

        @Override
        public <T> void setPropertyValue(String key, T value) throws ExtensionPropertyException {

        }

        @Override
        public <T> T getPropertyValue(String key) throws ExtensionPropertyException {
            return null;
        }

        @Override
        public List<String> getProperties() {
            return null;
        }

        @Override
        public byte[] serialize() {
            return new byte[0];
        }

        @Override
        public void deserialize(byte[] data) {

        }
    }

    ExtensionTreatment extensionTreatment1 = new MockExtensionTreatment(111);
    ExtensionTreatment extensionTreatment2 = new MockExtensionTreatment(222);

    DeviceId deviceId1 = DeviceId.deviceId("of:1");
    DeviceId deviceId2 = DeviceId.deviceId("of:2");

    private final Instruction extensionInstruction1 =
            Instructions.extension(extensionTreatment1, deviceId1);
    private final Instruction sameAsExtensionInstruction1 =
            Instructions.extension(extensionTreatment1, deviceId1);
    private final Instruction extensionInstruction2 =
            Instructions.extension(extensionTreatment2, deviceId2);

    /**
     * Test the extension method.
     */
    @Test
    public void testExtensionMethod() {
        final Instruction instruction =
                Instructions.extension(extensionTreatment1, deviceId1);
        final Instructions.ExtensionInstructionWrapper extensionInstructionWrapper =
                checkAndConvert(instruction,
                                Instruction.Type.EXTENSION,
                                Instructions.ExtensionInstructionWrapper.class);
        assertThat(extensionInstructionWrapper.deviceId(), is(deviceId1));
        assertThat(extensionInstructionWrapper.extensionInstruction(), is(extensionTreatment1));
    }

    /**
     * Test the equals() method of the ExtensionInstructionWrapper class.
     */
    @Test
    public void testExtensionInstructionWrapperEquals() {
        checkEqualsAndToString(extensionInstruction1,
                               sameAsExtensionInstruction1,
                               extensionInstruction2);
    }

    //  ModMplsHeaderInstructions

    private final EthType ethType1 = new EthType(1);
    private final EthType ethType2 = new EthType(2);
    private final Instruction modMplsHeaderInstruction1 = Instructions.popMpls(ethType1);
    private final Instruction sameAsModMplsHeaderInstruction1 = Instructions.popMpls(ethType1);
    private final Instruction modMplsHeaderInstruction2 = Instructions.popMpls(ethType2);

    /**
     * Test the pushMpls method.
     */
    @Test
    public void testPushMplsMethod() {
        final Instruction instruction = Instructions.pushMpls();
        final L2ModificationInstruction.ModMplsHeaderInstruction pushHeaderInstruction =
                checkAndConvert(instruction,
                                Instruction.Type.L2MODIFICATION,
                                L2ModificationInstruction.ModMplsHeaderInstruction.class);
        assertThat(pushHeaderInstruction.ethernetType().toString(),
                   is(EthType.EtherType.MPLS_UNICAST.toString()));
        assertThat(pushHeaderInstruction.subtype(),
                   is(L2ModificationInstruction.L2SubType.MPLS_PUSH));
    }

    /**
     * Test the popMpls method.
     */
    @Test
    public void testPopMplsMethod() {
        final Instruction instruction = Instructions.popMpls();
        final L2ModificationInstruction.ModMplsHeaderInstruction pushHeaderInstruction =
                checkAndConvert(instruction,
                                Instruction.Type.L2MODIFICATION,
                                L2ModificationInstruction.ModMplsHeaderInstruction.class);
        assertThat(pushHeaderInstruction.ethernetType().toString(),
                   is(EthType.EtherType.MPLS_UNICAST.toString()));
        assertThat(pushHeaderInstruction.subtype(),
                   is(L2ModificationInstruction.L2SubType.MPLS_POP));
    }

    /**
     * Test the popMpls(EtherType) method.
     */
    @Test
    public void testPopMplsEthertypeMethod() {
        final Instruction instruction = Instructions.popMpls(new EthType(1));
        final L2ModificationInstruction.ModMplsHeaderInstruction pushHeaderInstruction =
                checkAndConvert(instruction,
                                Instruction.Type.L2MODIFICATION,
                                L2ModificationInstruction.ModMplsHeaderInstruction.class);
        assertThat(pushHeaderInstruction.ethernetType().toShort(), is((short) 1));
        assertThat(pushHeaderInstruction.subtype(),
                   is(L2ModificationInstruction.L2SubType.MPLS_POP));
    }

    /**
     * Test the pushVlan method.
     */
    @Test
    public void testPushVlanMethod() {
        final Instruction instruction = Instructions.pushVlan();
        final L2ModificationInstruction.ModVlanHeaderInstruction pushHeaderInstruction =
                checkAndConvert(instruction,
                                Instruction.Type.L2MODIFICATION,
                                L2ModificationInstruction.ModVlanHeaderInstruction.class);
        assertThat(pushHeaderInstruction.ethernetType().toString(),
                   is(EthType.EtherType.VLAN.toString()));
        assertThat(pushHeaderInstruction.subtype(),
                   is(L2ModificationInstruction.L2SubType.VLAN_PUSH));
    }

    /**
     * Tests the equals(), hashCode() and toString() methods of the
     * ModMplsHeaderInstructions class.
     */

    @Test
    public void testModMplsHeaderInstructionsEquals() {
        checkEqualsAndToString(modMplsHeaderInstruction1,
                sameAsModMplsHeaderInstruction1,
                modMplsHeaderInstruction2);
    }

    //  ModMplsTtlInstruction

    private final Instruction modMplsTtlInstruction1 = Instructions.decMplsTtl();
    private final Instruction sameAsModMplsTtlInstruction1 = Instructions.decMplsTtl();

    /**
     * Test the modMplsBos() method.
     */
    @Test
    public void testDecMplsTtlMethod() {
        final Instruction instruction = Instructions.decMplsTtl();
        final L2ModificationInstruction.ModMplsTtlInstruction modMplsTtlInstruction =
                checkAndConvert(instruction,
                                Instruction.Type.L2MODIFICATION,
                                L2ModificationInstruction.ModMplsTtlInstruction.class);
        assertThat(modMplsTtlInstruction.subtype(),
                   is(L2ModificationInstruction.L2SubType.DEC_MPLS_TTL));
    }

    /**
     * Tests the equals(), hashCode() and toString() methods of the
     * ModMplsTtlInstructions class.
     */

    @Test
    public void testMplsTtlInstructionsEquals() {
        new EqualsTester()
                .addEqualityGroup(modMplsTtlInstruction1, sameAsModMplsTtlInstruction1)
                .testEquals();
    }

    //  ModMplsBosInstruction

    private final Instruction modMplsBosInstruction1 = Instructions.modMplsBos(true);
    private final Instruction sameAsModMplsBosInstruction1 = Instructions.modMplsBos(true);
    private final Instruction modMplsBosInstruction2 = Instructions.modMplsBos(false);

    /**
     * Test the modMplsBos() method.
     */
    @Test
    public void testModMplsBosMethod() {
        final Instruction instruction = Instructions.modMplsBos(true);
        final L2ModificationInstruction.ModMplsBosInstruction modMplsBosInstruction =
                checkAndConvert(instruction,
                                Instruction.Type.L2MODIFICATION,
                                L2ModificationInstruction.ModMplsBosInstruction.class);
        assertThat(modMplsBosInstruction.subtype(),
                   is(L2ModificationInstruction.L2SubType.MPLS_BOS));
        assertThat(modMplsBosInstruction.mplsBos(), is(true));
    }

    /**
     * Tests the equals(), hashCode() and toString() methods of the
     * ModMplsBosInstructions class.
     */

    @Test
    public void testMplsBosInstructionsEquals() {
        checkEqualsAndToString(modMplsBosInstruction1,
                               sameAsModMplsBosInstruction1,
                               modMplsBosInstruction2);
    }

    //  ModVlanHeaderInstruction

    private final Instruction modVlanHeaderInstruction1 = Instructions.popVlan();
    private final Instruction sameAsModVlanHeaderInstruction1 = Instructions.popVlan();

    /**
     * Test the popVlan method.
     */
    @Test
    public void testPopVlanMethod() {
        final Instruction instruction = Instructions.popVlan();
        final L2ModificationInstruction.ModVlanHeaderInstruction popVlanInstruction =
                checkAndConvert(instruction,
                                Instruction.Type.L2MODIFICATION,
                                L2ModificationInstruction.ModVlanHeaderInstruction.class);
        assertThat(popVlanInstruction.subtype(),
                   is(L2ModificationInstruction.L2SubType.VLAN_POP));
    }

    /**
     * Tests the equals(), hashCode() and toString() methods of the
     * ModVlanHeaderInstructions class.
     */

    @Test
    public void testModVlanHeaderInstructionsEquals() {
        new EqualsTester()
                .addEqualityGroup(modVlanHeaderInstruction1, sameAsModVlanHeaderInstruction1)
                .testEquals();
    }

    //  ModArpIPInstruction

    private final Instruction modArpIPInstruction1 = Instructions.modArpSpa(ip41);
    private final Instruction sameAsModArpIPInstruction1 = Instructions.modArpSpa(ip41);
    private final Instruction modArpIPInstruction2 = Instructions.modArpSpa(ip42);

    /**
     * Test the modArpSpa() method.
     */
    @Test
    public void testModArpSpaMethod() {
        final Instruction instruction = Instructions.modArpSpa(ip41);
        final L3ModificationInstruction.ModArpIPInstruction modArpIPInstruction =
                checkAndConvert(instruction,
                                Instruction.Type.L3MODIFICATION,
                                L3ModificationInstruction.ModArpIPInstruction.class);
        assertThat(modArpIPInstruction.subtype(),
                   is(L3ModificationInstruction.L3SubType.ARP_SPA));
        assertThat(modArpIPInstruction.ip(), is(ip41));
    }

    /**
     * Test the modArpTpa() method.
     */
    @Test
    public void testModArpTpaMethod() {
        final Instruction instruction = Instructions.modArpTpa(ip41);
        final L3ModificationInstruction.ModArpIPInstruction modArpIPInstruction =
                checkAndConvert(instruction,
                                Instruction.Type.L3MODIFICATION,
                                L3ModificationInstruction.ModArpIPInstruction.class);
        assertThat(modArpIPInstruction.subtype(),
                   is(L3ModificationInstruction.L3SubType.ARP_TPA));
        assertThat(modArpIPInstruction.ip(), is(ip41));
    }

    /**
     * Tests the equals(), hashCode() and toString() methods of the
     * ModArpIPInstruction class.
     */

    @Test
    public void testModArpIPInstructionEquals() {
        checkEqualsAndToString(modArpIPInstruction1,
                               sameAsModArpIPInstruction1,
                               modArpIPInstruction2);
    }

    //  ModArpEthInstruction

    private final Instruction modArpEthInstruction1 = Instructions.modArpSha(mac1);
    private final Instruction sameAsModArpEthInstruction1 = Instructions.modArpSha(mac1);
    private final Instruction modArpEthInstruction2 = Instructions.modArpSha(mac2);

    /**
     * Test the modArpSha() method.
     */
    @Test
    public void testModArpShaMethod() {
        final Instruction instruction = Instructions.modArpSha(mac1);
        final L3ModificationInstruction.ModArpEthInstruction modArpEthInstruction =
                checkAndConvert(instruction,
                                Instruction.Type.L3MODIFICATION,
                                L3ModificationInstruction.ModArpEthInstruction.class);
        assertThat(modArpEthInstruction.subtype(),
                   is(L3ModificationInstruction.L3SubType.ARP_SHA));
        assertThat(modArpEthInstruction.mac(), is(mac1));
    }

    /**
     * Test the modArpTha() method.
     */
    @Test
    public void testModArpThaMethod() {
        final Instruction instruction = Instructions.modArpTha(mac1);
        final L3ModificationInstruction.ModArpEthInstruction modArpEthInstruction =
                checkAndConvert(instruction,
                                Instruction.Type.L3MODIFICATION,
                                L3ModificationInstruction.ModArpEthInstruction.class);
        assertThat(modArpEthInstruction.subtype(),
                   is(L3ModificationInstruction.L3SubType.ARP_THA));
        assertThat(modArpEthInstruction.mac(), is(mac1));
    }

    /**
     * Tests the equals(), hashCode() and toString() methods of the
     * ModArpIPInstruction class.
     */

    @Test
    public void testModArpEthInstructionEquals() {
        checkEqualsAndToString(modArpEthInstruction1,
                               sameAsModArpEthInstruction1,
                               modArpEthInstruction2);
    }

    //  ModArpOpInstruction

    private final Instruction modArpOpInstruction1 = Instructions.modL3ArpOp((short) 1);
    private final Instruction sameAsModArpOpInstruction1 = Instructions.modL3ArpOp((short) 1);
    private final Instruction modArpOpInstruction2 = Instructions.modL3ArpOp((short) 2);

    /**
     * Test the modL3ArpOp() method.
     */
    @Test
    public void testModArpModL3ArpOpMethod() {
        final Instruction instruction = Instructions.modL3ArpOp((short) 1);
        final L3ModificationInstruction.ModArpOpInstruction modArpEthInstruction =
                checkAndConvert(instruction,
                                Instruction.Type.L3MODIFICATION,
                                L3ModificationInstruction.ModArpOpInstruction.class);
        assertThat(modArpEthInstruction.subtype(),
                   is(L3ModificationInstruction.L3SubType.ARP_OP));
        assertThat(modArpEthInstruction.op(), is(1L));
    }

    /**
     * Tests the equals(), hashCode() and toString() methods of the
     * ModArpIPInstruction class.
     */

    @Test
    public void testModArpOpInstructionEquals() {
        checkEqualsAndToString(modArpOpInstruction1,
                               sameAsModArpOpInstruction1,
                               modArpOpInstruction2);
    }

    //  ModTtlInstruction

    private final Instruction modArpTtlInstruction1 = Instructions.copyTtlIn();
    private final Instruction sameAsModArpTtlInstruction1 = Instructions.copyTtlIn();
    private final Instruction modArpTtlInstruction2 = Instructions.copyTtlOut();
    private final Instruction modArpTtlInstruction3 = Instructions.decNwTtl();

    /**
     * Test the copyTtlIn() method.
     */
    @Test
    public void testCopyTtlInMethod() {
        final Instruction instruction = Instructions.copyTtlIn();
        final L3ModificationInstruction.ModTtlInstruction modTtlInstruction =
                checkAndConvert(instruction,
                                Instruction.Type.L3MODIFICATION,
                                L3ModificationInstruction.ModTtlInstruction.class);
        assertThat(modTtlInstruction.subtype(),
                   is(L3ModificationInstruction.L3SubType.TTL_IN));
    }

    /**
     * Test the copyTtlOut() method.
     */
    @Test
    public void testCopyTtlOutMethod() {
        final Instruction instruction = Instructions.copyTtlOut();
        final L3ModificationInstruction.ModTtlInstruction modTtlInstruction =
                checkAndConvert(instruction,
                                Instruction.Type.L3MODIFICATION,
                                L3ModificationInstruction.ModTtlInstruction.class);
        assertThat(modTtlInstruction.subtype(),
                   is(L3ModificationInstruction.L3SubType.TTL_OUT));
    }

    /**
     * Test the decNwTtl() method.
     */
    @Test
    public void testDecNwTtlOutMethod() {
        final Instruction instruction = Instructions.decNwTtl();
        final L3ModificationInstruction.ModTtlInstruction modTtlInstruction =
                checkAndConvert(instruction,
                                Instruction.Type.L3MODIFICATION,
                                L3ModificationInstruction.ModTtlInstruction.class);
        assertThat(modTtlInstruction.subtype(),
                   is(L3ModificationInstruction.L3SubType.DEC_TTL));
    }

    /**
     * Tests the equals(), hashCode() and toString() methods of the
     * ModArpIPInstruction class.
     */

    @Test
    public void testModTtlInstructionEquals() {
        new EqualsTester()
                .addEqualityGroup(modArpTtlInstruction1, sameAsModArpTtlInstruction1)
                .addEqualityGroup(modArpTtlInstruction2)
                .addEqualityGroup(modArpTtlInstruction3)
                .testEquals();
    }

    // PiInstruction
    PiTableAction piTableAction1 = PiAction.builder()
            .withId(PiActionId.of("set_egress_port_0"))
            .withParameter(new PiActionParam(PiActionParamId.of("port"),
                                                             ImmutableByteSequence.copyFrom(10))).build();
    PiTableAction piTableAction2 = PiAction.builder()
            .withId(PiActionId.of("set_egress_port_0"))
            .withParameter(new PiActionParam(PiActionParamId.of("port"),
                    ImmutableByteSequence.copyFrom(20))).build();
    private final Instruction piSetEgressPortInstruction1 = new PiInstruction(piTableAction1);
    private final Instruction sameAsPiSetEgressPortInstruction1 = new PiInstruction(piTableAction1);
    private final Instruction piSetEgressPortInstruction2 = new PiInstruction(piTableAction2);

    /**
     * Test the PiInstruction() method.
     */
    @Test
    public void testPiMethod() {
        final Instruction instruction = new PiInstruction(piTableAction1);
        final PiInstruction piInstruction = checkAndConvert(instruction,
                Instruction.Type.PROTOCOL_INDEPENDENT, PiInstruction.class);

        assertThat(piInstruction.action(), is(piTableAction1));
        assertThat(piInstruction.type(), is(Instruction.Type.PROTOCOL_INDEPENDENT));
    }

    /**
     * Tests the equals(), hashCode() and toString() methods of the
     * PiInstruction class.
     */

    @Test
    public void testPiInstructionEquals() {
        new EqualsTester()
                .addEqualityGroup(piSetEgressPortInstruction1, sameAsPiSetEgressPortInstruction1)
                .addEqualityGroup(piSetEgressPortInstruction2)
                .testEquals();
    }

}
