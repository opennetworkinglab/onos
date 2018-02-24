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

import java.util.Collection;

import org.onlab.packet.MacAddress;
import org.onosproject.incubator.net.l2monitoring.cfm.service.CfmConfigException;
import org.onosproject.incubator.net.l2monitoring.soam.delay.DelayMeasurementEntry;
import org.onosproject.incubator.net.l2monitoring.soam.loss.LossMeasurementEntry;

/**
 * An extension of the Mep interface to represent the state attributes.
 *
 */
public interface MepEntry extends Mep {
    /**
     * Get the MAC address of the MEP.
     *
     * @return The MAC address of the MEP
     */
    MacAddress macAddress();

    /**
     * Get the state of the MEPs Fault Notification Generator.
     *
     * @return The state of the MEPs Fault Notification Generator
     */
    FngState fngState();

    /**
     * Get the highest-priority defect that has been present since last FNG reset.
     * @return The highest-priority defect
     */
    FaultDefectType getHighestPriorityDefect();

    /**
     * Get flag indicating that some other MEP in this MEP’s MA is transmitting the RDI bit.
     * @return true or false
     */
    boolean activeRdiCcmDefect();

    /**
     * Get flag indicating that a Port Status or Interface Status TLV is indicating an error condition.
     * @return true or false
     */
    boolean activeMacStatusDefect();

    /**
     * Get flag indicating that CCMs are not being received from at least one of the configured remote MEPs.
     * @return true or false
     */
    boolean activeRemoteCcmDefect();

    /**
     * Get flag indicating that erroneous CCMs are being received from some MEP in this MEP’s MA.
     * @return true or false
     */
    boolean activeErrorCcmDefect();

    /**
     * Get flag indicating indicating that CCMs are being received from a MEP that could be in some other MA.
     * @return true or false
     */
    boolean activeXconCcmDefect();

    /**
     * The last-received CCM that triggered a DEF_ERROR_CCM fault.
     *
     * @return An array of bytes (length 1-1522) containing the CCM
     */
    byte[] lastErrorCcm();

    /**
     * The last-received CCM that triggered a DEF_XCON_CCM fault.
     *
     * @return An array of bytes (length 1-1522) containing the CCM
     */
    byte[] lastXconCcm();

    /**
     * Get the total number of out-of-sequence CCMs received.
     * @return The total number of out-of-sequence CCMs received
     */
    int ccmSequenceErrorCount();

    /**
     * Get the total number of CCMs transmitted.
     * @return The total number of CCMs transmitted
     */
    int totalCcmsTransmitted();

    /**
     * Get the collection of attributes related to loopback.
     * @return An object with loopback attributes
     */
    MepLbEntry loopbackAttributes();

    /**
     * Get the collection of attributes related to linktrace.
     * @return An object with linktrace attributes
     */
    MepLtEntry linktraceAttributes();

    /**
     * Get the list of active Remote MEPs.
     * @return A list of remote MEPs including their states
     */
    Collection<RemoteMepEntry> activeRemoteMepList();

    /**
     * Get the list of Delay Measurements for this MEP.
     * @return A list of the Delay Measurements including their states
     */
    Collection<DelayMeasurementEntry> delayMeasurementList();

    /**
     * Get the list of Loss Measurements for this MEP.
     * @return A list of the Loss Measurements including their states
     */
    Collection<LossMeasurementEntry> lossMeasurementList();

    /**
     * States of Fault Notification Generator.
     */
    public enum FngState {
        FNG_RESET,
        FNG_DEFECT,
        FNG_REPORT_DEFECT,
        FNG_DEFECT_REPORTED,
        FNG_DEFECT_CLEARING
    }

    /**
     * Builder for {@link MepEntry}.
     */
    interface MepEntryBuilder extends MepBuilder {
        MepEntryBuilder macAddress(MacAddress macAddress);

        MepEntryBuilder fngState(FngState fngState);

        MepEntryBuilder highestPriorityDefect(FaultDefectType highestPriorityDefect);

        MepEntryBuilder activeRdiCcmDefect(boolean activeRdiCcmDefect);

        MepEntryBuilder activeMacStatusDefect(boolean activeMacStatusDefect);

        MepEntryBuilder activeRemoteCcmDefect(boolean activeRemoteCcmDefect);

        MepEntryBuilder activeErrorCcmDefect(boolean activeErrorCcmDefect);

        MepEntryBuilder activeXconCcmDefect(boolean activeXconCcmDefect);

        MepEntryBuilder lastErrorCcm(byte[] lastErrorCcm);

        MepEntryBuilder lastXconCcm(byte[] lastXconCcm);

        MepEntryBuilder ccmSequenceErrorCount(int ccmSequenceErrorCount);

        MepEntryBuilder totalCcmsTransmitted(int totalCcmsTransmitted);

        MepEntryBuilder loopbackAttributes(MepLbEntry loopbackAttributes);

        MepEntryBuilder linktraceAttributes(MepLtEntry linktraceAttributes);

        MepEntryBuilder addToActiveRemoteMepList(RemoteMepEntry activeRemoteMep);

        MepEntryBuilder addToDelayMeasurementList(DelayMeasurementEntry delayMeasurement);

        MepEntryBuilder addToLossMeasurementList(LossMeasurementEntry lossMeasurement);

        MepEntry buildEntry() throws CfmConfigException;
    }
}
