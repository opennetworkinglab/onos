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

package org.onosproject.p4runtime.api;

import com.google.common.annotations.Beta;
import org.onosproject.net.pi.model.PiActionProfileId;
import org.onosproject.net.pi.model.PiCounterId;
import org.onosproject.net.pi.model.PiMeterId;
import org.onosproject.net.pi.model.PiPipeconf;
import org.onosproject.net.pi.model.PiTableId;
import org.onosproject.net.pi.runtime.PiActionGroup;
import org.onosproject.net.pi.runtime.PiActionGroupMember;
import org.onosproject.net.pi.runtime.PiCounterCellData;
import org.onosproject.net.pi.runtime.PiCounterCellId;
import org.onosproject.net.pi.runtime.PiMeterCellConfig;
import org.onosproject.net.pi.runtime.PiMeterCellId;
import org.onosproject.net.pi.runtime.PiPacketOperation;
import org.onosproject.net.pi.runtime.PiTableEntry;

import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * Client to control a P4Runtime device.
 */
@Beta
public interface P4RuntimeClient {

    /**
     * Type of write operation.
     */
    enum WriteOperationType {
        UNSPECIFIED,
        INSERT,
        MODIFY,
        DELETE
    }

    /**
     * Starts the client by starting the Stream RPC with the device.
     *
     * @return completable future containing true if the operation was
     * successful, false otherwise.
     */
    CompletableFuture<Boolean> start();

    /**
     * Shutdowns the client by terminating any active RPC such as the Stream
     * one.
     *
     * @return a completable future to signal the completion of the shutdown
     * procedure
     */
    CompletableFuture<Void> shutdown();

    /**
     * Sends a master arbitration update to the device with a new election ID
     * that is guaranteed to be the highest value between all clients.
     *
     * @return completable future containing true if the operation was
     * successful; false otherwise
     */
    CompletableFuture<Boolean> becomeMaster();

    /**
     * Sets the device pipeline according to the given pipeconf, and for the
     * given byte buffer representing the target-specific data to be used in the
     * P4Runtime's SetPipelineConfig message. This method should be called
     * before any other method of this client.
     *
     * @param pipeconf   pipeconf
     * @param deviceData target-specific data
     * @return a completable future of a boolean, true if the operations was
     * successful, false otherwise.
     */
    CompletableFuture<Boolean> setPipelineConfig(
            PiPipeconf pipeconf, ByteBuffer deviceData);

    /**
     * Performs the given write operation for the given table entries and
     * pipeconf.
     *
     * @param entries  table entries
     * @param opType   operation type
     * @param pipeconf pipeconf currently deployed on the device
     * @return true if the operation was successful, false otherwise.
     */
    CompletableFuture<Boolean> writeTableEntries(
            Collection<PiTableEntry> entries, WriteOperationType opType,
            PiPipeconf pipeconf);

    /**
     * Dumps all entries currently installed in the given table, for the given
     * pipeconf.
     *
     * @param tableId  table identifier
     * @param pipeconf pipeconf currently deployed on the device
     * @return completable future of a collection of table entries
     */
    CompletableFuture<Collection<PiTableEntry>> dumpTable(
            PiTableId tableId, PiPipeconf pipeconf);

    /**
     * Dumps entries from all tables, for the given pipeconf.
     *
     * @param pipeconf pipeconf currently deployed on the device
     * @return completable future of a collection of table entries
     */
    CompletableFuture<Collection<PiTableEntry>> dumpAllTables(PiPipeconf pipeconf);

    /**
     * Executes a packet-out operation for the given pipeconf.
     *
     * @param packet   packet-out operation to be performed by the device
     * @param pipeconf pipeconf currently deployed on the device
     * @return a completable future of a boolean, true if the operations was
     * successful, false otherwise.
     */
    CompletableFuture<Boolean> packetOut(
            PiPacketOperation packet, PiPipeconf pipeconf);

    /**
     * Returns the value of all counter cells for the given set of counter
     * identifiers and pipeconf.
     *
     * @param counterIds counter identifiers
     * @param pipeconf   pipeconf
     * @return collection of counter data
     */
    CompletableFuture<Collection<PiCounterCellData>> readAllCounterCells(
            Set<PiCounterId> counterIds, PiPipeconf pipeconf);

    /**
     * Returns a collection of counter data corresponding to the given set of
     * counter cell identifiers, for the given pipeconf.
     *
     * @param cellIds  set of counter cell identifiers
     * @param pipeconf pipeconf
     * @return collection of counter data
     */
    CompletableFuture<Collection<PiCounterCellData>> readCounterCells(
            Set<PiCounterCellId> cellIds, PiPipeconf pipeconf);

    /**
     * Performs the given write operation for the given action group members and
     * pipeconf.
     *
     * @param profileId action group profile ID
     * @param members   action group members
     * @param opType    write operation type
     * @param pipeconf  the pipeconf currently deployed on the device
     * @return true if the operation was successful, false otherwise
     */
    CompletableFuture<Boolean> writeActionGroupMembers(
            PiActionProfileId profileId, Collection<PiActionGroupMember> members,
            WriteOperationType opType, PiPipeconf pipeconf);

    /**
     * Performs the given write operation for the given action group and
     * pipeconf.
     *
     * @param group    the action group
     * @param opType   write operation type
     * @param pipeconf the pipeconf currently deployed on the device
     * @return true if the operation was successful, false otherwise
     */
    CompletableFuture<Boolean> writeActionGroup(
            PiActionGroup group, WriteOperationType opType, PiPipeconf pipeconf);

    /**
     * Dumps all groups currently installed for the given action profile.
     *
     * @param actionProfileId the action profile id
     * @param pipeconf        the pipeconf currently deployed on the device
     * @return completable future of a collection of groups
     */
    CompletableFuture<Collection<PiActionGroup>> dumpGroups(
            PiActionProfileId actionProfileId, PiPipeconf pipeconf);

    /**
     * Returns the configuration of all meter cells for the given set of meter
     * identifiers and pipeconf.
     *
     * @param meterIds meter identifiers
     * @param pipeconf pipeconf
     * @return collection of meter configurations
     */
    CompletableFuture<Collection<PiMeterCellConfig>> readAllMeterCells(
            Set<PiMeterId> meterIds, PiPipeconf pipeconf);

    /**
     * Returns a collection of meter configurations corresponding to the given
     * set of meter cell identifiers, for the given pipeconf.
     *
     * @param cellIds  set of meter cell identifiers
     * @param pipeconf pipeconf
     * @return collection of meter configrations
     */
    CompletableFuture<Collection<PiMeterCellConfig>> readMeterCells(
            Set<PiMeterCellId> cellIds, PiPipeconf pipeconf);

    /**
     * Performs a write operation for the given meter configurations and
     * pipeconf.
     *
     * @param cellConfigs meter cell configurations
     * @param pipeconf    pipeconf currently deployed on the device
     * @return true if the operation was successful, false otherwise.
     */
    CompletableFuture<Boolean> writeMeterCells(
            Collection<PiMeterCellConfig> cellConfigs, PiPipeconf pipeconf);
}
