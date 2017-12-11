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
package org.onosproject.drivers.microsemi.yang;

import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MaIdShort;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MdId;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MepId;
import org.onosproject.incubator.net.l2monitoring.cfm.service.CfmConfigException;
import org.onosproject.incubator.net.l2monitoring.soam.SoamId;
import org.onosproject.netconf.DatastoreId;
import org.onosproject.netconf.NetconfException;
import org.onosproject.netconf.NetconfSession;
import org.onosproject.yang.gen.v1.mseacfm.rev20160229.MseaCfm;
import org.onosproject.yang.gen.v1.mseacfm.rev20160229.MseaCfmOpParam;
import org.onosproject.yang.gen.v1.mseacfm.rev20160229.mseacfm.abortloopback.AbortLoopbackInput;
import org.onosproject.yang.gen.v1.mseacfm.rev20160229.mseacfm.transmitlinktrace.TransmitLinktraceInput;
import org.onosproject.yang.gen.v1.mseacfm.rev20160229.mseacfm.transmitlinktrace.TransmitLinktraceOutput;
import org.onosproject.yang.gen.v1.mseacfm.rev20160229.mseacfm.transmitloopback.TransmitLoopbackInput;

import java.util.Optional;

/**
 * Extension of mseaCfmService to include NETCONF sessions.
 *
 * This is manually extended and should be revised if the msea-cfm.yang,
 * msea-soam-pm.yang or msea-soam-fm.yang files change
 */
public interface MseaCfmNetconfService {

    /**
     * Returns minimal set of attributes of MEP.
     *
     * @param mdId The name of the MD
     * @param maId The name of the MA
     * @param mepId The ID of the MEP
     * @param session An active NETCONF session
     * @return mseaCfm
     * @throws NetconfException if the session has any error
     */
    MseaCfm getMepEssentials(MdId mdId, MaIdShort maId, MepId mepId,
                             NetconfSession session) throws NetconfException;

    /**
     * Returns full set of attributes of MEP.
     * This returns config and state attributes of all children of the MEP
     * except for Delay Measurements and Loss Measurements - these have to be
     * retrieved separately, because of their potential size
     * @param mdId The name of the MD
     * @param maId The name of the MA
     * @param mepId The ID of the MEP
     * @param session An active NETCONF session
     * @return mseaCfm
     * @throws NetconfException if the session has any error
     */
    MseaCfm getMepFull(MdId mdId, MaIdShort maId, MepId mepId,
            NetconfSession session) throws NetconfException;


    /**
     * Returns set of all MepIds from one Md or Ma or all.
     *
     * @param mdIdOptional An MdId to filter by, or empty to select all
     * @param maIdOptional An MaId to filter by, or empty to select all
     * @param session An active NETCONF session
     * @param targetDs one of running, candidate or startup
     * @return mseaCfm
     * @throws NetconfException if the session has any error
     */
    MseaCfm getMepIds(Optional<MdId> mdIdOptional, Optional<MaIdShort> maIdOptional,
                      NetconfSession session, DatastoreId targetDs) throws NetconfException;
    /**
     * Returns attributes of DM.
     *
     * @param mdId The name of the MD
     * @param maId The name of the MA
     * @param mepId The ID of the MEP
     * @param dmId The Id of the Delay Measurement - if null then all DMs
     * @param parts The parts of the DM to return
     * @param session An active NETCONF session
     * @return mseaCfm
     * @throws NetconfException if the session has any error
     */
    MseaCfm getSoamDm(MdId mdId, MaIdShort maId, MepId mepId,
                  SoamId dmId, DmEntryParts parts, NetconfSession session)
                    throws NetconfException;

    /**
     * Sets the value to attribute mseaCfm.
     *
     * @param mseaCfm value of mseaCfm
     * @param session An active NETCONF session
     * @param targetDs one of running, candidate or startup
     * @return Boolean to indicate success or failure
     * @throws NetconfException if the session has any error
     */
    boolean setMseaCfm(MseaCfmOpParam mseaCfm, NetconfSession session,
                       DatastoreId targetDs) throws NetconfException;

    /**
     * Deletes named Meps of mseaCfm.
     * Expects to see a list of Meps
     *
     * @param mseaCfm value of mseaCfm
     * @param session An active NETCONF session
     * @param targetDs one of running, candidate or startup
     * @return Boolean to indicate success or failure
     * @throws NetconfException if the session has any error
     * @throws CfmConfigException if the Cfm config has any error
     */
    boolean deleteMseaMep(MseaCfmOpParam mseaCfm, NetconfSession session,
                            DatastoreId targetDs) throws NetconfException, CfmConfigException;

    /**
     * Deletes named Ma of mseaCfm.
     * Expects to see a list of Mas
     *
     * @param mseaCfm value of mseaCfm
     * @param session An active NETCONF session
     * @param targetDs one of running, candidate or startup
     * @return Boolean to indicate success or failure
     * @throws NetconfException if the session has any error
     * @throws CfmConfigException if the Cfm config has any error
     */
    boolean deleteMseaMa(MseaCfmOpParam mseaCfm, NetconfSession session,
                            DatastoreId targetDs) throws NetconfException, CfmConfigException;

    /**
     * Deletes a remote Mep from an MA.
     * Expects one or more RMeps
     *
     * @param mseaCfm value of mseaCfm
     * @param session An active NETCONF session
     * @param targetDs one of running, candidate or startup
     * @return Boolean to indicate success or failure
     * @throws NetconfException if the session has any error
     * @throws CfmConfigException if the Cfm config has any error
     */
    boolean deleteMseaMaRMep(MseaCfmOpParam mseaCfm, NetconfSession session,
                         DatastoreId targetDs) throws NetconfException, CfmConfigException;

    /**
     * Deletes named Md of mseaCfm.
     * Expects to see a list of Mds
     *
     * @param mseaCfm value of mseaCfm
     * @param session An active NETCONF session
     * @param targetDs one of running, candidate or startup
     * @return Boolean to indicate success or failure
     * @throws NetconfException if the session has any error
     * @throws CfmConfigException if the Cfm config has any error
     */
    boolean deleteMseaMd(MseaCfmOpParam mseaCfm, NetconfSession session,
                            DatastoreId targetDs) throws NetconfException, CfmConfigException;

    /**
     * Deletes named delay measurements of mseaCfm.
     * Expects to see a list of Delay Measurements
     *
     * @param mseaCfm value of mseaCfm
     * @param session An active NETCONF session
     * @param targetDs one of running, candidate or startup
     * @return Boolean to indicate success or failure
     * @throws NetconfException if the session has any error
     * @throws CfmConfigException if the Cfm config has any error
     */
    boolean deleteMseaCfmDm(MseaCfmOpParam mseaCfm, NetconfSession session,
                       DatastoreId targetDs) throws NetconfException, CfmConfigException;

    /**
     * Service interface of transmitLoopback.
     *
     * @param inputVar input of service interface transmitLoopback
     * @param session An active NETCONF session
     * @throws NetconfException if the session has any error
     */
    void transmitLoopback(TransmitLoopbackInput inputVar,
                          NetconfSession session) throws NetconfException;

    /**
     * Service interface of abortLoopback.
     *
     * @param inputVar input of service interface abortLoopback
     * @param session An active NETCONF session
     * @throws NetconfException if the session has any error
     */
    void abortLoopback(AbortLoopbackInput inputVar,
                       NetconfSession session) throws NetconfException;

    /**
     * Service interface of transmitLinktrace.
     *
     * @param inputVar input of service interface transmitLinktrace
     * @param session An active NETCONF session
     * @return transmitLinktraceOutput output of service interface transmitLinktrace
     * @throws NetconfException if the session has any error
     */
    TransmitLinktraceOutput transmitLinktrace(TransmitLinktraceInput inputVar,
              NetconfSession session) throws NetconfException;

    public enum DmEntryParts {
        ALL_PARTS,
        CURRENT_ONLY,
        HISTORY_ONLY;
    }
}
