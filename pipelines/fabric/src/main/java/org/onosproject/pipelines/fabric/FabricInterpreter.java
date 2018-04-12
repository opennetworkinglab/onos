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

import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.onlab.packet.DeserializationException;
import org.onlab.packet.Ethernet;
import org.onlab.util.ImmutableByteSequence;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Port;
import org.onosproject.net.PortNumber;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.driver.AbstractHandlerBehaviour;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flow.criteria.Criterion;
import org.onosproject.net.flow.instructions.Instructions;
import org.onosproject.net.packet.DefaultInboundPacket;
import org.onosproject.net.packet.InboundPacket;
import org.onosproject.net.packet.OutboundPacket;
import org.onosproject.net.pi.model.PiMatchFieldId;
import org.onosproject.net.pi.model.PiPipelineInterpreter;
import org.onosproject.net.pi.model.PiTableId;
import org.onosproject.net.pi.runtime.PiAction;
import org.onosproject.net.pi.runtime.PiControlMetadata;
import org.onosproject.net.pi.runtime.PiPacketOperation;

import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static java.lang.String.format;
import static java.util.stream.Collectors.toList;
import static org.onlab.util.ImmutableByteSequence.copyFrom;
import static org.onosproject.net.PortNumber.FLOOD;
import static org.onosproject.net.flow.instructions.Instruction.Type.OUTPUT;
import static org.onosproject.net.pi.model.PiPacketOperationType.PACKET_OUT;

/**
 * Interpreter for fabric pipeline.
 */
public class FabricInterpreter extends AbstractHandlerBehaviour
        implements PiPipelineInterpreter {
    private static final ImmutableBiMap<Integer, PiTableId> TABLE_ID_MAP =
            ImmutableBiMap.<Integer, PiTableId>builder()
                    // Filtering
                    .put(0, FabricConstants.TBL_INGRESS_PORT_VLAN_ID)
                    .put(1, FabricConstants.TBL_FWD_CLASSIFIER_ID)
                    // Forwarding
                    .put(2, FabricConstants.TBL_MPLS_ID)
                    .put(3, FabricConstants.TBL_UNICAST_V4_ID)
                    .put(4, FabricConstants.TBL_UNICAST_V6_ID)
                    .put(5, FabricConstants.TBL_MULTICAST_V4_ID)
                    .put(6, FabricConstants.TBL_MULTICAST_V6_ID)
                    .put(7, FabricConstants.TBL_BRIDGING_ID)
                    .put(8, FabricConstants.TBL_ACL_ID)
                    // Next
                    .put(9, FabricConstants.TBL_SIMPLE_ID)
                    .put(10, FabricConstants.TBL_HASHED_ID)
                    .put(11, FabricConstants.TBL_MULTICAST_ID)
                    .build();

    private static final Set<PiTableId> FILTERING_CTRL_TBLS = ImmutableSet.of(FabricConstants.TBL_INGRESS_PORT_VLAN_ID,
                                                                              FabricConstants.TBL_FWD_CLASSIFIER_ID);
    private static final Set<PiTableId> FORWARDING_CTRL_TBLS = ImmutableSet.of(FabricConstants.TBL_MPLS_ID,
                                                                               FabricConstants.TBL_UNICAST_V4_ID,
                                                                               FabricConstants.TBL_UNICAST_V6_ID,
                                                                               FabricConstants.TBL_MULTICAST_V4_ID,
                                                                               FabricConstants.TBL_MULTICAST_V6_ID,
                                                                               FabricConstants.TBL_BRIDGING_ID,
                                                                               FabricConstants.TBL_ACL_ID);
    private static final Set<PiTableId> NEXT_CTRL_TBLS = ImmutableSet.of(FabricConstants.TBL_SIMPLE_ID,
                                                                         FabricConstants.TBL_HASHED_ID,
                                                                         FabricConstants.TBL_MULTICAST_ID);

    private static final ImmutableMap<Criterion.Type, PiMatchFieldId> CRITERION_MAP =
            ImmutableMap.<Criterion.Type, PiMatchFieldId>builder()
                    .put(Criterion.Type.IN_PORT, FabricConstants.HF_STANDARD_METADATA_INGRESS_PORT_ID)
                    .put(Criterion.Type.ETH_DST, FabricConstants.HF_ETHERNET_DST_ADDR_ID)
                    .put(Criterion.Type.ETH_SRC, FabricConstants.HF_ETHERNET_SRC_ADDR_ID)
                    .put(Criterion.Type.ETH_TYPE, FabricConstants.HF_FABRIC_METADATA_ORIGINAL_ETHER_TYPE_ID)
                    .put(Criterion.Type.MPLS_LABEL, FabricConstants.HF_MPLS_LABEL_ID)
                    .put(Criterion.Type.VLAN_VID, FabricConstants.HF_VLAN_TAG_VLAN_ID_ID)
                    .put(Criterion.Type.IPV4_DST, FabricConstants.HF_IPV4_DST_ADDR_ID)
                    .put(Criterion.Type.IPV4_SRC, FabricConstants.HF_IPV4_SRC_ADDR_ID)
                    .put(Criterion.Type.IPV6_DST, FabricConstants.HF_IPV6_DST_ADDR_ID)
                    .put(Criterion.Type.TCP_SRC, FabricConstants.HF_FABRIC_METADATA_L4_SRC_PORT_ID)
                    .put(Criterion.Type.TCP_DST, FabricConstants.HF_FABRIC_METADATA_L4_DST_PORT_ID)
                    .put(Criterion.Type.UDP_SRC, FabricConstants.HF_FABRIC_METADATA_L4_SRC_PORT_ID)
                    .put(Criterion.Type.UDP_DST, FabricConstants.HF_FABRIC_METADATA_L4_DST_PORT_ID)
                    .put(Criterion.Type.IP_PROTO, FabricConstants.HF_FABRIC_METADATA_IP_PROTO_ID)
                    .put(Criterion.Type.ICMPV6_TYPE, FabricConstants.HF_ICMP_ICMP_TYPE_ID)
                    .put(Criterion.Type.ICMPV6_CODE, FabricConstants.HF_ICMP_ICMP_CODE_ID)
                    .build();

    private static final ImmutableMap<PiMatchFieldId, Criterion.Type> INVERSE_CRITERION_MAP =
            ImmutableMap.<PiMatchFieldId, Criterion.Type>builder()
                    .put(FabricConstants.HF_STANDARD_METADATA_INGRESS_PORT_ID, Criterion.Type.IN_PORT)
                    .put(FabricConstants.HF_ETHERNET_DST_ADDR_ID, Criterion.Type.ETH_DST)
                    .put(FabricConstants.HF_ETHERNET_SRC_ADDR_ID, Criterion.Type.ETH_SRC)
                    .put(FabricConstants.HF_FABRIC_METADATA_ORIGINAL_ETHER_TYPE_ID, Criterion.Type.ETH_TYPE)
                    .put(FabricConstants.HF_MPLS_LABEL_ID, Criterion.Type.MPLS_LABEL)
                    .put(FabricConstants.HF_VLAN_TAG_VLAN_ID_ID, Criterion.Type.VLAN_VID)
                    .put(FabricConstants.HF_IPV4_DST_ADDR_ID, Criterion.Type.IPV4_DST)
                    .put(FabricConstants.HF_IPV4_SRC_ADDR_ID, Criterion.Type.IPV4_SRC)
                    .put(FabricConstants.HF_IPV6_DST_ADDR_ID, Criterion.Type.IPV6_DST)
                    // FIXME: might be incorrect if we inverse the map....
                    .put(FabricConstants.HF_FABRIC_METADATA_L4_SRC_PORT_ID, Criterion.Type.UDP_SRC)
                    .put(FabricConstants.HF_FABRIC_METADATA_L4_DST_PORT_ID, Criterion.Type.UDP_DST)
                    .put(FabricConstants.HF_FABRIC_METADATA_IP_PROTO_ID, Criterion.Type.IP_PROTO)
                    .put(FabricConstants.HF_ICMP_ICMP_TYPE_ID, Criterion.Type.ICMPV6_TYPE)
                    .put(FabricConstants.HF_ICMP_ICMP_CODE_ID, Criterion.Type.ICMPV6_CODE)
                    .build();

    @Override
    public Optional<PiMatchFieldId> mapCriterionType(Criterion.Type type) {
        return Optional.ofNullable(CRITERION_MAP.get(type));
    }

    @Override
    public Optional<Criterion.Type> mapPiMatchFieldId(PiMatchFieldId fieldId) {
        return Optional.ofNullable(INVERSE_CRITERION_MAP.get(fieldId));
    }

    @Override
    public Optional<PiTableId> mapFlowRuleTableId(int flowRuleTableId) {
        return Optional.ofNullable(TABLE_ID_MAP.get(flowRuleTableId));
    }

    @Override
    public Optional<Integer> mapPiTableId(PiTableId piTableId) {
        return Optional.ofNullable(TABLE_ID_MAP.inverse().get(piTableId));
    }

    @Override
    public PiAction mapTreatment(TrafficTreatment treatment, PiTableId piTableId)
            throws PiInterpreterException {

        if (FILTERING_CTRL_TBLS.contains(piTableId)) {
            return FabricTreatmentInterpreter.mapFilteringTreatment(treatment);
        } else if (FORWARDING_CTRL_TBLS.contains(piTableId)) {
            return FabricTreatmentInterpreter.mapForwardingTreatment(treatment);
        } else if (NEXT_CTRL_TBLS.contains(piTableId)) {
            return FabricTreatmentInterpreter.mapNextTreatment(treatment);
        } else {
            throw new PiInterpreterException(String.format("Table %s unsupported", piTableId));
        }
    }

    private PiPacketOperation createPiPacketOperation(DeviceId deviceId, ByteBuffer data, long portNumber)
            throws PiInterpreterException {
        PiControlMetadata metadata = createPacketMetadata(portNumber);
        return PiPacketOperation.builder()
                .forDevice(deviceId)
                .withType(PACKET_OUT)
                .withData(copyFrom(data))
                .withMetadatas(ImmutableList.of(metadata))
                .build();
    }

    private PiControlMetadata createPacketMetadata(long portNumber) throws PiInterpreterException {
        try {
            return PiControlMetadata.builder()
                    .withId(FabricConstants.CTRL_META_EGRESS_PORT_ID)
                    .withValue(copyFrom(portNumber).fit(FabricConstants.PORT_BITWIDTH))
                    .build();
        } catch (ImmutableByteSequence.ByteSequenceTrimException e) {
            throw new PiInterpreterException(format(
                    "Port number %d too big, %s", portNumber, e.getMessage()));
        }
    }

    @Override
    public Collection<PiPacketOperation> mapOutboundPacket(OutboundPacket packet)
            throws PiInterpreterException {
        DeviceId deviceId = packet.sendThrough();
        TrafficTreatment treatment = packet.treatment();

        // fabric.p4 supports only OUTPUT instructions.
        List<Instructions.OutputInstruction> outInstructions = treatment
                .allInstructions()
                .stream()
                .filter(i -> i.type().equals(OUTPUT))
                .map(i -> (Instructions.OutputInstruction) i)
                .collect(toList());

        if (treatment.allInstructions().size() != outInstructions.size()) {
            // There are other instructions that are not of type OUTPUT.
            throw new PiInterpreterException("Treatment not supported: " + treatment);
        }

        ImmutableList.Builder<PiPacketOperation> builder = ImmutableList.builder();
        for (Instructions.OutputInstruction outInst : outInstructions) {
            if (outInst.port().isLogical() && !outInst.port().equals(FLOOD)) {
                throw new PiInterpreterException(format(
                        "Output on logical port '%s' not supported", outInst.port()));
            } else if (outInst.port().equals(FLOOD)) {
                // Since fabric.p4 does not support flooding, we create a packet
                // operation for each switch port.
                final DeviceService deviceService = handler().get(DeviceService.class);
                for (Port port : deviceService.getPorts(packet.sendThrough())) {
                    builder.add(createPiPacketOperation(deviceId, packet.data(), port.number().toLong()));
                }
            } else {
                builder.add(createPiPacketOperation(deviceId, packet.data(), outInst.port().toLong()));
            }
        }
        return builder.build();
    }

    @Override
    public InboundPacket mapInboundPacket(PiPacketOperation packetIn) throws PiInterpreterException {
        // Assuming that the packet is ethernet, which is fine since fabric.p4
        // can deparse only ethernet packets.
        DeviceId deviceId = packetIn.deviceId();
        Ethernet ethPkt;
        try {
            ethPkt = Ethernet.deserializer().deserialize(packetIn.data().asArray(), 0,
                                                         packetIn.data().size());
        } catch (DeserializationException dex) {
            throw new PiInterpreterException(dex.getMessage());
        }

        // Returns the ingress port packet metadata.
        Optional<PiControlMetadata> packetMetadata = packetIn.metadatas()
                .stream().filter(m -> m.id().equals(FabricConstants.CTRL_META_INGRESS_PORT_ID))
                .findFirst();

        if (packetMetadata.isPresent()) {
            ImmutableByteSequence portByteSequence = packetMetadata.get().value();
            short s = portByteSequence.asReadOnlyBuffer().getShort();
            ConnectPoint receivedFrom = new ConnectPoint(deviceId, PortNumber.portNumber(s));
            ByteBuffer rawData = ByteBuffer.wrap(packetIn.data().asArray());
            return new DefaultInboundPacket(receivedFrom, ethPkt, rawData);
        } else {
            throw new PiInterpreterException(format(
                    "Missing metadata '%s' in packet-in received from '%s': %s",
                    FabricConstants.CTRL_META_INGRESS_PORT_ID, deviceId, packetIn));
        }
    }
}
