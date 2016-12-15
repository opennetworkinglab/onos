/*
 * Copyright 2017-present Open Networking Laboratory
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

import org.onosproject.event.ListenerService;
import org.onosproject.netconf.NetconfException;
import org.onosproject.netconf.NetconfSession;
import org.onosproject.netconf.TargetConfig;
import org.onosproject.yang.gen.v1.http.www.microsemi.com.microsemi.edge.assure.msea.cfm.rev20160229.MseaCfm;
import org.onosproject.yang.gen.v1.http.www.microsemi.com.microsemi.edge.assure.msea.cfm.rev20160229.MseaCfmOpParam;
import org.onosproject.yang.gen.v1.http.www.microsemi.com.microsemi.edge.assure.msea.cfm.rev20160229.mseacfm.MseaCfmEvent;
import org.onosproject.yang.gen.v1.http.www.microsemi.com.microsemi.edge.assure.msea.cfm.rev20160229.mseacfm.MseaCfmEventListener;
import org.onosproject.yang.gen.v1.http.www.microsemi.com.microsemi.edge.assure.msea.cfm.rev20160229.mseacfm.abortloopback.AbortLoopbackInput;
import org.onosproject.yang.gen.v1.http.www.microsemi.com.microsemi.edge.assure.msea.cfm.rev20160229.mseacfm.transmitlinktrace.TransmitLinktraceInput;
import org.onosproject.yang.gen.v1.http.www.microsemi.com.microsemi.edge.assure.msea.cfm.rev20160229.mseacfm.transmitlinktrace.TransmitLinktraceOutput;
import org.onosproject.yang.gen.v1.http.www.microsemi.com.microsemi.edge.assure.msea.cfm.rev20160229.mseacfm.transmitloopback.TransmitLoopbackInput;

/**
 * Extension of mseaCfmService to include NETCONF sessions.
 *
 * This is manually extended and should be revised if the msea-cfm.yang,
 * msea-soam-pm.yang or msea-soam-fm.yang files change
 */
public interface MseaCfmNetconfService extends ListenerService<MseaCfmEvent, MseaCfmEventListener> {

    /**
     * Returns attributes of MEP.
     *
     * @param mdName The name of the MD
     * @param maName The name of the MA
     * @param mepId The ID of the MEP
     * @param session An active NETCONF session
     * @return mseaCfm
     * @throws NetconfException if the session has any error
     */
    MseaCfm getMepEssentials(String mdName, String maName, int mepId,
            NetconfSession session) throws NetconfException;


    /**
     * Returns attributes of DM.
     *
     * @param mdName The name of the MD
     * @param maName The name of the MA
     * @param mepId The ID of the MEP
     * @param dmId The Id of the Delay Measurement
     * @param session An active NETCONF session
     * @return mseaCfm
     * @throws NetconfException if the session has any error
     */
    MseaCfm getSoamDm(String mdName, String maName, int mepId,
            int dmId, NetconfSession session) throws NetconfException;

    /**
     * Sets the value to attribute mseaCfm.
     *
     * @param mseaCfm value of mseaCfm
     * @param session An active NETCONF session
     * @param targetDs one of running, candidate or startup
     * @throws NetconfException if the session has any error
     */
    void setMseaCfm(MseaCfmOpParam mseaCfm, NetconfSession session, TargetConfig targetDs) throws NetconfException;

    /**
     * Service interface of transmitLoopback.
     *
     * @param inputVar input of service interface transmitLoopback
     * @param session An active NETCONF session
     * @throws NetconfException if the session has any error
     */
    void transmitLoopback(TransmitLoopbackInput inputVar, NetconfSession session) throws NetconfException;

    /**
     * Service interface of abortLoopback.
     *
     * @param inputVar input of service interface abortLoopback
     * @param session An active NETCONF session
     * @throws NetconfException if the session has any error
     */
    void abortLoopback(AbortLoopbackInput inputVar, NetconfSession session) throws NetconfException;

    /**
     * Service interface of transmitLinktrace.
     *
     * @param inputVar input of service interface transmitLinktrace
     * @param session An active NETCONF session
     * @return transmitLinktraceOutput output of service interface transmitLinktrace
     * @throws NetconfException if the session has any error
     */
    TransmitLinktraceOutput transmitLinktrace(TransmitLinktraceInput inputVar, NetconfSession session)
            throws NetconfException;
}
