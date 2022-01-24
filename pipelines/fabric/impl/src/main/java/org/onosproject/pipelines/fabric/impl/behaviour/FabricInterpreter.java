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
import org.onosproject.net.driver.DriverHandler;
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
import org.onosproject.net.pi.runtime.PiPacketMetadata;
import org.onosproject.net.pi.runtime.PiPacketOperation;
import org.onosproject.pipelines.fabric.FabricConstants;

import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static java.lang.String.format;
import static java.util.stream.Collectors.toList;
import static org.onlab.util.ImmutableByteSequence.copyFrom;
import static org.onosproject.net.PortNumber.CONTROLLER;
import static org.onosproject.net.PortNumber.FLOOD;
import static org.onosproject.net.PortNumber.TABLE;
import static org.onosproject.net.flow.instructions.Instruction.Type.OUTPUT;
import static org.onosproject.net.pi.model.PiPacketOperationType.PACKET_OUT;

/**
 * Interpreter for fabric pipeline.
 */
public class FabricInterpreter extends AbstractFabricHandlerBehavior
        implements PiPipelineInterpreter {

    private static final int PORT_BITWIDTH = 9;
    public static final byte[] ONE = new byte[]{1};
    public static final byte[] ZERO = new byte[]{0};

    // Group tables by control block.
    private static final Set<PiTableId> FORWARDING_CTRL_TBLS = ImmutableSet.of(
            FabricConstants.FABRIC_INGRESS_FORWARDING_MPLS,
            FabricConstants.FABRIC_INGRESS_FORWARDING_ROUTING_V4,
            FabricConstants.FABRIC_INGRESS_FORWARDING_ROUTING_V6,
            FabricConstants.FABRIC_INGRESS_FORWARDING_BRIDGING);
    private static final Set<PiTableId> PRE_NEXT_CTRL_TBLS  = ImmutableSet.of(
            FabricConstants.FABRIC_INGRESS_PRE_NEXT_NEXT_MPLS,
            FabricConstants.FABRIC_INGRESS_PRE_NEXT_NEXT_VLAN);
    private static final Set<PiTableId> ACL_CTRL_TBLS = ImmutableSet.of(
            FabricConstants.FABRIC_INGRESS_ACL_ACL);
    private static final Set<PiTableId> NEXT_CTRL_TBLS = ImmutableSet.of(
            FabricConstants.FABRIC_INGRESS_NEXT_SIMPLE,
            FabricConstants.FABRIC_INGRESS_NEXT_HASHED,
            FabricConstants.FABRIC_INGRESS_NEXT_XCONNECT);
    private static final Set<PiTableId> E_NEXT_CTRL_TBLS = ImmutableSet.of(
            FabricConstants.FABRIC_EGRESS_EGRESS_NEXT_EGRESS_VLAN);

    private static final ImmutableMap<Criterion.Type, PiMatchFieldId> CRITERION_MAP =
            ImmutableMap.<Criterion.Type, PiMatchFieldId>builder()
                    .put(Criterion.Type.IN_PORT, FabricConstants.HDR_IG_PORT)
                    .put(Criterion.Type.ETH_DST, FabricConstants.HDR_ETH_DST)
                    .put(Criterion.Type.ETH_SRC, FabricConstants.HDR_ETH_SRC)
                    .put(Criterion.Type.ETH_DST_MASKED, FabricConstants.HDR_ETH_DST)
                    .put(Criterion.Type.ETH_SRC_MASKED, FabricConstants.HDR_ETH_SRC)
                    .put(Criterion.Type.ETH_TYPE, FabricConstants.HDR_ETH_TYPE)
                    .put(Criterion.Type.MPLS_LABEL, FabricConstants.HDR_MPLS_LABEL)
                    .put(Criterion.Type.VLAN_VID, FabricConstants.HDR_VLAN_ID)
                    .put(Criterion.Type.INNER_VLAN_VID, FabricConstants.HDR_INNER_VLAN_ID)
                    .put(Criterion.Type.IPV4_DST, FabricConstants.HDR_IPV4_DST)
                    .put(Criterion.Type.IPV4_SRC, FabricConstants.HDR_IPV4_SRC)
                    .put(Criterion.Type.IPV6_DST, FabricConstants.HDR_IPV6_DST)
                    .put(Criterion.Type.IP_PROTO, FabricConstants.HDR_IP_PROTO)
                    .put(Criterion.Type.ICMPV6_TYPE, FabricConstants.HDR_ICMP_TYPE)
                    .put(Criterion.Type.ICMPV6_CODE, FabricConstants.HDR_ICMP_CODE)
                    .put(Criterion.Type.UDP_DST, FabricConstants.HDR_L4_DPORT)
                    .put(Criterion.Type.UDP_SRC, FabricConstants.HDR_L4_SPORT)
                    .put(Criterion.Type.UDP_DST_MASKED, FabricConstants.HDR_L4_DPORT)
                    .put(Criterion.Type.UDP_SRC_MASKED, FabricConstants.HDR_L4_SPORT)
                    .put(Criterion.Type.TCP_DST, FabricConstants.HDR_L4_DPORT)
                    .put(Criterion.Type.TCP_SRC, FabricConstants.HDR_L4_SPORT)
                    .put(Criterion.Type.TCP_DST_MASKED, FabricConstants.HDR_L4_DPORT)
                    .put(Criterion.Type.TCP_SRC_MASKED, FabricConstants.HDR_L4_SPORT)
                    .build();

    private static final PiAction NOP = PiAction.builder()
            .withId(FabricConstants.NOP).build();

    private static final ImmutableMap<PiTableId, PiAction> DEFAULT_ACTIONS =
            ImmutableMap.<PiTableId, PiAction>builder()
                    .put(FabricConstants.FABRIC_INGRESS_FORWARDING_ROUTING_V4, NOP)
                    .build();

    private FabricTreatmentInterpreter treatmentInterpreter;

    /**
     * Creates a new instance of this behavior with the given capabilities.
     *
     * @param capabilities capabilities
     */
    public FabricInterpreter(FabricCapabilities capabilities) {
        super(capabilities);
        instantiateTreatmentInterpreter();
    }

    /**
     * Create a new instance of this behaviour. Used by the abstract projectable
     * model (i.e., {@link org.onosproject.net.Device#as(Class)}.
     */
    public FabricInterpreter() {
        super();
    }

    private void instantiateTreatmentInterpreter() {
        this.treatmentInterpreter = new FabricTreatmentInterpreter(this.capabilities);
    }

    @Override
    public void setHandler(DriverHandler handler) {
        super.setHandler(handler);
        instantiateTreatmentInterpreter();
    }

    @Override
    public Optional<PiMatchFieldId> mapCriterionType(Criterion.Type type) {
        return Optional.ofNullable(CRITERION_MAP.get(type));
    }

    @Override
    public Optional<PiTableId> mapFlowRuleTableId(int flowRuleTableId) {
        // The only use case for Index ID->PiTableId is when using the single
        // table pipeliner. fabric.p4 is never used with such pipeliner.
        return Optional.empty();
    }

    @Override
    public PiAction mapTreatment(TrafficTreatment treatment, PiTableId piTableId)
            throws PiInterpreterException {
        if (FORWARDING_CTRL_TBLS.contains(piTableId)) {
            return treatmentInterpreter.mapForwardingTreatment(treatment, piTableId);
        } else if (PRE_NEXT_CTRL_TBLS.contains(piTableId)) {
            return treatmentInterpreter.mapPreNextTreatment(treatment, piTableId);
        } else if (ACL_CTRL_TBLS.contains(piTableId)) {
            return treatmentInterpreter.mapAclTreatment(treatment, piTableId);
        } else if (NEXT_CTRL_TBLS.contains(piTableId)) {
            return treatmentInterpreter.mapNextTreatment(treatment, piTableId);
        } else if (E_NEXT_CTRL_TBLS.contains(piTableId)) {
            return treatmentInterpreter.mapEgressNextTreatment(treatment, piTableId);
        } else {
            throw new PiInterpreterException(format(
                    "Treatment mapping not supported for table '%s'", piTableId));
        }
    }

    private PiPacketOperation createPiPacketOperation(
            ByteBuffer data, long portNumber, boolean doForwarding)
            throws PiInterpreterException {
        return PiPacketOperation.builder()
                .withType(PACKET_OUT)
                .withData(copyFrom(data))
                .withMetadatas(createPacketMetadata(portNumber, doForwarding))
                .build();
    }

    private Collection<PiPacketMetadata> createPacketMetadata(
            long portNumber, boolean doForwarding)
            throws PiInterpreterException {
        try {
            ImmutableList.Builder<PiPacketMetadata> builder = ImmutableList.builder();
            // We have observed an issue with p4lang/PI and BMv2 where in
            // presence of multiple metadata fields, the PI implementation for
            // BMv2 provides an erroneous serialization of the packet-out
            // header, an hence affects the parsing/forwarding behavior. As a
            // workaround, since we cannot control the order of fields in the
            // p4runtime.PacketOut message, we modify the interpreter to only
            // add one field, egress_port or do_forwarding. Both fields
            // are treated as mutually exclusive by the P4 pipeline, so the
            // operation is safe. This is against the P4Runtime spec (all fields
            // should be provided), but supported by bmv2 (unset fields are
            // initialized to zero).
            if (portNumber >= 0) {
                // 0 is a valid port number.
                builder.add(PiPacketMetadata.builder()
                        .withId(FabricConstants.EGRESS_PORT)
                        .withValue(copyFrom(portNumber)
                                .fit(PORT_BITWIDTH))
                        .build());
            }
            if (doForwarding) {
                builder.add(PiPacketMetadata.builder()
                        .withId(FabricConstants.DO_FORWARDING)
                        .withValue(copyFrom(ONE))
                        .build());
            }
            return builder.build();
        } catch (ImmutableByteSequence.ByteSequenceTrimException e) {
            throw new PiInterpreterException(format(
                    "Port number '%d' too big, %s", portNumber, e.getMessage()));
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
            if (outInst.port().equals(TABLE)) {
                // Logical port. Forward using the switch tables like a regular packet.
                builder.add(createPiPacketOperation(packet.data(), -1, true));
            } else if (outInst.port().equals(FLOOD)) {
                // Logical port. Create a packet operation for each switch port.
                final DeviceService deviceService = handler().get(DeviceService.class);
                for (Port port : deviceService.getPorts(packet.sendThrough())) {
                    builder.add(createPiPacketOperation(packet.data(), port.number().toLong(), false));
                }
            } else if (outInst.port().isLogical()) {
                throw new PiInterpreterException(format(
                        "Output on logical port '%s' not supported", outInst.port()));
            } else {
                // Send as-is to given port bypassing all switch tables.
                builder.add(createPiPacketOperation(packet.data(), outInst.port().toLong(), false));
            }
        }
        return builder.build();
    }

    @Override
    public InboundPacket mapInboundPacket(PiPacketOperation packetIn, DeviceId deviceId) throws PiInterpreterException {
        // Assuming that the packet is ethernet, which is fine since fabric.p4
        // can deparse only ethernet packets.
        Ethernet ethPkt;
        try {
            ethPkt = Ethernet.deserializer().deserialize(packetIn.data().asArray(), 0,
                                                         packetIn.data().size());
        } catch (DeserializationException dex) {
            throw new PiInterpreterException(dex.getMessage());
        }

        // Returns the ingress port packet metadata.
        Optional<PiPacketMetadata> packetMetadata = packetIn.metadatas()
                .stream().filter(m -> m.id().equals(FabricConstants.INGRESS_PORT))
                .findFirst();

        if (packetMetadata.isPresent()) {
            ImmutableByteSequence portByteSequence = packetMetadata.get().value();
            short s = portByteSequence.asReadOnlyBuffer().getShort();
            ConnectPoint receivedFrom = new ConnectPoint(deviceId, PortNumber.portNumber(s));
            if (!receivedFrom.port().hasName()) {
                receivedFrom = translateSwitchPort(receivedFrom);
            }
            ByteBuffer rawData = ByteBuffer.wrap(packetIn.data().asArray());
            return new DefaultInboundPacket(receivedFrom, ethPkt, rawData);
        } else {
            throw new PiInterpreterException(format(
                    "Missing metadata '%s' in packet-in received from '%s': %s",
                    FabricConstants.INGRESS_PORT, deviceId, packetIn));
        }
    }

    @Override
    public Optional<PiAction> getOriginalDefaultAction(PiTableId tableId) {
        return Optional.ofNullable(DEFAULT_ACTIONS.get(tableId));
    }

    @Override
    public Optional<Long> mapLogicalPort(PortNumber port) {
        if (!port.equals(CONTROLLER)) {
            return Optional.empty();
        }
        return capabilities.cpuPort();
    }

    /* Connect point generated using sb metadata does not have port name
       we use the device service as translation service */
    private ConnectPoint translateSwitchPort(ConnectPoint connectPoint) {
        final DeviceService deviceService = handler().get(DeviceService.class);
        if (deviceService == null) {
            log.warn("Unable to translate switch port due to DeviceService not available");
            return connectPoint;
        }
        Port devicePort = deviceService.getPort(connectPoint);
        if (devicePort != null) {
            return new ConnectPoint(connectPoint.deviceId(), devicePort.number());
        }
        return connectPoint;
    }
}
