/*
 * Copyright 2014-2015 Open Networking Laboratory
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

import org.junit.Test;
import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onlab.packet.MplsLabel;
import org.onlab.packet.TpPort;
import org.onlab.packet.VlanId;
import org.onosproject.net.ChannelSpacing;
import org.onosproject.net.GridType;
import org.onosproject.net.IndexedLambda;
import org.onosproject.net.Lambda;
import org.onosproject.net.OduSignalId;
import org.onosproject.net.PortNumber;

import com.google.common.testing.EqualsTester;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.onlab.junit.ImmutableClassChecker.assertThatClassIsImmutable;
import static org.onlab.junit.UtilityClassChecker.assertThatClassIsUtility;
import static org.onosproject.net.PortNumber.portNumber;
import static org.onosproject.net.OduSignalId.oduSignalId;

/**
 * Unit tests for the Instructions class.
 */
public class InstructionsTest {

    /**
     * Checks that a Criterion object has the proper type, and then converts
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
     * Checks the equals() and toString() methods of a Criterion class.
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
        assertThatClassIsImmutable(Instructions.DropInstruction.class);
        assertThatClassIsImmutable(Instructions.OutputInstruction.class);
        assertThatClassIsImmutable(L0ModificationInstruction.ModLambdaInstruction.class);
        assertThatClassIsImmutable(L0ModificationInstruction.ModOchSignalInstruction.class);
        assertThatClassIsImmutable(L1ModificationInstruction.ModOduSignalIdInstruction.class);
        assertThatClassIsImmutable(L2ModificationInstruction.ModEtherInstruction.class);
        assertThatClassIsImmutable(L2ModificationInstruction.ModVlanIdInstruction.class);
        assertThatClassIsImmutable(L2ModificationInstruction.ModVlanPcpInstruction.class);
        assertThatClassIsImmutable(L3ModificationInstruction.ModIPInstruction.class);
        assertThatClassIsImmutable(L3ModificationInstruction.ModIPv6FlowLabelInstruction.class);
        assertThatClassIsImmutable(L2ModificationInstruction.ModMplsLabelInstruction.class);
        assertThatClassIsImmutable(L2ModificationInstruction.PushHeaderInstructions.class);
    }

    //  DropInstruction

    private final Instructions.DropInstruction drop1 = Instructions.createDrop();
    private final Instructions.DropInstruction drop2 = Instructions.createDrop();

    /**
     * Test the createDrop method.
     */
    @Test
    public void testCreateDropMethod() {
        Instructions.DropInstruction instruction = Instructions.createDrop();
        checkAndConvert(instruction,
                        Instruction.Type.DROP,
                        Instructions.DropInstruction.class);
    }

    /**
     * Test the equals() method of the DropInstruction class.
     */

    @Test
    public void testDropInstructionEquals() throws Exception {
        assertThat(drop1, is(equalTo(drop2)));
    }

    /**
     * Test the hashCode() method of the DropInstruction class.
     */

    @Test
    public void testDropInstructionHashCode() {
        assertThat(drop1.hashCode(), is(equalTo(drop2.hashCode())));
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

    private final IndexedLambda lambda1 = new IndexedLambda(1);
    private final IndexedLambda lambda2 = new IndexedLambda(2);
    private final Instruction lambdaInstruction1 = Instructions.modL0Lambda(lambda1);
    private final Instruction sameAsLambdaInstruction1 = Instructions.modL0Lambda(lambda1);
    private final Instruction lambdaInstruction2 = Instructions.modL0Lambda(lambda2);

    /**
     * Test the modL0Lambda method.
     */
    @Test
    public void testCreateLambdaMethod() {
        final Instruction instruction = Instructions.modL0Lambda(lambda1);
        final L0ModificationInstruction.ModLambdaInstruction lambdaInstruction =
                checkAndConvert(instruction,
                        Instruction.Type.L0MODIFICATION,
                        L0ModificationInstruction.ModLambdaInstruction.class);
        assertThat(lambdaInstruction.lambda(), is(equalTo((short) lambda1.index())));
    }

    /**
     * Test the equals() method of the ModLambdaInstruction class.
     */

    @Test
    public void testModLambdaInstructionEquals() throws Exception {
        checkEqualsAndToString(lambdaInstruction1,
                               sameAsLambdaInstruction1,
                               lambdaInstruction2);
    }

    /**
     * Test the hashCode() method of the ModLambdaInstruction class.
     */

    @Test
    public void testModLambdaInstructionHashCode() {
        assertThat(lambdaInstruction1.hashCode(),
                   is(equalTo(sameAsLambdaInstruction1.hashCode())));
        assertThat(lambdaInstruction1.hashCode(),
                is(not(equalTo(lambdaInstruction2.hashCode()))));
    }

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
     * Test the hashCode() method of the ModEtherInstruction class.
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
        assertThat(modMplsLabelInstruction.mplsLabel(), is(equalTo(mplsLabel)));
        assertThat(modMplsLabelInstruction.subtype(),
                is(equalTo(L2ModificationInstruction.L2SubType.MPLS_LABEL)));
    }

    /**
     * Test the equals(), hashCode and toString() methods of the
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
}
