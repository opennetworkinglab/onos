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
package org.onosproject.incubator.net.l2monitoring.cfm;

import java.time.Duration;

import org.onlab.packet.MacAddress;
import org.onosproject.incubator.net.l2monitoring.cfm.SenderIdTlv.SenderIdTlvType;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MepId;

/**
 * Defined in IEEE 802.1Q Section 12.14.7.6.3 Read MEP Database - Outputs.
 *
 */
public interface RemoteMepEntry {

    MepId remoteMepId();

    /**
     * Get the operational state of a Remote MEP state machine for a remote MEP.
     * @return An enumerated value
     */
    RemoteMepState state();

    /**
     * Get the time since Remote MEP last entered either RMEP_FAILED or RMEP_OK state.
     * @return Duration
     */
    Duration failedOrOkTime();

    /**
     * Get the MAC address of the remote MEP or 0 if no CCM has been received.
     * @return A MAC address
     */
    MacAddress macAddress();

    /**
     * A Boolean value indicating the state of the RDI bit in the last received CCM.
     *
     * @return (TRUE for RDI = 1), or FALSE, if none has been received
     */
    boolean rdi();

    /**
     * Get the Port Status TLV from the last CCM received from the remote MEP.
     * @return An enumerated value
     */
    PortStatusTlvType portStatusTlvType();

    /**
     * Get the Interface Status TLV from the last CCM received from the remote MEP.
     * @return An enumerated value
     */
    InterfaceStatusTlvType interfaceStatusTlvType();

    /**
     * Get the Sender ID TLV type from the last CCM received from the remote MEP or default.
     * @return An enumerated value
     */
    SenderIdTlvType senderIdTlvType();

    /**
     * Builder for {@link RemoteMepEntry}.
     */
    public interface RemoteMepEntryBuilder {
        RemoteMepEntryBuilder failedOrOkTime(Duration failedOrOkTime);

        RemoteMepEntryBuilder macAddress(MacAddress macAddress);

        RemoteMepEntryBuilder rdi(boolean rdi);

        RemoteMepEntryBuilder portStatusTlvType(PortStatusTlvType portStatusTlvType);

        RemoteMepEntryBuilder interfaceStatusTlvType(InterfaceStatusTlvType interfaceStatusTlvType);

        RemoteMepEntryBuilder senderIdTlvType(SenderIdTlvType senderIdTlvType);

        RemoteMepEntry build();
    }

    /**
     * Remote MEP States.
     */
    public enum RemoteMepState {
        RMEP_IDLE,
        RMEP_START,
        RMEP_FAILED,
        RMEP_OK
    }

    /**
     * Port Status TLV types.
     */
    public enum PortStatusTlvType {
        PS_NO_STATUS_TLV,
        PS_BLOCKED,
        PS_UP;
    }

    /**
     * Interface Status TLV types.
     */
    public enum InterfaceStatusTlvType {
        IS_NO_STATUS_TLV,
        IS_UP,
        IS_DOWN,
        IS_TESTING,
        IS_UNKNOWN,
        IS_DORMANT,
        IS_NOTPRESENT,
        IS_LOWERLAYERDOWN;
    }
}
