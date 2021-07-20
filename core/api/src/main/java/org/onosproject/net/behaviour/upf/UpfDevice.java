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
     * Remove any state previously created by this API.
     */
    void cleanUp();

    /**
     * Remove all interfaces currently installed on the UPF-programmable device.
     */
    void clearInterfaces();

    /**
     * Remove all UE flows (PDRs, FARs) currently installed on the UPF-programmable device.
     */
    void clearFlows();

    /**
     * Get all ForwardingActionRules currently installed on the UPF-programmable device.
     *
     * @return a collection of installed FARs
     * @throws UpfProgrammableException if FARs are unable to be read
     */
    Collection<ForwardingActionRule> getFars() throws UpfProgrammableException;

    /**
     * Get all PacketDetectionRules currently installed on the UPF-programmable device.
     *
     * @return a collection of installed PDRs
     * @throws UpfProgrammableException if PDRs are unable to be read
     */
    Collection<PacketDetectionRule> getPdrs() throws UpfProgrammableException;

    /**
     * Get all UPF interface lookup entries currently installed on the UPF-programmable device.
     *
     * @return a collection of installed interfaces
     * @throws UpfProgrammableException if interfaces are unable to be read
     */
    Collection<UpfInterface> getInterfaces() throws UpfProgrammableException;

    /**
     * Add a Packet Detection Rule (PDR) to the given device.
     *
     * @param pdr The PDR to be added
     * @throws UpfProgrammableException if the PDR cannot be installed, or the counter index is out
     *                                  of bounds
     */
    void addPdr(PacketDetectionRule pdr) throws UpfProgrammableException;

    /**
     * Remove a previously installed Packet Detection Rule (PDR) from the target device.
     *
     * @param pdr The PDR to be removed
     * @throws UpfProgrammableException if the PDR cannot be found
     */
    void removePdr(PacketDetectionRule pdr) throws UpfProgrammableException;

    /**
     * Add a Forwarding Action Rule (FAR) to the given device.
     *
     * @param far The FAR to be added
     * @throws UpfProgrammableException if the FAR cannot be installed
     */
    void addFar(ForwardingActionRule far) throws UpfProgrammableException;

    /**
     * Remove a previously installed Forwarding Action Rule (FAR) from the target device.
     *
     * @param far The FAR to be removed
     * @throws UpfProgrammableException if the FAR cannot be found
     */
    void removeFar(ForwardingActionRule far) throws UpfProgrammableException;

    /**
     * Install a new interface on the UPF device's interface lookup tables.
     *
     * @param upfInterface the interface to install
     * @throws UpfProgrammableException if the interface cannot be installed
     */
    void addInterface(UpfInterface upfInterface) throws UpfProgrammableException;

    /**
     * Remove a previously installed UPF interface from the target device.
     *
     * @param upfInterface the interface to be removed
     * @throws UpfProgrammableException if the interface cannot be found
     */
    void removeInterface(UpfInterface upfInterface) throws UpfProgrammableException;

    /**
     * Read the the given cell (Counter index) of the PDR counters from the given device.
     *
     * @param counterIdx The counter cell index from which to read
     * @return A structure containing ingress and egress packet and byte counts for the given
     * cellId.
     * @throws UpfProgrammableException if the cell ID is out of bounds
     */
    PdrStats readCounter(int counterIdx) throws UpfProgrammableException;

    /**
     * Return the number of PDR counter cells available. The number of cells in the ingress and
     * egress PDR counters are equivalent.
     *
     * @return PDR counter size
     */
    long pdrCounterSize();

    /**
     * Return the number of maximum number of table entries the FAR table supports.
     *
     * @return the number of FARs that can be installed
     */
    long farTableSize();

    /**
     * Return the total number of table entries the downlink and uplink PDR tables support. Both
     * tables support an equal number of entries, so the total is twice the size of either.
     *
     * @return the total number of PDRs that can be installed
     */
    long pdrTableSize();

    /**
     * Read the counter contents for all cell indices that are valid on the hardware switch.
     * {@code maxCounterId} parameter is used to limit the number of counters
     * retrieved from the UPF device. If the limit given is larger than the
     * physical limit, the physical limit will be used. A limit of -1 removes
     * limitations.
     *
     * @param maxCounterId Maximum counter ID to retrieve from the UPF device.
     * @return A collection of counter values for all valid hardware counter cells
     * @throws UpfProgrammableException if the counters are unable to be read
     */
    Collection<PdrStats> readAllCounters(long maxCounterId) throws UpfProgrammableException;

    /**
     * Instructs the UPF-programmable device to use GTP-U extension PDU Session Container (PSC) when
     * doing encap of downlink packets, with the given QoS Flow Identifier (QFI).
     *
     * @param defaultQfi QFI to be used by default for all encapped packets.
     * @throws UpfProgrammableException if operation is not available
     */
    // FIXME: remove once we expose QFI in logical pipeline
    //  QFI should be set by the SMF using PFCP
    void enablePscEncap(int defaultQfi) throws UpfProgrammableException;

    /**
     * Disable PSC encap previously enabled with {@link #enablePscEncap(int)}.
     *
     * @throws UpfProgrammableException if operation is not available
     */
    // FIXME: remove once we expose QFI in logical pipeline
    //  QFI should be set by the SMF using PFCP
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
