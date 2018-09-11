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
import org.onosproject.grpc.api.GrpcClient;
import org.onosproject.net.pi.model.PiActionProfileId;
import org.onosproject.net.pi.model.PiCounterId;
import org.onosproject.net.pi.model.PiMeterId;
import org.onosproject.net.pi.model.PiPipeconf;
import org.onosproject.net.pi.model.PiTableId;
import org.onosproject.net.pi.runtime.PiActionProfileGroup;
import org.onosproject.net.pi.runtime.PiActionProfileMember;
import org.onosproject.net.pi.runtime.PiActionProfileMemberId;
import org.onosproject.net.pi.runtime.PiCounterCell;
import org.onosproject.net.pi.runtime.PiCounterCellId;
import org.onosproject.net.pi.runtime.PiMeterCellConfig;
import org.onosproject.net.pi.runtime.PiMeterCellId;
import org.onosproject.net.pi.runtime.PiMulticastGroupEntry;
import org.onosproject.net.pi.runtime.PiPacketOperation;
import org.onosproject.net.pi.runtime.PiTableEntry;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * Client to control a P4Runtime device.
 */
@Beta
public interface P4RuntimeClient extends GrpcClient {

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
     * Starts the Stream RPC with the device.
     *
     * @return completable future containing true if the operation was
     * successful, false otherwise.
     */
    CompletableFuture<Boolean> startStreamChannel();

    /**
     * Returns true if the stream RPC is active, false otherwise.
     *
     * @return boolean
     */
    boolean isStreamChannelOpen();

    /**
     * Sends a master arbitration update to the device with a new election ID
     * that is guaranteed to be the highest value between all clients.
     *
     * @return completable future containing true if the operation was
     * successful; false otherwise
     */
    CompletableFuture<Boolean> becomeMaster();

    /**
     * Returns true if this client is master for the device, false otherwise.
     *
     * @return boolean
     */
    boolean isMaster();

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
     * Returns true if the device has the given pipeconf set, false otherwise.
     * Equality is based on the P4Info extension of the pipeconf as well as the
     * given device data byte buffer.
     * <p>
     * This method is expected to return {@code true} if invoked after calling
     * {@link #setPipelineConfig(PiPipeconf, ByteBuffer)} with the same
     * parameters.
     *
     * @param pipeconf   pipeconf
     * @param deviceData target-specific data
     * @return boolean
     */
    boolean isPipelineConfigSet(PiPipeconf pipeconf, ByteBuffer deviceData);

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
            List<PiTableEntry> entries, WriteOperationType opType,
            PiPipeconf pipeconf);

    /**
     * Dumps all entries currently installed in the given tables, for the given
     * pipeconf. If defaultEntries is set to true only the default action
     * entries will be returned, otherwise non-default entries will be
     * considered.
     *
     * @param tableIds       table identifiers
     * @param defaultEntries true to read default entries, false for
     *                       non-default
     * @param pipeconf       pipeconf currently deployed on the device
     * @return completable future of a list of table entries
     */
    CompletableFuture<List<PiTableEntry>> dumpTables(
            Set<PiTableId> tableIds, boolean defaultEntries, PiPipeconf pipeconf);

    /**
     * Dumps entries from all tables, for the given pipeconf.
     *
     * @param pipeconf pipeconf currently deployed on the device
     * @return completable future of a list of table entries
     */
    CompletableFuture<List<PiTableEntry>> dumpAllTables(PiPipeconf pipeconf);

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
     * @return list of counter data
     */
    CompletableFuture<List<PiCounterCell>> readAllCounterCells(
            Set<PiCounterId> counterIds, PiPipeconf pipeconf);

    /**
     * Returns a list of counter data corresponding to the given set of counter
     * cell identifiers, for the given pipeconf.
     *
     * @param cellIds  set of counter cell identifiers
     * @param pipeconf pipeconf
     * @return list of counter data
     */
    CompletableFuture<List<PiCounterCell>> readCounterCells(
            Set<PiCounterCellId> cellIds, PiPipeconf pipeconf);

    /**
     * Performs the given write operation for the given action profile members
     * and pipeconf.
     *
     * @param members   action profile members
     * @param opType    write operation type
     * @param pipeconf  the pipeconf currently deployed on the device
     * @return true if the operation was successful, false otherwise
     */
    CompletableFuture<Boolean> writeActionProfileMembers(
            List<PiActionProfileMember> members,
            WriteOperationType opType, PiPipeconf pipeconf);

    /**
     * Performs the given write operation for the given action profile group and
     * pipeconf.
     *
     * @param group         the action profile group
     * @param opType        write operation type
     * @param pipeconf      the pipeconf currently deployed on the device
     * @param maxMemberSize the maximum number of members that can be added to
     *                      the group. This is meaningful only if it's an INSERT
     *                      operation, otherwise its value should be 0
     * @return true if the operation was successful, false otherwise
     */
    CompletableFuture<Boolean> writeActionProfileGroup(
            PiActionProfileGroup group,
            WriteOperationType opType,
            PiPipeconf pipeconf,
            int maxMemberSize);

    /**
     * Dumps all groups currently installed in the given action profile.
     *
     * @param actionProfileId the action profile id
     * @param pipeconf        the pipeconf currently deployed on the device
     * @return completable future of a list of groups
     */
    CompletableFuture<List<PiActionProfileGroup>> dumpActionProfileGroups(
            PiActionProfileId actionProfileId, PiPipeconf pipeconf);

    /**
     * Dumps all action profile member IDs for a given action profile.
     *
     * @param actionProfileId action profile ID
     * @param pipeconf        pipeconf
     * @return future of list of action profile member ID
     */
    CompletableFuture<List<PiActionProfileMemberId>> dumpActionProfileMemberIds(
            PiActionProfileId actionProfileId, PiPipeconf pipeconf);

    /**
     * Removes the given members from the given action profile. Returns the list
     * of successfully removed members.
     *
     * @param actionProfileId action profile ID
     * @param memberIds       member IDs
     * @param pipeconf        pipeconf
     * @return list of member IDs that were successfully removed from the device
     */
    CompletableFuture<List<PiActionProfileMemberId>> removeActionProfileMembers(
            PiActionProfileId actionProfileId,
            List<PiActionProfileMemberId> memberIds,
            PiPipeconf pipeconf);

    /**
     * Returns the configuration of all meter cells for the given set of meter
     * identifiers and pipeconf.
     *
     * @param meterIds meter identifiers
     * @param pipeconf pipeconf
     * @return list of meter configurations
     */
    CompletableFuture<List<PiMeterCellConfig>> readAllMeterCells(
            Set<PiMeterId> meterIds, PiPipeconf pipeconf);

    /**
     * Returns a list of meter configurations corresponding to the given set of
     * meter cell identifiers, for the given pipeconf.
     *
     * @param cellIds  set of meter cell identifiers
     * @param pipeconf pipeconf
     * @return list of meter configrations
     */
    CompletableFuture<List<PiMeterCellConfig>> readMeterCells(
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
            List<PiMeterCellConfig> cellConfigs, PiPipeconf pipeconf);

    /**
     * Performs the given write operation for the given PI multicast groups
     * entries.
     *
     * @param entries multicast group entries
     * @param opType  write operation type
     * @return true if the operation was successful, false otherwise
     */
    CompletableFuture<Boolean> writePreMulticastGroupEntries(
            List<PiMulticastGroupEntry> entries,
            WriteOperationType opType);

    /**
     * Returns all multicast groups on device.
     *
     * @return multicast groups
     */
    CompletableFuture<List<PiMulticastGroupEntry>> readAllMulticastGroupEntries();
}
