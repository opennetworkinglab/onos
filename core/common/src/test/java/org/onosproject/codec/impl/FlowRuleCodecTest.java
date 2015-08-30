/*
 * Copyright 2015 Open Networking Laboratory
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

import java.io.IOException;
import java.io.InputStream;
import java.util.SortedMap;
import java.util.TreeMap;

import org.junit.Before;
import org.junit.Test;
import org.onlab.packet.EthType;
import org.onlab.packet.Ethernet;
import org.onlab.packet.IpAddress;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.MacAddress;
import org.onlab.packet.MplsLabel;
import org.onlab.packet.VlanId;
import org.onosproject.codec.JsonCodec;
import org.onosproject.core.CoreService;
import org.onosproject.net.ChannelSpacing;
import org.onosproject.net.GridType;
import org.onosproject.net.Lambda;
import org.onosproject.net.OchSignal;
import org.onosproject.net.PortNumber;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.criteria.Criterion;
import org.onosproject.net.flow.criteria.EthCriterion;
import org.onosproject.net.flow.criteria.EthTypeCriterion;
import org.onosproject.net.flow.criteria.IPCriterion;
import org.onosproject.net.flow.criteria.IPDscpCriterion;
import org.onosproject.net.flow.criteria.IPEcnCriterion;
import org.onosproject.net.flow.criteria.IPProtocolCriterion;
import org.onosproject.net.flow.criteria.IPv6ExthdrFlagsCriterion;
import org.onosproject.net.flow.criteria.IPv6FlowLabelCriterion;
import org.onosproject.net.flow.criteria.IPv6NDLinkLayerAddressCriterion;
import org.onosproject.net.flow.criteria.IPv6NDTargetAddressCriterion;
import org.onosproject.net.flow.criteria.IcmpCodeCriterion;
import org.onosproject.net.flow.criteria.IcmpTypeCriterion;
import org.onosproject.net.flow.criteria.Icmpv6CodeCriterion;
import org.onosproject.net.flow.criteria.Icmpv6TypeCriterion;
import org.onosproject.net.flow.criteria.IndexedLambdaCriterion;
import org.onosproject.net.flow.criteria.MplsCriterion;
import org.onosproject.net.flow.criteria.OchSignalCriterion;
import org.onosproject.net.flow.criteria.PortCriterion;
import org.onosproject.net.flow.criteria.SctpPortCriterion;
import org.onosproject.net.flow.criteria.TcpPortCriterion;
import org.onosproject.net.flow.criteria.TunnelIdCriterion;
import org.onosproject.net.flow.criteria.UdpPortCriterion;
import org.onosproject.net.flow.criteria.VlanIdCriterion;
import org.onosproject.net.flow.criteria.VlanPcpCriterion;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onosproject.net.flow.instructions.Instruction;
import org.onosproject.net.flow.instructions.Instructions;
import org.onosproject.net.flow.instructions.L0ModificationInstruction;
import org.onosproject.net.flow.instructions.L2ModificationInstruction;
import org.onosproject.net.flow.instructions.L3ModificationInstruction;
import org.onosproject.net.flow.instructions.L4ModificationInstruction;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.onosproject.net.NetTestTools.APP_ID;

/**
 * Flow rule codec unit tests.
 */
public class FlowRuleCodecTest {

    MockCodecContext context;
    JsonCodec<FlowRule> flowRuleCodec;
    final CoreService mockCoreService = createMock(CoreService.class);

    /**
     * Sets up for each test.  Creates a context and fetches the flow rule
     * codec.
     */
    @Before
    public void setUp() {
        context = new MockCodecContext();
        flowRuleCodec = context.codec(FlowRule.class);
        assertThat(flowRuleCodec, notNullValue());

        expect(mockCoreService.registerApplication(FlowRuleCodec.REST_APP_ID))
                .andReturn(APP_ID).anyTimes();
        replay(mockCoreService);
        context.registerService(CoreService.class, mockCoreService);
    }

    /**
     * Reads in a rule from the given resource and decodes it.
     *
     * @param resourceName resource to use to read the JSON for the rule
     * @return decoded flow rule
     * @throws IOException if processing the resource fails
     */
    private FlowRule getRule(String resourceName) throws IOException {
        InputStream jsonStream = FlowRuleCodecTest.class
                .getResourceAsStream(resourceName);
        JsonNode json = context.mapper().readTree(jsonStream);
        assertThat(json, notNullValue());
        FlowRule rule = flowRuleCodec.decode((ObjectNode) json, context);
        assertThat(rule, notNullValue());
        return rule;
    }

    /**
     * Checks that the data shared by all the resources is correct for a
     * given rule.
     *
     * @param rule rule to check
     */
    private void checkCommonData(FlowRule rule) {
        assertThat(rule.appId(), is(APP_ID.id()));
        assertThat(rule.isPermanent(), is(false));
        assertThat(rule.timeout(), is(1));
        assertThat(rule.priority(), is(1));
        assertThat(rule.deviceId().toString(), is("of:0000000000000001"));
    }

    /**
     * Checks that a simple rule decodes properly.
     *
     * @throws IOException if the resource cannot be processed
     */
    @Test
    public void codecSimpleFlowTest() throws IOException {
        FlowRule rule = getRule("simple-flow.json");

        checkCommonData(rule);

        assertThat(rule.selector().criteria().size(), is(1));
        Criterion criterion1 = rule.selector().criteria().iterator().next();
        assertThat(criterion1.type(), is(Criterion.Type.ETH_TYPE));
        assertThat(((EthTypeCriterion) criterion1).ethType(), is(new EthType(2054)));

        assertThat(rule.treatment().allInstructions().size(), is(1));
        Instruction instruction1 = rule.treatment().allInstructions().get(0);
        assertThat(instruction1.type(), is(Instruction.Type.OUTPUT));
        assertThat(((Instructions.OutputInstruction) instruction1).port(), is(PortNumber.CONTROLLER));
    }

    SortedMap<String, Instruction> instructions = new TreeMap<>();

    /**
     * Looks up an instruction in the instruction map based on type and subtype.
     *
     * @param type type string
     * @param subType subtype string
     * @return instruction that matches
     */
    private Instruction getInstruction(Instruction.Type type, String subType) {
        Instruction instruction = instructions.get(type.name() + "/" + subType);
        assertThat(instruction, notNullValue());
        assertThat(instruction.type(), is(type));
        return instruction;
    }

    /**
     * Checks that a rule with one of each instruction type decodes properly.
     *
     * @throws IOException if the resource cannot be processed
     */
    @Test
    public void decodeInstructionsFlowTest() throws Exception {
        FlowRule rule = getRule("instructions-flow.json");

        checkCommonData(rule);

        rule.treatment().allInstructions()
                .stream()
                .forEach(instruction ->
                {
                    String subType;
                    if (instruction.type() == Instruction.Type.L0MODIFICATION) {
                        subType = ((L0ModificationInstruction) instruction)
                                .subtype().name();
                    } else if (instruction.type() == Instruction.Type.L2MODIFICATION) {
                        subType = ((L2ModificationInstruction) instruction)
                                .subtype().name();
                    } else if (instruction.type() == Instruction.Type.L3MODIFICATION) {
                        subType = ((L3ModificationInstruction) instruction)
                                .subtype().name();
                    } else if (instruction.type() == Instruction.Type.L4MODIFICATION) {
                        subType = ((L4ModificationInstruction) instruction)
                                .subtype().name();
                    } else {
                        subType = "";
                    }
                    instructions.put(
                            instruction.type().name() + "/" + subType, instruction);
                });

        assertThat(rule.treatment().allInstructions().size(), is(24));

        Instruction instruction;

        instruction = getInstruction(Instruction.Type.OUTPUT, "");
        assertThat(instruction.type(), is(Instruction.Type.OUTPUT));
        assertThat(((Instructions.OutputInstruction) instruction).port(), is(PortNumber.CONTROLLER));

        instruction = getInstruction(Instruction.Type.L2MODIFICATION,
                L2ModificationInstruction.L2SubType.ETH_SRC.name());
        assertThat(instruction.type(), is(Instruction.Type.L2MODIFICATION));
        assertThat(((L2ModificationInstruction.ModEtherInstruction) instruction).mac(),
                is(MacAddress.valueOf("12:34:56:78:90:12")));

        instruction = getInstruction(Instruction.Type.L2MODIFICATION,
                L2ModificationInstruction.L2SubType.ETH_DST.name());
        assertThat(instruction.type(), is(Instruction.Type.L2MODIFICATION));
        assertThat(((L2ModificationInstruction.ModEtherInstruction) instruction).mac(),
                is(MacAddress.valueOf("98:76:54:32:01:00")));

        instruction = getInstruction(Instruction.Type.L2MODIFICATION,
                L2ModificationInstruction.L2SubType.VLAN_ID.name());
        assertThat(instruction.type(), is(Instruction.Type.L2MODIFICATION));
        assertThat(((L2ModificationInstruction.ModVlanIdInstruction) instruction).vlanId().toShort(),
                is((short) 22));

        instruction = getInstruction(Instruction.Type.L2MODIFICATION,
                L2ModificationInstruction.L2SubType.VLAN_PCP.name());
        assertThat(instruction.type(), is(Instruction.Type.L2MODIFICATION));
        assertThat(((L2ModificationInstruction.ModVlanPcpInstruction) instruction).vlanPcp(),
                is((byte) 1));

        instruction = getInstruction(Instruction.Type.L2MODIFICATION,
                L2ModificationInstruction.L2SubType.MPLS_LABEL.name());
        assertThat(instruction.type(), is(Instruction.Type.L2MODIFICATION));
        assertThat(((L2ModificationInstruction.ModMplsLabelInstruction) instruction)
                        .mplsLabel().toInt(),
                is(MplsLabel.MAX_MPLS));

        instruction = getInstruction(Instruction.Type.L2MODIFICATION,
                L2ModificationInstruction.L2SubType.MPLS_PUSH.name());
        assertThat(instruction.type(), is(Instruction.Type.L2MODIFICATION));
        assertThat(((L2ModificationInstruction.PushHeaderInstructions) instruction)
                        .ethernetType().toShort(),
                is(Ethernet.MPLS_UNICAST));

        instruction = getInstruction(Instruction.Type.L2MODIFICATION,
                L2ModificationInstruction.L2SubType.MPLS_POP.name());
        assertThat(instruction.type(), is(Instruction.Type.L2MODIFICATION));
        assertThat(((L2ModificationInstruction.PushHeaderInstructions) instruction)
                        .ethernetType().toShort(),
                is(Ethernet.MPLS_UNICAST));

        instruction = getInstruction(Instruction.Type.L2MODIFICATION,
                L2ModificationInstruction.L2SubType.DEC_MPLS_TTL.name());
        assertThat(instruction.type(), is(Instruction.Type.L2MODIFICATION));
        assertThat(instruction, instanceOf(L2ModificationInstruction.ModMplsTtlInstruction.class));

        instruction = getInstruction(Instruction.Type.L2MODIFICATION,
                L2ModificationInstruction.L2SubType.VLAN_POP.name());
        assertThat(instruction.type(), is(Instruction.Type.L2MODIFICATION));
        assertThat(instruction, instanceOf(L2ModificationInstruction.PopVlanInstruction.class));

        instruction = getInstruction(Instruction.Type.L2MODIFICATION,
                L2ModificationInstruction.L2SubType.VLAN_PUSH.name());
        assertThat(instruction.type(), is(Instruction.Type.L2MODIFICATION));
        assertThat(instruction, instanceOf(L2ModificationInstruction.PushHeaderInstructions.class));

        instruction = getInstruction(Instruction.Type.L2MODIFICATION,
                L2ModificationInstruction.L2SubType.TUNNEL_ID.name());
        assertThat(instruction.type(), is(Instruction.Type.L2MODIFICATION));
        assertThat(((L2ModificationInstruction.ModTunnelIdInstruction) instruction)
                        .tunnelId(), is(100L));

        instruction = getInstruction(Instruction.Type.L3MODIFICATION,
                L3ModificationInstruction.L3SubType.IPV4_SRC.name());
        assertThat(instruction.type(), is(Instruction.Type.L3MODIFICATION));
        assertThat(((L3ModificationInstruction.ModIPInstruction) instruction).ip(),
                is(IpAddress.valueOf("1.2.3.4")));

        instruction = getInstruction(Instruction.Type.L3MODIFICATION,
                L3ModificationInstruction.L3SubType.IPV4_DST.name());
        assertThat(instruction.type(), is(Instruction.Type.L3MODIFICATION));
        assertThat(((L3ModificationInstruction.ModIPInstruction) instruction).ip(),
                is(IpAddress.valueOf("1.2.3.3")));

        instruction = getInstruction(Instruction.Type.L3MODIFICATION,
                L3ModificationInstruction.L3SubType.IPV6_SRC.name());
        assertThat(instruction.type(), is(Instruction.Type.L3MODIFICATION));
        assertThat(((L3ModificationInstruction.ModIPInstruction) instruction).ip(),
                is(IpAddress.valueOf("1.2.3.2")));

        instruction = getInstruction(Instruction.Type.L3MODIFICATION,
                L3ModificationInstruction.L3SubType.IPV6_DST.name());
        assertThat(instruction.type(), is(Instruction.Type.L3MODIFICATION));
        assertThat(((L3ModificationInstruction.ModIPInstruction) instruction).ip(),
                is(IpAddress.valueOf("1.2.3.1")));

        instruction = getInstruction(Instruction.Type.L3MODIFICATION,
                L3ModificationInstruction.L3SubType.IPV6_FLABEL.name());
        assertThat(instruction.type(), is(Instruction.Type.L3MODIFICATION));
        assertThat(((L3ModificationInstruction.ModIPv6FlowLabelInstruction) instruction)
                .flowLabel(),
                is(8));

        instruction = getInstruction(Instruction.Type.L0MODIFICATION,
                L0ModificationInstruction.L0SubType.LAMBDA.name());
        assertThat(instruction.type(), is(Instruction.Type.L0MODIFICATION));
        assertThat(((L0ModificationInstruction.ModLambdaInstruction) instruction)
                        .lambda(),
                is((short) 7));

        instruction = getInstruction(Instruction.Type.L0MODIFICATION,
                L0ModificationInstruction.L0SubType.OCH.name());
        assertThat(instruction.type(), is(Instruction.Type.L0MODIFICATION));
        L0ModificationInstruction.ModOchSignalInstruction och =
                (L0ModificationInstruction.ModOchSignalInstruction) instruction;
        assertThat(och.lambda().spacingMultiplier(), is(4));
        assertThat(och.lambda().slotGranularity(), is(8));
        assertThat(och.lambda().gridType(), is(GridType.DWDM));
        assertThat(och.lambda().channelSpacing(), is(ChannelSpacing.CHL_100GHZ));

        instruction = getInstruction(Instruction.Type.L4MODIFICATION,
                L4ModificationInstruction.L4SubType.TCP_DST.name());
        assertThat(instruction.type(), is(Instruction.Type.L4MODIFICATION));
        assertThat(((L4ModificationInstruction.ModTransportPortInstruction) instruction)
                .port().toInt(), is(40001));

        instruction = getInstruction(Instruction.Type.L4MODIFICATION,
                L4ModificationInstruction.L4SubType.TCP_SRC.name());
        assertThat(instruction.type(), is(Instruction.Type.L4MODIFICATION));
        assertThat(((L4ModificationInstruction.ModTransportPortInstruction) instruction)
                .port().toInt(), is(40002));

        instruction = getInstruction(Instruction.Type.L4MODIFICATION,
                L4ModificationInstruction.L4SubType.UDP_DST.name());
        assertThat(instruction.type(), is(Instruction.Type.L4MODIFICATION));
        assertThat(((L4ModificationInstruction.ModTransportPortInstruction) instruction)
                .port().toInt(), is(40003));

        instruction = getInstruction(Instruction.Type.L4MODIFICATION,
                L4ModificationInstruction.L4SubType.UDP_SRC.name());
        assertThat(instruction.type(), is(Instruction.Type.L4MODIFICATION));
        assertThat(((L4ModificationInstruction.ModTransportPortInstruction) instruction)
                .port().toInt(), is(40004));
    }

    SortedMap<String, Criterion> criteria = new TreeMap<>();

    /**
     * Looks up a criterion in the instruction map based on type and subtype.
     *
     * @param type type string
     * @return criterion that matches
     */
    private Criterion getCriterion(Criterion.Type type) {
        Criterion criterion = criteria.get(type.name());
        assertThat(criterion.type(), is(type));
        return criterion;
    }

    /**
     * Checks that a rule with one of each kind of criterion decodes properly.
     *
     * @throws IOException if the resource cannot be processed
     */
    @Test
    public void codecCriteriaFlowTest() throws Exception {
        FlowRule rule = getRule("criteria-flow.json");

        checkCommonData(rule);

        assertThat(rule.selector().criteria().size(), is(33));

        rule.selector().criteria()
                .stream()
                .forEach(criterion ->
                        criteria.put(criterion.type().name(), criterion));

        Criterion criterion;

        criterion = getCriterion(Criterion.Type.ETH_TYPE);
        assertThat(((EthTypeCriterion) criterion).ethType(), is(new EthType(2054)));

        criterion = getCriterion(Criterion.Type.ETH_DST);
        assertThat(((EthCriterion) criterion).mac(),
                is(MacAddress.valueOf("00:11:22:33:44:55")));

        criterion = getCriterion(Criterion.Type.ETH_SRC);
        assertThat(((EthCriterion) criterion).mac(),
                is(MacAddress.valueOf("00:11:22:33:44:55")));

        criterion = getCriterion(Criterion.Type.IN_PORT);
        assertThat(((PortCriterion) criterion).port(),
                is(PortNumber.portNumber(23)));

        criterion = getCriterion(Criterion.Type.IN_PHY_PORT);
        assertThat(((PortCriterion) criterion).port(),
                is(PortNumber.portNumber(44)));

        criterion = getCriterion(Criterion.Type.VLAN_VID);
        assertThat(((VlanIdCriterion) criterion).vlanId(),
                is(VlanId.vlanId((short) 777)));

        criterion = getCriterion(Criterion.Type.VLAN_PCP);
        assertThat(((VlanPcpCriterion) criterion).priority(),
                is(((byte) 3)));

        criterion = getCriterion(Criterion.Type.IP_DSCP);
        assertThat(((IPDscpCriterion) criterion).ipDscp(),
                is(((byte) 2)));

        criterion = getCriterion(Criterion.Type.IP_ECN);
        assertThat(((IPEcnCriterion) criterion).ipEcn(),
                is(((byte) 1)));

        criterion = getCriterion(Criterion.Type.IP_PROTO);
        assertThat(((IPProtocolCriterion) criterion).protocol(),
                is(((short) 4)));

        criterion = getCriterion(Criterion.Type.IPV4_SRC);
        assertThat(((IPCriterion) criterion).ip(),
                is((IpPrefix.valueOf("1.2.0.0/32"))));

        criterion = getCriterion(Criterion.Type.IPV4_DST);
        assertThat(((IPCriterion) criterion).ip(),
                is((IpPrefix.valueOf("2.2.0.0/32"))));

        criterion = getCriterion(Criterion.Type.IPV6_SRC);
        assertThat(((IPCriterion) criterion).ip(),
                is((IpPrefix.valueOf("3.2.0.0/32"))));

        criterion = getCriterion(Criterion.Type.IPV6_DST);
        assertThat(((IPCriterion) criterion).ip(),
                is((IpPrefix.valueOf("4.2.0.0/32"))));

        criterion = getCriterion(Criterion.Type.TCP_SRC);
        assertThat(((TcpPortCriterion) criterion).tcpPort().toInt(),
                is(80));

        criterion = getCriterion(Criterion.Type.TCP_DST);
        assertThat(((TcpPortCriterion) criterion).tcpPort().toInt(),
                is(443));

        criterion = getCriterion(Criterion.Type.UDP_SRC);
        assertThat(((UdpPortCriterion) criterion).udpPort().toInt(),
                is(180));

        criterion = getCriterion(Criterion.Type.UDP_DST);
        assertThat(((UdpPortCriterion) criterion).udpPort().toInt(),
                is(1443));

        criterion = getCriterion(Criterion.Type.SCTP_SRC);
        assertThat(((SctpPortCriterion) criterion).sctpPort().toInt(),
                is(280));

        criterion = getCriterion(Criterion.Type.SCTP_DST);
        assertThat(((SctpPortCriterion) criterion).sctpPort().toInt(),
                is(2443));

        criterion = getCriterion(Criterion.Type.ICMPV4_TYPE);
        assertThat(((IcmpTypeCriterion) criterion).icmpType(),
                is((short) 24));

        criterion = getCriterion(Criterion.Type.ICMPV4_CODE);
        assertThat(((IcmpCodeCriterion) criterion).icmpCode(),
                is((short) 16));

        criterion = getCriterion(Criterion.Type.ICMPV6_TYPE);
        assertThat(((Icmpv6TypeCriterion) criterion).icmpv6Type(),
                is((short) 14));

        criterion = getCriterion(Criterion.Type.ICMPV6_CODE);
        assertThat(((Icmpv6CodeCriterion) criterion).icmpv6Code(),
                is((short) 6));

        criterion = getCriterion(Criterion.Type.IPV6_FLABEL);
        assertThat(((IPv6FlowLabelCriterion) criterion).flowLabel(),
                is(8));

        criterion = getCriterion(Criterion.Type.IPV6_ND_TARGET);
        assertThat(((IPv6NDTargetAddressCriterion) criterion)
                        .targetAddress().toString(),
                is("1111:2222:3333:4444:5555:6666:7777:8888"));

        criterion = getCriterion(Criterion.Type.IPV6_ND_SLL);
        assertThat(((IPv6NDLinkLayerAddressCriterion) criterion).mac(),
                is(MacAddress.valueOf("00:11:22:33:44:56")));

        criterion = getCriterion(Criterion.Type.IPV6_ND_TLL);
        assertThat(((IPv6NDLinkLayerAddressCriterion) criterion).mac(),
                is(MacAddress.valueOf("00:11:22:33:44:57")));

        criterion = getCriterion(Criterion.Type.MPLS_LABEL);
        assertThat(((MplsCriterion) criterion).label(),
                is(MplsLabel.mplsLabel(123)));

        criterion = getCriterion(Criterion.Type.IPV6_EXTHDR);
        assertThat(((IPv6ExthdrFlagsCriterion) criterion).exthdrFlags(),
                is(99));

        criterion = getCriterion(Criterion.Type.OCH_SIGID);
        assertThat(((IndexedLambdaCriterion) criterion).lambda(),
                is(Lambda.indexedLambda(122)));

        criterion = getCriterion(Criterion.Type.TUNNEL_ID);
        assertThat(((TunnelIdCriterion) criterion).tunnelId(),
                is(100L));
    }

    /**
     * Checks that a rule with a SigId criterion decodes properly.
     *
     * @throws IOException if the resource cannot be processed
     */
    @Test
    public void codecSigIdCriteriaFlowTest() throws Exception {
        FlowRule rule = getRule("sigid-flow.json");

        checkCommonData(rule);

        assertThat(rule.selector().criteria().size(), is(1));
        Criterion criterion = rule.selector().criteria().iterator().next();
        assertThat(criterion.type(), is(Criterion.Type.OCH_SIGID));
        Lambda lambda = ((OchSignalCriterion) criterion).lambda();
        assertThat(lambda, instanceOf(OchSignal.class));
        OchSignal ochSignal = (OchSignal) lambda;
        assertThat(ochSignal.spacingMultiplier(), is(3));
        assertThat(ochSignal.slotGranularity(), is(4));
        assertThat(ochSignal.gridType(), is(GridType.CWDM));
        assertThat(ochSignal.channelSpacing(), is(ChannelSpacing.CHL_25GHZ));
    }

}
