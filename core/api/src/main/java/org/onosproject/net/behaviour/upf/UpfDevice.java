/*
 * Copyright 2021-present Open Networking Foundation
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

package org.onosproject.net.behaviour.upf;

import com.google.common.annotations.Beta;

import java.nio.ByteBuffer;
import java.util.Collection;

/**
 * Provides means to update forwarding state to implement a 3GPP User Plane Function.
 */
@Beta
public interface UpfDevice {

    /**
     * Removes any state previously created by this API.
     */
    void cleanUp();

    /**
     * Applies the given UPF entity to the UPF-programmable device.
     *
     * @param entity The UPF entity to be applied.
     * @throws UpfProgrammableException if the given UPF entity can not be applied or
     *                                  the operation is not supported on the given UPF entity.
     */
    void apply(UpfEntity entity) throws UpfProgrammableException;

    /**
     * Reads all the UPF entities of the given type from the UPF-programmable device.
     *
     * @param entityType The type of entities to read.
     * @return A collection of installed UPF entities.
     * @throws UpfProgrammableException if UPF entity type is not available to be read or
     *                                  the operation is not supported on the given UPF entity type.
     */
    Collection<? extends UpfEntity> readAll(UpfEntityType entityType) throws UpfProgrammableException;

    /**
     * Reads the given UPF counter type and index from the UPF-programmable device.
     *
     * @param counterIdx The counter index from which to read.
     * @param type       {@link UpfEntityType} of UPF counter to read
     *                   ({@code COUNTER, INGRESS_COUNTER, EGRESS_COUNTER})
     * @return The content of the UPF counter.
     * @throws UpfProgrammableException if the counter ID is out of bounds.
     */
    UpfCounter readCounter(int counterIdx, UpfEntityType type) throws UpfProgrammableException;

    /**
     * Reads the given UPF counter type contents for all indices that are valid
     * on the UPF-programmable device. {@code maxCounterId} parameter is used to
     * limit the number of counters retrieved from the UPF. If the limit given is
     * larger than the physical limit, the physical limit will be used.
     * A limit of -1 removes limitations, and it is equivalent of calling
     * {@link #readAll(UpfEntityType)} passing the given {@link UpfEntityType}.
     *
     * @param maxCounterIdx Maximum counter index to retrieve from the UPF device.
     * @param type          {@link UpfEntityType} of UPF counter to read
     *                      ({@code COUNTER, INGRESS_COUNTER, EGRESS_COUNTER})
     * @return A collection of UPF counters for all valid hardware counter cells.
     * @throws UpfProgrammableException if the counters are unable to be read.
     */
    Collection<UpfCounter> readCounters(long maxCounterIdx, UpfEntityType type) throws UpfProgrammableException;

    /**
     * Deletes the given UPF entity from the UPF-programmable device.
     *
     * @param entity The UPF entity to be removed.
     * @throws UpfProgrammableException if the given UPF entity is not found or
     *                                  the operation is not supported on the given UPF entity.
     */
    void delete(UpfEntity entity) throws UpfProgrammableException;

    /**
     * Deletes the given UPF entity from the UPF-programmable device.
     *
     * @param entityType The UPF entity type to be removed.
     * @throws UpfProgrammableException if the given UPF entity is not found or
     *                                  the operation is not supported on the given UPF entity.
     */
    void deleteAll(UpfEntityType entityType) throws UpfProgrammableException;

    /**
     * Returns the total number of UPF entities of the given type supported by
     * the UPF-programmable device. For entities that have a direction,returns
     * the total amount of entities including both the downlink and the uplink
     * directions.
     *
     * @param entityType The type of UPF programmable entities to retrieve the size from.
     * @return The total number of supported UPF entities.
     * @throws UpfProgrammableException if the operation is not supported on the given UPF entity.
     */
    long tableSize(UpfEntityType entityType) throws UpfProgrammableException;

    /**
     * Instructs the UPF-programmable device to use GTP-U extension PDU Session Container (PSC) when
     * doing encap of downlink packets, with the given QoS Flow Identifier (QFI).
     *
     * @throws UpfProgrammableException if operation is not available
     */
    void enablePscEncap() throws UpfProgrammableException;

    /**
     * Disable PSC encap previously enabled with {@link #enablePscEncap()}.
     *
     * @throws UpfProgrammableException if operation is not available
     */
    void disablePscEncap() throws UpfProgrammableException;

    /**
     * Sends the given data as a data plane packet-out through this device. Data is expected to
     * contain an Ethernet frame.
     * <p>
     * The device should process the packet through the pipeline tables to select an output port
     * and to apply eventual modifications (e.g., MAC rewrite for routing, pushing a VLAN tag,
     * etc.).
     *
     * @param data Ethernet frame bytes
     * @throws UpfProgrammableException if operation is not available
     */
    void sendPacketOut(ByteBuffer data) throws UpfProgrammableException;
}
