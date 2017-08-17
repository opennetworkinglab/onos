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
import org.onosproject.net.pi.model.PiPipeconf;
import org.onosproject.net.pi.runtime.PiActionProfileId;
import org.onosproject.net.pi.runtime.PiCounterCellData;
import org.onosproject.net.pi.runtime.PiCounterCellId;
import org.onosproject.net.pi.runtime.PiCounterId;
import org.onosproject.net.pi.runtime.PiActionGroup;
import org.onosproject.net.pi.runtime.PiActionGroupMember;
import org.onosproject.net.pi.runtime.PiPacketOperation;
import org.onosproject.net.pi.runtime.PiTableEntry;
import org.onosproject.net.pi.runtime.PiTableId;

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
     * Sets the pipeline configuration defined by the given pipeconf for the given target-specific configuration
     * extension type (e.g. {@link PiPipeconf.ExtensionType#BMV2_JSON}, or {@link PiPipeconf.ExtensionType#TOFINO_BIN}).
     * This method should be called before any other method of this client.
     *
     * @param pipeconf            pipeconf
     * @param targetConfigExtType extension type of the target-specific configuration
     * @return a completable future of a boolean, true if the operations was successful, false otherwise.
     */
    CompletableFuture<Boolean> setPipelineConfig(PiPipeconf pipeconf, PiPipeconf.ExtensionType targetConfigExtType);

    /**
     * Initializes the stream channel, after which all messages received from the device will be notified using the
     * {@link P4RuntimeController} event listener.
     *
     * @return a completable future of a boolean, true if the operations was successful, false otherwise.
     */
    CompletableFuture<Boolean> initStreamChannel();

    /**
     * Performs the given write operation for the given table entries and pipeconf.
     *
     * @param entries  table entries
     * @param opType   operation type
     * @param pipeconf pipeconf currently deployed on the device
     * @return true if the operation was successful, false otherwise.
     */
    CompletableFuture<Boolean> writeTableEntries(Collection<PiTableEntry> entries, WriteOperationType opType,
                                                 PiPipeconf pipeconf);

    /**
     * Dumps all entries currently installed in the given table, for the given pipeconf.
     *
     * @param tableId  table identifier
     * @param pipeconf pipeconf currently deployed on the device
     * @return completable future of a collection of table entries
     */
    CompletableFuture<Collection<PiTableEntry>> dumpTable(PiTableId tableId, PiPipeconf pipeconf);

    /**
     * Executes a packet-out operation for the given pipeconf.
     *
     * @param packet   packet-out operation to be performed by the device
     * @param pipeconf pipeconf currently deployed on the device
     * @return a completable future of a boolean, true if the operations was successful, false otherwise.
     */
    CompletableFuture<Boolean> packetOut(PiPacketOperation packet, PiPipeconf pipeconf);

    /**
     * Returns the value of all counter cells for the given set of counter identifiers and pipeconf.
     *
     * @param counterIds counter identifiers
     * @param pipeconf   pipeconf
     * @return collection of counter data
     */
    CompletableFuture<Collection<PiCounterCellData>> readAllCounterCells(Set<PiCounterId> counterIds,
                                                                         PiPipeconf pipeconf);

    /**
     * Returns a collection of counter data corresponding to the given set of counter cell identifiers, for the given
     * pipeconf.
     *
     * @param cellIds set of counter cell identifiers
     * @param pipeconf   pipeconf
     * @return collection of counter data
     */
    CompletableFuture<Collection<PiCounterCellData>> readCounterCells(Set<PiCounterCellId> cellIds,
                                                                      PiPipeconf pipeconf);

    /**
     * Performs the given write operation for the given action group members and pipeconf.
     *
     * @param group action group
     * @param members the collection of action group members
     * @param opType write operation type
     * @param pipeconf the pipeconf currently deployed on the device
     * @return true if the operation was successful, false otherwise
     */
    CompletableFuture<Boolean> writeActionGroupMembers(PiActionGroup group,
                                                       Collection<PiActionGroupMember> members,
                                                       WriteOperationType opType,
                                                       PiPipeconf pipeconf);

    /**
     * Performs the given write operation for the given action group and pipeconf.
     *
     * @param group the action group
     * @param opType write operation type
     * @param pipeconf the pipeconf currently deployed on the device
     * @return true if the operation was successful, false otherwise
     */
    CompletableFuture<Boolean> writeActionGroup(PiActionGroup group,
                                                WriteOperationType opType,
                                                PiPipeconf pipeconf);

    /**
     * Dumps all groups currently installed for the given action profile.
     *
     * @param actionProfileId the action profile id
     * @param pipeconf the pipeconf currently deployed on the device
     * @return completable future of a collection of groups
     */
    CompletableFuture<Collection<PiActionGroup>> dumpGroups(PiActionProfileId actionProfileId,
                                                            PiPipeconf pipeconf);

    /**
     * Shutdown the client by terminating any active RPC such as the stream channel.
     */
    void shutdown();

    // TODO: work in progress.
}
