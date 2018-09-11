/*
 * Copyright 2015-present Open Networking Foundation
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
package org.onosproject.codec.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.hamcrest.MatcherAssert;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.onlab.packet.Ip4Address;
import org.onlab.packet.Ip6Address;
import org.onlab.packet.MacAddress;
import org.onlab.packet.MplsLabel;
import org.onlab.packet.VlanId;
import org.onlab.util.ImmutableByteSequence;
import org.onosproject.codec.CodecContext;
import org.onosproject.codec.JsonCodec;
import org.onosproject.net.ChannelSpacing;
import org.onosproject.net.GridType;
import org.onosproject.net.Lambda;
import org.onosproject.net.OduSignalId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.flow.instructions.Instruction;
import org.onosproject.net.flow.instructions.Instructions;
import org.onosproject.net.flow.instructions.L0ModificationInstruction;
import org.onosproject.net.flow.instructions.L1ModificationInstruction;
import org.onosproject.net.flow.instructions.L2ModificationInstruction;
import org.onosproject.net.flow.instructions.L3ModificationInstruction;
import org.onosproject.net.flow.instructions.PiInstruction;
import org.onosproject.net.pi.model.PiActionId;
import org.onosproject.net.pi.model.PiActionParamId;
import org.onosproject.net.pi.runtime.PiAction;
import org.onosproject.net.pi.runtime.PiActionParam;
import org.onosproject.net.pi.runtime.PiActionProfileGroupId;
import org.onosproject.net.pi.runtime.PiActionProfileMemberId;
import org.onosproject.net.pi.runtime.PiTableAction;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.onlab.util.ImmutableByteSequence.copyFrom;
import static org.onosproject.codec.impl.InstructionJsonMatcher.matchesInstruction;

/**
 * Unit tests for Instruction codec.
 */
public class InstructionCodecTest {
    CodecContext context;
    JsonCodec<Instruction> instructionCodec;
    /**
     * Sets up for each test.  Creates a context and fetches the instruction
     * codec.
     */
    @Before
    public void setUp() {
        context = new MockCodecContext();
        instructionCodec = context.codec(Instruction.class);
        assertThat(instructionCodec, notNullValue());
    }

    /**
     * Tests the encoding of push mpls header instructions.
     */
    @Test
    public void pushHeaderInstructionsTest() {
        final L2ModificationInstruction.ModMplsHeaderInstruction instruction =
                (L2ModificationInstruction.ModMplsHeaderInstruction) Instructions.pushMpls();
        final ObjectNode instructionJson = instructionCodec.encode(instruction, context);

        assertThat(instructionJson, matchesInstruction(instruction));
    }

    /**
     * Tests the encoding of output instructions.
     */
    @Test
    public void outputInstructionTest() {
        final Instructions.OutputInstruction instruction =
                Instructions.createOutput(PortNumber.portNumber(22));
        final ObjectNode instructionJson =
                instructionCodec.encode(instruction, context);
        assertThat(instructionJson, matchesInstruction(instruction));
    }

    /**
     * Tests the encoding of mod OCh signal instructions.
     */
    @Test
    public void modOchSignalInstructionTest() {
        L0ModificationInstruction.ModOchSignalInstruction instruction =
                (L0ModificationInstruction.ModOchSignalInstruction)
                        Instructions.modL0Lambda(Lambda.ochSignal(GridType.DWDM, ChannelSpacing.CHL_100GHZ, 4, 8));
        ObjectNode instructionJson =
                instructionCodec.encode(instruction, context);
        assertThat(instructionJson, matchesInstruction(instruction));
    }

    /**
     * Tests the encoding of mod ODU signal ID instructions.
     */
    @Test
    public void modOduSignalIdInstructionTest() {
        OduSignalId oduSignalId = OduSignalId.oduSignalId(1, 8, new byte[] {8, 0, 0, 0, 0, 0, 0, 0, 0, 0});
        L1ModificationInstruction.ModOduSignalIdInstruction instruction =
                (L1ModificationInstruction.ModOduSignalIdInstruction)
                    Instructions.modL1OduSignalId(oduSignalId);
        ObjectNode instructionJson =
                instructionCodec.encode(instruction, context);
        assertThat(instructionJson, matchesInstruction(instruction));
    }

    /**
     * Tests the encoding of mod ether instructions.
     */
    @Test
    public void modEtherInstructionTest() {
        final L2ModificationInstruction.ModEtherInstruction instruction =
                (L2ModificationInstruction.ModEtherInstruction)
                        Instructions.modL2Src(MacAddress.valueOf("11:22:33:44:55:66"));
        final ObjectNode instructionJson =
                instructionCodec.encode(instruction, context);
        assertThat(instructionJson, matchesInstruction(instruction));
    }

    /**
     * Tests the encoding of mod vlan id instructions.
     */
    @Test
    public void modVlanIdInstructionTest() {
        final L2ModificationInstruction.ModVlanIdInstruction instruction =
                (L2ModificationInstruction.ModVlanIdInstruction)
                        Instructions.modVlanId(VlanId.vlanId((short) 12));

        final ObjectNode instructionJson =
                instructionCodec.encode(instruction, context);
        assertThat(instructionJson, matchesInstruction(instruction));
    }

    /**
     * Tests the encoding of mod vlan pcp instructions.
     */
    @Test
    public void modVlanPcpInstructionTest() {
        final L2ModificationInstruction.ModVlanPcpInstruction instruction =
                (L2ModificationInstruction.ModVlanPcpInstruction)
                        Instructions.modVlanPcp((byte) 9);
        final ObjectNode instructionJson =
                instructionCodec.encode(instruction, context);
        assertThat(instructionJson, matchesInstruction(instruction));
    }

    /**
     * Tests the encoding of mod IPv4 src instructions.
     */
    @Test
    public void modIPSrcInstructionTest() {
        final Ip4Address ip = Ip4Address.valueOf("1.2.3.4");
        final L3ModificationInstruction.ModIPInstruction instruction =
                (L3ModificationInstruction.ModIPInstruction)
                        Instructions.modL3Src(ip);
        final ObjectNode instructionJson =
                instructionCodec.encode(instruction, context);
        assertThat(instructionJson, matchesInstruction(instruction));
    }

    /**
     * Tests the encoding of mod IPv4 dst instructions.
     */
    @Test
    public void modIPDstInstructionTest() {
        final Ip4Address ip = Ip4Address.valueOf("1.2.3.4");
        final L3ModificationInstruction.ModIPInstruction instruction =
                (L3ModificationInstruction.ModIPInstruction)
                        Instructions.modL3Dst(ip);
        final ObjectNode instructionJson =
                instructionCodec.encode(instruction, context);
        assertThat(instructionJson, matchesInstruction(instruction));
    }

    /**
     * Tests the encoding of mod IPv6 src instructions.
     */
    @Test
    public void modIPv6SrcInstructionTest() {
        final Ip6Address ip = Ip6Address.valueOf("1111::2222");
        final L3ModificationInstruction.ModIPInstruction instruction =
                (L3ModificationInstruction.ModIPInstruction)
                        Instructions.modL3IPv6Src(ip);
        final ObjectNode instructionJson =
                instructionCodec.encode(instruction, context);
        assertThat(instructionJson, matchesInstruction(instruction));
    }

    /**
     * Tests the encoding of mod IPv6 dst instructions.
     */
    @Test
    public void modIPv6DstInstructionTest() {
        final Ip6Address ip = Ip6Address.valueOf("1111::2222");
        final L3ModificationInstruction.ModIPInstruction instruction =
                (L3ModificationInstruction.ModIPInstruction)
                        Instructions.modL3IPv6Dst(ip);
        final ObjectNode instructionJson =
                instructionCodec.encode(instruction, context);
        assertThat(instructionJson, matchesInstruction(instruction));
    }

    /**
     * Tests the encoding of mod IPv6 flow label instructions.
     */
    @Test
    public void modIPv6FlowLabelInstructionTest() {
        final int flowLabel = 0xfffff;
        final L3ModificationInstruction.ModIPv6FlowLabelInstruction instruction =
                (L3ModificationInstruction.ModIPv6FlowLabelInstruction)
                        Instructions.modL3IPv6FlowLabel(flowLabel);
        final ObjectNode instructionJson =
                instructionCodec.encode(instruction, context);
        assertThat(instructionJson, matchesInstruction(instruction));
    }

    /**
     * Tests the encoding of mod MPLS label instructions.
     */
    @Test
    public void modMplsLabelInstructionTest() {
        final L2ModificationInstruction.ModMplsLabelInstruction instruction =
                (L2ModificationInstruction.ModMplsLabelInstruction)
                        Instructions.modMplsLabel(MplsLabel.mplsLabel(99));
        final ObjectNode instructionJson =
                instructionCodec.encode(instruction, context);
        assertThat(instructionJson, matchesInstruction(instruction));
    }

    /**
     * Tests the encoding of protocol-independent instructions.
     */
    @Test
    public void piInstructionEncodingTest() {
        PiActionId actionId = PiActionId.of("set_egress_port");
        PiActionParamId actionParamId = PiActionParamId.of("port");
        PiActionParam actionParam = new PiActionParam(actionParamId, ImmutableByteSequence.copyFrom(10));
        PiTableAction action = PiAction.builder().withId(actionId).withParameter(actionParam).build();
        final PiInstruction actionInstruction = Instructions.piTableAction(action);
        final ObjectNode actionInstructionJson =
                instructionCodec.encode(actionInstruction, context);
        assertThat(actionInstructionJson, matchesInstruction(actionInstruction));

        PiTableAction actionGroupId = PiActionProfileGroupId.of(10);
        final PiInstruction actionGroupIdInstruction = Instructions.piTableAction(actionGroupId);
        final ObjectNode actionGroupIdInstructionJson =
                instructionCodec.encode(actionGroupIdInstruction, context);
        assertThat(actionGroupIdInstructionJson, matchesInstruction(actionGroupIdInstruction));

        PiTableAction actionProfileMemberId = PiActionProfileMemberId.of(10);
        final PiInstruction actionProfileMemberIdInstruction = Instructions.piTableAction(actionProfileMemberId);
        final ObjectNode actionProfileMemberIdInstructionJson =
                instructionCodec.encode(actionProfileMemberIdInstruction, context);
        assertThat(actionProfileMemberIdInstructionJson, matchesInstruction(actionProfileMemberIdInstruction));
    }

    /**
     * Tests the decoding of protocol-independent instructions.
     */
    @Test
    public void piInstructionDecodingTest() throws IOException {

        Instruction actionInstruction = getInstruction("PiActionInstruction.json");
        Assert.assertThat(actionInstruction.type(), is(Instruction.Type.PROTOCOL_INDEPENDENT));
        PiTableAction action = ((PiInstruction) actionInstruction).action();
        Assert.assertThat(action.type(), is(PiTableAction.Type.ACTION));
        Assert.assertThat(((PiAction) action).id().id(), is("set_egress_port"));
        Assert.assertThat(((PiAction) action).parameters().size(), is(1));
        Collection<PiActionParam> actionParams = ((PiAction) action).parameters();
        PiActionParam actionParam = actionParams.iterator().next();
        Assert.assertThat(actionParam.id().id(), is("port"));
        Assert.assertThat(actionParam.value(), is(copyFrom((byte) 0x1)));

        Instruction actionGroupIdInstruction = getInstruction("PiActionProfileGroupIdInstruction.json");
        Assert.assertThat(actionInstruction.type(), is(Instruction.Type.PROTOCOL_INDEPENDENT));
        PiTableAction actionGroupId = ((PiInstruction) actionGroupIdInstruction).action();
        Assert.assertThat(actionGroupId.type(), is(PiTableAction.Type.ACTION_PROFILE_GROUP_ID));
        Assert.assertThat(((PiActionProfileGroupId) actionGroupId).id(), is(100));

        Instruction actionMemberIdInstruction = getInstruction("PiActionProfileMemberIdInstruction.json");
        Assert.assertThat(actionInstruction.type(), is(Instruction.Type.PROTOCOL_INDEPENDENT));
        PiTableAction actionMemberId = ((PiInstruction) actionMemberIdInstruction).action();
        Assert.assertThat(actionMemberId.type(), is(PiTableAction.Type.ACTION_PROFILE_MEMBER_ID));
        Assert.assertThat(((PiActionProfileMemberId) actionMemberId).id(), is(100));
    }

    /**
     * Reads in an instruction from the given resource and decodes it.
     *
     * @param resourceName resource to use to read the JSON for the rule
     * @return decoded instruction
     * @throws IOException if processing the resource fails
     */
    private Instruction getInstruction(String resourceName) throws IOException {
        InputStream jsonStream = InstructionCodecTest.class.getResourceAsStream(resourceName);
        JsonNode json = context.mapper().readTree(jsonStream);
        MatcherAssert.assertThat(json, notNullValue());
        Instruction instruction = instructionCodec.decode((ObjectNode) json, context);
        Assert.assertThat(instruction, notNullValue());
        return instruction;
    }

}
