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
package org.onosproject.incubator.net.l2monitoring.soam;

import java.util.Collection;
import java.util.Optional;

import org.onosproject.incubator.net.l2monitoring.cfm.MepTsCreate;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MaIdShort;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MdId;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MepId;
import org.onosproject.incubator.net.l2monitoring.cfm.service.CfmConfigException;
import org.onosproject.incubator.net.l2monitoring.soam.delay.DelayMeasurementCreate;
import org.onosproject.incubator.net.l2monitoring.soam.delay.DelayMeasurementEntry;
import org.onosproject.incubator.net.l2monitoring.soam.delay.DelayMeasurementStatCurrent;
import org.onosproject.incubator.net.l2monitoring.soam.delay.DelayMeasurementStatHistory;
import org.onosproject.incubator.net.l2monitoring.soam.loss.LossMeasurementCreate;
import org.onosproject.incubator.net.l2monitoring.soam.loss.LossMeasurementEntry;
import org.onosproject.incubator.net.l2monitoring.soam.loss.LossMeasurementStatCurrent;

/**
 * Methods callable on MEPs to implement SOAM functionality.
 * Most of the methods have been derived from the MEF 38 and MEF 39 standards
 *
 */
public interface SoamService {
    /**
     * Get all of the Delay Measurements on a particular MEP.
     *
     * @param mdName The Maintenance Domain of the MEP
     * @param maName The Maintenance Association of the MEP
     * @param mepId The id of the MEP itself
     * @return A collection of Delay Measurements and their children
     * @throws CfmConfigException If there's a problem with Cfm attributes
     * @throws SoamConfigException If there's a problem with Soam attributes
     */
    Collection<DelayMeasurementEntry> getAllDms(MdId mdName, MaIdShort maName, MepId mepId)
            throws CfmConfigException, SoamConfigException;

    /**
     * Get a named Delay Measurements on a particular MEP.
     * While devices are not required to have named delay measurement objects
     * so do. The getAllDms() method may have produced a list of DM Ids that can
     * be used here to retrieve one DM at a time
     *
     * @param mdName The Maintenance Domain of the MEP
     * @param maName The Maintenance Association of the MEP
     * @param mepId The id of the MEP itself
     * @param dmId The id of the Delay Measurement
     * @return A collection of Delay Measurements and their children
     * @throws CfmConfigException If there's a problem with Cfm attributes
     * @throws SoamConfigException If there's a problem with Soam attributes
     */
    DelayMeasurementEntry getDm(MdId mdName, MaIdShort maName, MepId mepId, SoamId dmId)
            throws CfmConfigException, SoamConfigException;

    /**
     * Get only the current stats of a named Delay Measurements on a particular MEP.
     * It may be useful to retrieve the current stats on their own to retrieve
     * values between Delay Measurement intervals
     *
     * @param mdName The Maintenance Domain of the MEP
     * @param maName The Maintenance Association of the MEP
     * @param mepId The id of the MEP itself
     * @param dmId The id of the Delay Measurement
     * @throws CfmConfigException If there's a problem with Cfm attributes
     * @throws SoamConfigException If there's a problem with Soam attributes
     * @return A collection of Delay Measurements and their children
     */
    DelayMeasurementStatCurrent getDmCurrentStat(
            MdId mdName, MaIdShort maName, MepId mepId, SoamId dmId)
                    throws CfmConfigException, SoamConfigException;

    /**
     * Get only the history stats of a named Delay Measurements on a particular MEP.
     * It may be useful to retrieve the history stats on their own to retrieve
     * values before they have been overwritten
     *
     * @param mdName The Maintenance Domain of the MEP
     * @param maName The Maintenance Association of the MEP
     * @param mepId The id of the MEP itself
     * @param dmId The id of the Delay Measurement
     * @throws CfmConfigException If there's a problem with Cfm attributes
     * @throws SoamConfigException If there's a problem with Soam attributes
     * @return A collection of Delay Measurements and their children
     */
    Collection<DelayMeasurementStatHistory> getDmHistoricalStats(
            MdId mdName, MaIdShort maName, MepId mepId, SoamId dmId)
                throws CfmConfigException, SoamConfigException;

    /**
     * Create a Delay Measurement on a particular MEP.
     * MEF 39 defines a delay measurement as an ephemeral object and does not
     * require the supporting device to persist it. It runs as an action until
     * stopped with the corresponding abort action below.
     *
     * @param mdName The Maintenance Domain of the MEP
     * @param maName The Maintenance Association of the MEP
     * @param mepId The id of the MEP itself
     * @param dm The parameters of the Delay Measurement
     * @return The id of the newly created DM if available
     * @throws CfmConfigException If there's a problem with Cfm attributes
     * @throws SoamConfigException If there's a problem with Soam attributes
     */
    Optional<SoamId> createDm(MdId mdName, MaIdShort maName, MepId mepId,
        DelayMeasurementCreate dm) throws CfmConfigException, SoamConfigException;

    /**
     * Stop all Delay Measurements on a particular MEP.
     * This stops the Delay Measurement activity started through the
     * createDm action above. It is up to the individual device how to implement
     * it. It does not necessarily mean delete the DM.
     *
     * @param mdName The Maintenance Domain of the MEP
     * @param maName The Maintenance Association of the MEP
     * @param mepId The id of the MEP itself
     * @throws CfmConfigException When the command cannot be completed
     */
    void abortDm(MdId mdName, MaIdShort maName, MepId mepId)
            throws CfmConfigException;

    /**
     * Stop a particular named Delay Measurement on a particular MEP.
     * This stops the Delay Measurement activity started through the
     * createDm action above. It is up to the individual device how to implement
     * it. It does not necessarily mean delete the DM.
     *
     * @param mdName The Maintenance Domain of the MEP
     * @param maName The Maintenance Association of the MEP
     * @param mepId The id of the MEP itself
     * @param dmId The id of the DM
     * @throws CfmConfigException When the command cannot be completed
     */
    void abortDm(MdId mdName, MaIdShort maName, MepId mepId, SoamId dmId)
            throws CfmConfigException;

    /**
     * Clear the history stats on all Delay Measurements on a particular MEP.
     * This removes any historical stats stored on a device for one MEP
     * It does NOT require that the Delay Measurement test is aborted.
     *
     * @param mdName The Maintenance Domain of the MEP
     * @param maName The Maintenance Association of the MEP
     * @param mepId The id of the MEP itself
     * @throws CfmConfigException When the command cannot be completed
     */
    void clearDelayHistoryStats(MdId mdName, MaIdShort maName, MepId mepId)
            throws CfmConfigException;

    /**
     * Clear the history stats on a named Delay Measurement on a particular MEP.
     * This removes any historical stats stored on a device for one DM on one MEP
     * It does NOT require that the Delay Measurement test is aborted.
     *
     * @param mdName The Maintenance Domain of the MEP
     * @param maName The Maintenance Association of the MEP
     * @param mepId The id of the MEP itself
     * @param dmId The id of the DM
     * @throws CfmConfigException When the command cannot be completed
     */
    void clearDelayHistoryStats(MdId mdName, MaIdShort maName, MepId mepId,
            SoamId dmId) throws CfmConfigException;

    /**
     * Get all of the Loss Measurements on a particular MEP.
     *
     * @param mdName The Maintenance Domain of the MEP
     * @param maName The Maintenance Association of the MEP
     * @param mepId The id of the MEP itself
     * @return A collection of Loss Measurements and their children
     * @throws CfmConfigException When the command cannot be completed
     * @throws SoamConfigException When the command cannot be completed
     */
    Collection<LossMeasurementEntry> getAllLms(MdId mdName, MaIdShort maName, MepId mepId)
            throws CfmConfigException, SoamConfigException;

    /**
     * Get a named Loss Measurements on a particular MEP.
     * While devices are not required to have named Loss measurement objects
     * some do. The getAllLms() method may have produced a list of LM Ids that
     * can be used here to retrieve one LM at a time
     *
     * @param mdName The Maintenance Domain of the MEP
     * @param maName The Maintenance Association of the MEP
     * @param mepId The id of the MEP itself
     * @param lmId The id of the Loss Measurement
     * @return A collection of Loss Measurements and their children
     * @throws CfmConfigException When the command cannot be completed
     * @throws SoamConfigException When the command cannot be completed
     */
    LossMeasurementEntry getLm(MdId mdName, MaIdShort maName, MepId mepId,
            SoamId lmId) throws CfmConfigException, SoamConfigException;

    /**
     * Get only the current stats of a named Loss Measurements on a particular MEP.
     * It may be useful to retrieve the current stats on their own to retrieve
     * values between Loss Measurement intervals
     *
     * @param mdName The Maintenance Domain of the MEP
     * @param maName The Maintenance Association of the MEP
     * @param mepId The id of the MEP itself
     * @param lmId The id of the Loss Measurement
     * @return A collection of Loss Measurements and their children
     */
    LossMeasurementStatCurrent getLmCurrentStat(
            MdId mdName, MaIdShort maName, MepId mepId, SoamId lmId);

    /**
     * Get only the history stats of a named Loss Measurements on a particular MEP.
     * It may be useful to retrieve the history stats on their own to retrieve
     * values before they have been overwritten
     *
     * @param mdName The Maintenance Domain of the MEP
     * @param maName The Maintenance Association of the MEP
     * @param mepId The id of the MEP itself
     * @param lmId The id of the Loss Measurement
     * @return A collection of Loss Measurements and their children
     */
    Collection<LossMeasurementStatCurrent> getLmHistoricalStats(
            MdId mdName, MaIdShort maName, MepId mepId, SoamId lmId);

    /**
     * Create a Loss Measurement on a particular MEP.
     * MEF 39 defines a Loss measurement as an ephemeral object and does not
     * require the supporting device to persist it. It runs as an action until
     * stopped with the corresponding abort action below.
     *
     * @param mdName The Maintenance Domain of the MEP
     * @param maName The Maintenance Association of the MEP
     * @param mepId The id of the MEP itself
     * @param lm The parameters of the Loss Measurement
     * @return The id of the newly created LM if available
     * @throws CfmConfigException When the command cannot be completed
     * @throws SoamConfigException When the command cannot be completed
     */
    Optional<SoamId> createLm(MdId mdName, MaIdShort maName, MepId mepId,
            LossMeasurementCreate lm) throws CfmConfigException, SoamConfigException;

    /**
     * Stop all Loss Measurements on a particular MEP.
     * This stops the Loss Measurement activity started through the
     * createLm action above. It is up to the individual device how to implement
     * it. It does not necessarily mean delete the LM.
     *
     * @param mdName The Maintenance Domain of the MEP
     * @param maName The Maintenance Association of the MEP
     * @param mepId The id of the MEP itself
     * @throws CfmConfigException When the command cannot be completed
     */
    void abortLm(MdId mdName, MaIdShort maName, MepId mepId)
            throws CfmConfigException;

    /**
     * Stop a particular named Loss Measurement on a particular MEP.
     * This stops the Loss Measurement activity started through the
     * createLm action above. It is up to the individual device how to implement
     * it. It does not necessarily mean delete the LM.
     *
     * @param mdName The Maintenance Domain of the MEP
     * @param maName The Maintenance Association of the MEP
     * @param mepId The id of the MEP itself
     * @param lmId The id of the LM
     * @throws CfmConfigException When the command cannot be completed
     */
    void abortLm(MdId mdName, MaIdShort maName, MepId mepId, SoamId lmId)
            throws CfmConfigException;

    /**
     * Clear the history stats on all Loss Measurements on a particular MEP.
     * This removes any historical stats stored on a device for one MEP
     * It does NOT require that the Loss Measurement test is aborted.
     *
     * @param mdName The Maintenance Domain of the MEP
     * @param maName The Maintenance Association of the MEP
     * @param mepId The id of the MEP itself
     * @throws CfmConfigException When the command cannot be completed
     */
    void clearLossHistoryStats(MdId mdName, MaIdShort maName, MepId mepId)
            throws CfmConfigException;

    /**
     * Clear the history stats on a named Loss Measurement on a particular MEP.
     * This removes any historical stats stored on a device for one LM on one MEP
     * It does NOT require that the Loss Measurement test is aborted.
     *
     * @param mdName The Maintenance Domain of the MEP
     * @param maName The Maintenance Association of the MEP
     * @param mepId The id of the MEP itself
     * @param lmId The id of the LM
     * @throws CfmConfigException When the command cannot be completed
     */
    void clearLossHistoryStats(MdId mdName, MaIdShort maName, MepId mepId,
            SoamId lmId) throws CfmConfigException;

    /**
     * Create a Test Signal operation on a particular MEP.
     * MEF39 defines the Test Signal as an ephemeral operation that is not
     * required to be persisted by a device. Only one Test Signal is active
     * on a MEP at a time
     *
     * @param mdName The Maintenance Domain of the MEP
     * @param maName The Maintenance Association of the MEP
     * @param mepId The id of the MEP itself
     * @param tsCreate The parameters the Test Signal is created with
     * @throws CfmConfigException When the command cannot be completed
     */
    void createTestSignal(MdId mdName, MaIdShort maName, MepId mepId,
            MepTsCreate tsCreate) throws CfmConfigException;

    /**
     * Abort a Test Signal operation on a particular MEP.
     * Abort a Test Signal operation on a Mep
     *
     * @param mdName The Maintenance Domain of the MEP
     * @param maName The Maintenance Association of the MEP
     * @param mepId The id of the MEP itself
     * @throws CfmConfigException When the command cannot be completed
     */
    void abortTestSignal(MdId mdName, MaIdShort maName, MepId mepId)
            throws CfmConfigException;
}
