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

package org.onosproject.p4runtime.model;

import com.google.common.collect.ImmutableMap;
import org.onosproject.net.pi.model.PiActionProfileId;
import org.onosproject.net.pi.model.PiActionProfileModel;
import org.onosproject.net.pi.model.PiCounterId;
import org.onosproject.net.pi.model.PiCounterModel;
import org.onosproject.net.pi.model.PiMeterId;
import org.onosproject.net.pi.model.PiMeterModel;
import org.onosproject.net.pi.model.PiPacketOperationModel;
import org.onosproject.net.pi.model.PiPacketOperationType;
import org.onosproject.net.pi.model.PiPipelineModel;
import org.onosproject.net.pi.model.PiRegisterId;
import org.onosproject.net.pi.model.PiRegisterModel;
import org.onosproject.net.pi.model.PiTableId;
import org.onosproject.net.pi.model.PiTableModel;

import java.util.Collection;
import java.util.Objects;
import java.util.Optional;

import static com.google.common.base.MoreObjects.toStringHelper;

/**
 * Implementation of PiPipelineModel for P4Runtime.
 */
final class P4PipelineModel implements PiPipelineModel {

    private final ImmutableMap<PiTableId, PiTableModel> tables;
    private final ImmutableMap<PiCounterId, PiCounterModel> counters;
    private final ImmutableMap<PiMeterId, PiMeterModel> meters;
    private final ImmutableMap<PiRegisterId, PiRegisterModel> registers;
    private final ImmutableMap<PiActionProfileId, PiActionProfileModel> actionProfiles;
    private final ImmutableMap<PiPacketOperationType, PiPacketOperationModel> packetOperations;
    private final String architecture;
    private final int fingerprint;

    P4PipelineModel(
            ImmutableMap<PiTableId, PiTableModel> tables,
            ImmutableMap<PiCounterId, PiCounterModel> counters,
            ImmutableMap<PiMeterId, PiMeterModel> meters,
            ImmutableMap<PiRegisterId, PiRegisterModel> registers,
            ImmutableMap<PiActionProfileId, PiActionProfileModel> actionProfiles,
            ImmutableMap<PiPacketOperationType, PiPacketOperationModel> packetOperations,
            String architecture,
            int fingerprint) {
        this.tables = tables;
        this.counters = counters;
        this.meters = meters;
        this.registers = registers;
        this.actionProfiles = actionProfiles;
        this.packetOperations = packetOperations;
        this.fingerprint = fingerprint;
        this.architecture = architecture;
    }

    @Override
    public Optional<String> architecture() {
        return Optional.ofNullable(this.architecture);
    }

    @Override
    public Optional<PiTableModel> table(PiTableId tableId) {
        return Optional.ofNullable(tables.get(tableId));
    }

    @Override
    public Collection<PiTableModel> tables() {
        return tables.values();
    }

    @Override
    public Optional<PiCounterModel> counter(PiCounterId counterId) {
        return Optional.ofNullable(counters.get(counterId));
    }

    @Override
    public Collection<PiCounterModel> counters() {
        return counters.values();
    }

    @Override
    public Optional<PiMeterModel> meter(PiMeterId meterId) {
        return Optional.ofNullable(meters.get(meterId));
    }

    @Override
    public Collection<PiMeterModel> meters() {
        return meters.values();
    }

    @Override
    public Optional<PiRegisterModel> register(PiRegisterId registerId) {
        return Optional.ofNullable(registers.get(registerId));
    }

    @Override
    public Collection<PiRegisterModel> registers() {
        return registers.values();
    }

    @Override
    public Optional<PiActionProfileModel> actionProfiles(PiActionProfileId actionProfileId) {
        return Optional.ofNullable(actionProfiles.get(actionProfileId));
    }

    @Override
    public Collection<PiActionProfileModel> actionProfiles() {
        return actionProfiles.values();
    }

    @Override
    public Optional<PiPacketOperationModel> packetOperationModel(PiPacketOperationType type) {
        return Optional.ofNullable(packetOperations.get(type));

    }

    @Override
    public int hashCode() {
        // NOTE: that the fingerprint is derived by hashing the p4Info file
        //       this because the hashcode is not deterministic across multiple
        //       JVMs instance. This hashcode is also used to derive the fingerprint
        //       of the pipeconf.
        return fingerprint;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final P4PipelineModel other = (P4PipelineModel) obj;
        return Objects.equals(this.tables, other.tables)
                && Objects.equals(this.counters, other.counters)
                && Objects.equals(this.meters, other.meters)
                && Objects.equals(this.registers, other.registers)
                && Objects.equals(this.actionProfiles, other.actionProfiles)
                && Objects.equals(this.packetOperations, other.packetOperations)
                && this.fingerprint == other.fingerprint;
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("tables", tables.values())
                .add("counters", counters.values())
                .add("meters", meters.values())
                .add("registers", registers.values())
                .add("actionProfiles", actionProfiles.values())
                .add("packetOperations", packetOperations.values())
                .add("fingerprint", fingerprint)
                .add("architecture", architecture)
                .toString();
    }
}
