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
package org.onosproject.net.flow;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import org.onlab.packet.EthType;
import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onlab.packet.MplsLabel;
import org.onlab.packet.TpPort;
import org.onlab.packet.VlanId;
import org.onosproject.core.GroupId;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.flow.instructions.ExtensionTreatment;
import org.onosproject.net.flow.instructions.Instruction;
import org.onosproject.net.flow.instructions.Instructions;
import org.onosproject.net.meter.MeterId;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;
import org.onosproject.net.pi.runtime.PiTableAction;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Default traffic treatment implementation.
 */
public final class DefaultTrafficTreatment implements TrafficTreatment {

    private final List<Instruction> immediate;
    private final List<Instruction> deferred;
    private final List<Instruction> all;
    private final Instructions.TableTypeTransition table;
    private final Instructions.MetadataInstruction meta;
    private final Instructions.StatTriggerInstruction statTrigger;

    private final boolean hasClear;

    private static final DefaultTrafficTreatment EMPTY
            = new DefaultTrafficTreatment(ImmutableList.of(Instructions.createNoAction()));
    private final Set<Instructions.MeterInstruction> meter;

    /**
     * Creates a new traffic treatment from the specified list of instructions.
     *
     * @param immediate immediate instructions
     */
    private DefaultTrafficTreatment(List<Instruction> immediate) {
        this.immediate = ImmutableList.copyOf(checkNotNull(immediate));
        this.deferred = ImmutableList.of();
        this.all = this.immediate;
        this.hasClear = false;
        this.table = null;
        this.meta = null;
        this.meter = ImmutableSet.of();
        this.statTrigger = null;
    }

    /**
     * Creates a new traffic treatment from the specified list of instructions.
     *
     * @param deferred deferred instructions
     * @param immediate immediate instructions
     * @param table table transition instruction
     * @param clear instruction to clear the deferred actions list
     */
    private DefaultTrafficTreatment(List<Instruction> deferred,
                                    List<Instruction> immediate,
                                    Instructions.TableTypeTransition table,
                                    boolean clear,
                                    Instructions.MetadataInstruction meta,
                                    Set<Instructions.MeterInstruction> meters,
                                    Instructions.StatTriggerInstruction statTrigger
                                    ) {
        this.immediate = ImmutableList.copyOf(checkNotNull(immediate));
        this.deferred = ImmutableList.copyOf(checkNotNull(deferred));
        this.all = new ImmutableList.Builder<Instruction>()
                .addAll(immediate)
                .addAll(deferred)
                .build();
        this.table = table;
        this.meta = meta;
        this.hasClear = clear;
        this.meter = ImmutableSet.copyOf(meters);
        this.statTrigger = statTrigger;
    }

    @Override
    public List<Instruction> deferred() {
        return deferred;
    }

    @Override
    public List<Instruction> immediate() {
        return immediate;
    }

    @Override
    public List<Instruction> allInstructions() {
        return all;
    }

    @Override
    public Instructions.TableTypeTransition tableTransition() {
        return table;
    }

    @Override
    public boolean clearedDeferred() {
        return hasClear;
    }

    @Override
    public Instructions.MetadataInstruction writeMetadata() {
        return meta;
    }

    @Override
    public Instructions.StatTriggerInstruction statTrigger() {
        return statTrigger;
    }

    @Override
    public Instructions.MeterInstruction metered() {
        if (meter.isEmpty()) {
            return null;
        }
        return meter.iterator().next();
    }

    @Override
    public Set<Instructions.MeterInstruction> meters() {
        return meter;
    }

    /**
     * Returns a new traffic treatment builder.
     *
     * @return traffic treatment builder
     */
    public static TrafficTreatment.Builder builder() {
        return new Builder();
    }

    /**
     * Returns an empty traffic treatment.
     *
     * @return empty traffic treatment
     */
    public static TrafficTreatment emptyTreatment() {
        return EMPTY;
    }

    /**
     * Returns a new traffic treatment builder primed to produce entities
     * patterned after the supplied treatment.
     *
     * @param treatment base treatment
     * @return traffic treatment builder
     */
    public static TrafficTreatment.Builder builder(TrafficTreatment treatment) {
        return new Builder(treatment);
    }

    //FIXME: Order of instructions may affect hashcode
    @Override
    public int hashCode() {
        return Objects.hash(immediate, deferred, table, meta);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof DefaultTrafficTreatment) {
            DefaultTrafficTreatment that = (DefaultTrafficTreatment) obj;
            return Objects.equals(immediate, that.immediate) &&
                    Objects.equals(deferred, that.deferred) &&
                    Objects.equals(table, that.table) &&
                    Objects.equals(meta, that.meta);

        }
        return false;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("immediate", immediate)
                .add("deferred", deferred)
                .add("transition", table == null ? "None" : table.toString())
                .add("meter", meter == null ? "None" : meter)
                .add("cleared", hasClear)
                .add("StatTrigger", statTrigger)
                .add("metadata", meta)
                .toString();
    }

    /**
     * Builds a list of treatments following the following order.
     * Modifications -&gt; Group -&gt; Output (including drop)
     */
    public static final class Builder implements TrafficTreatment.Builder {

        boolean clear = false;

        Instructions.TableTypeTransition table;

        Instructions.MetadataInstruction meta;

        Set<Instructions.MeterInstruction> meter = Sets.newHashSet();

        Instructions.StatTriggerInstruction statTrigger;

        List<Instruction> deferred = new ArrayList<>();

        List<Instruction> immediate = new ArrayList<>();

        List<Instruction> current = immediate;

        // Creates a new builder
        private Builder() {
        }

        // Creates a new builder based off an existing treatment
        private Builder(TrafficTreatment treatment) {
            deferred();
            treatment.deferred().forEach(i -> add(i));

            immediate();
            treatment.immediate().stream()
                    // NOACTION will get re-added if there are no other actions
                    .filter(i -> i.type() != Instruction.Type.NOACTION)
                    .forEach(i -> add(i));

            clear = treatment.clearedDeferred();
        }

        @Override
        public Builder add(Instruction instruction) {

            switch (instruction.type()) {
                case NOACTION:
                case OUTPUT:
                case GROUP:
                case QUEUE:
                case L0MODIFICATION:
                case L1MODIFICATION:
                case L2MODIFICATION:
                case L3MODIFICATION:
                case L4MODIFICATION:
                case PROTOCOL_INDEPENDENT:
                case EXTENSION:
                case TRUNCATE:
                    current.add(instruction);
                    break;
                case TABLE:
                    table = (Instructions.TableTypeTransition) instruction;
                    break;
                case METADATA:
                    meta = (Instructions.MetadataInstruction) instruction;
                    break;
                case METER:
                    meter.add((Instructions.MeterInstruction) instruction);
                    break;
                case STAT_TRIGGER:
                    statTrigger = (Instructions.StatTriggerInstruction) instruction;
                    break;
                default:
                    throw new IllegalArgumentException("Unknown instruction type: " +
                                                               instruction.type());
            }

            return this;
        }

        /**
         * Add a NOACTION when DROP instruction is explicitly specified.
         *
         * @return the traffic treatment builder
         */
        @Override
        public Builder drop() {
            return add(Instructions.createNoAction());
        }

        /**
         * Add a NOACTION when no instruction is specified.
         *
         * @return the traffic treatment builder
         */
        private Builder noAction() {
            return add(Instructions.createNoAction());
        }

        @Override
        public Builder punt() {
            return add(Instructions.createOutput(PortNumber.CONTROLLER));
        }

        @Override
        public Builder setOutput(PortNumber number) {
            return add(Instructions.createOutput(number));
        }

        @Override
        public Builder setEthSrc(MacAddress addr) {
            return add(Instructions.modL2Src(addr));
        }

        @Override
        public Builder setEthDst(MacAddress addr) {
            return add(Instructions.modL2Dst(addr));
        }

        @Override
        public Builder setVlanId(VlanId id) {
            return add(Instructions.modVlanId(id));
        }

        @Override
        public Builder setVlanPcp(Byte pcp) {
            return add(Instructions.modVlanPcp(pcp));
        }

        @Override
        public Builder setIpSrc(IpAddress addr) {
            return add(Instructions.modL3Src(addr));
        }

        @Override
        public Builder setIpDst(IpAddress addr) {
            return add(Instructions.modL3Dst(addr));
        }

        @Override
        public Builder decNwTtl() {
            return add(Instructions.decNwTtl());
        }

        @Override
        public Builder copyTtlIn() {
            return add(Instructions.copyTtlIn());
        }

        @Override
        public Builder copyTtlOut() {
            return add(Instructions.copyTtlOut());
        }

        @Override
        public Builder pushMpls() {
            return add(Instructions.pushMpls());
        }

        @Override
        public Builder popMpls() {
            return add(Instructions.popMpls());
        }

        @Override
        public Builder popMpls(EthType etherType) {
            return add(Instructions.popMpls(etherType));
        }

        @Override
        public Builder setMpls(MplsLabel mplsLabel) {
            return add(Instructions.modMplsLabel(mplsLabel));
        }

        @Override
        public Builder setMplsBos(boolean mplsBos) {
            return add(Instructions.modMplsBos(mplsBos));
        }

        @Override
        public Builder decMplsTtl() {
            return add(Instructions.decMplsTtl());
        }


        @Override
        public Builder group(GroupId groupId) {
            return add(Instructions.createGroup(groupId));
        }

        @Override
        public Builder setQueue(long queueId) {
            return add(Instructions.setQueue(queueId, null));
        }

        @Override
        public Builder setQueue(long queueId, PortNumber port) {
            return add(Instructions.setQueue(queueId, port));
        }

        @Override
        public TrafficTreatment.Builder meter(MeterId meterId) {
            return add(Instructions.meterTraffic(meterId));
        }

        @Override
        public Builder popVlan() {
            return add(Instructions.popVlan());
        }

        @Override
        public Builder pushVlan() {
            return add(Instructions.pushVlan());
        }

        @Override
        public Builder pushVlan(EthType ethType) {
            return add(Instructions.pushVlan(ethType));
        }

        @Override
        public Builder transition(Integer tableId) {
            return add(Instructions.transition(tableId));
        }

        @Override
        public Builder immediate() {
            current = immediate;
            return this;
        }

        @Override
        public Builder deferred() {
            current = deferred;
            return this;
        }

        @Override
        public Builder wipeDeferred() {
            clear = true;
            return this;
        }

        @Override
        public Builder notWipeDeferred() {
            clear = false;
            return this;
        }

        @Override
        public Builder writeMetadata(long metadata, long metadataMask) {
            return add(Instructions.writeMetadata(metadata, metadataMask));
        }

        @Override
        public Builder setTunnelId(long tunnelId) {
            return add(Instructions.modTunnelId(tunnelId));
        }

        @Override
        public TrafficTreatment.Builder setTcpSrc(TpPort port) {
            return add(Instructions.modTcpSrc(port));
        }

        @Override
        public TrafficTreatment.Builder setTcpDst(TpPort port) {
            return add(Instructions.modTcpDst(port));
        }

        @Override
        public TrafficTreatment.Builder setUdpSrc(TpPort port) {
            return add(Instructions.modUdpSrc(port));
        }

        @Override
        public TrafficTreatment.Builder setUdpDst(TpPort port) {
            return add(Instructions.modUdpDst(port));
        }

        @Override
        public Builder setArpSpa(IpAddress addr) {
            return add(Instructions.modArpSpa(addr));
        }

        @Override
        public Builder setArpSha(MacAddress addr) {
            return add(Instructions.modArpSha(addr));
        }

        @Override
        public Builder setArpTpa(IpAddress addr) {
            return add(Instructions.modArpTpa(addr));
        }

        @Override
        public Builder setArpTha(MacAddress addr) {
            return add(Instructions.modArpTha(addr));
        }

        @Override
        public Builder setArpOp(short op) {
            return add(Instructions.modL3ArpOp(op));
        }

        @Override
        public Builder piTableAction(PiTableAction piTableAction) {
            return add(Instructions.piTableAction(piTableAction));
        }

        @Override
        public TrafficTreatment.Builder setIpDscp(byte dscpValue) {
            return add(Instructions.modIpDscp(dscpValue));
        }

        @Override
        public TrafficTreatment.Builder extension(ExtensionTreatment extension,
                                                  DeviceId deviceId) {
            return add(Instructions.extension(extension, deviceId));
        }

        @Override
        public TrafficTreatment.Builder statTrigger(Map<StatTriggerField, Long> statTriggerFieldMap,
                                                    StatTriggerFlag statTriggerFlag) {
            return add(Instructions.statTrigger(statTriggerFieldMap, statTriggerFlag));
        }

        @Override
        public TrafficTreatment.Builder addTreatment(TrafficTreatment treatment) {
            List<Instruction> previous = current;
            deferred();
            treatment.deferred().forEach(i -> add(i));

            immediate();
            treatment.immediate().stream()
                    // NOACTION will get re-added if there are no other actions
                    .filter(i -> i.type() != Instruction.Type.NOACTION)
                    .forEach(i -> add(i));

            clear = treatment.clearedDeferred();
            current = previous;
            return this;
        }

        @Override
        public TrafficTreatment.Builder truncate(int maxLen) {
            return add(Instructions.truncate(maxLen));
        }

        @Override
        public TrafficTreatment build() {
            if (deferred.isEmpty() && immediate.isEmpty()
                    && table == null && !clear) {
                immediate();
                noAction();
            }
            return new DefaultTrafficTreatment(deferred, immediate, table, clear, meta, meter, statTrigger);
        }

    }

}
