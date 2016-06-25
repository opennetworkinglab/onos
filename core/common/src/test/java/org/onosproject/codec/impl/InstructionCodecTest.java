/*
 * Copyright 2015-present Open Networking Laboratory
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

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Before;
import org.junit.Test;
import org.onlab.packet.Ip4Address;
import org.onlab.packet.Ip6Address;
import org.onlab.packet.MacAddress;
import org.onlab.packet.MplsLabel;
import org.onlab.packet.VlanId;
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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;
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

}
