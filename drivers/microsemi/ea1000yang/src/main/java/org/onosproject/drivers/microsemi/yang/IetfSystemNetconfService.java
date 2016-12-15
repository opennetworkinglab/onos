/*
 * Copyright 2016-present Open Networking Laboratory
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

import java.time.OffsetDateTime;

import org.onosproject.netconf.NetconfException;
import org.onosproject.netconf.NetconfSession;
import org.onosproject.netconf.TargetConfig;
import org.onosproject.yang.gen.v1.http.www.microsemi.com.microsemi.edge.assure.msea.system.rev20160505.ietfsystemmicrosemi.doupgradeandreboot.DoUpgradeAndRebootInput;
import org.onosproject.yang.gen.v1.http.www.microsemi.com.microsemi.edge.assure.msea.system.rev20160505.ietfsystemmicrosemi.doupgradeandreboot.DoUpgradeAndRebootOutput;
import org.onosproject.yang.gen.v1.http.www.microsemi.com.microsemi.edge.assure.msea.system.rev20160505.ietfsystemmicrosemi.pullupdatetarfromtftp.PullUpdateTarFromTftpInput;
import org.onosproject.yang.gen.v1.http.www.microsemi.com.microsemi.edge.assure.msea.system.rev20160505.ietfsystemmicrosemi.readfromsyslog.ReadFromSyslogInput;
import org.onosproject.yang.gen.v1.http.www.microsemi.com.microsemi.edge.assure.msea.system.rev20160505.ietfsystemmicrosemi.readfromsyslog.ReadFromSyslogOutput;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.system.rev20140806.IetfSystem;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.system.rev20140806.IetfSystemOpParam;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.system.rev20140806.ietfsystem.systemrestart.SystemRestartInput;

/**
 * Extension of ietfSystemService to include NETCONF sessions.
 *
 * This is manually extended and should be revised if the ietf-system.yang file changes
 */
public interface IetfSystemNetconfService {
    /**
     * Returns the attribute ietfSystem.
     *
     * @param ietfSystem value of ietfSystem
     * @param session An active NETCONF session
     * @return ietfSystem
     * @throws NetconfException if the session has any error
     */
    IetfSystem getIetfSystem(IetfSystemOpParam ietfSystem, NetconfSession session) throws NetconfException;

    /**
     * Returns the result of the init query.
     *
     * @param session An active NETCONF session
     * @return ietfSystem
     * @throws NetconfException if the session has any error
     */
    IetfSystem getIetfSystemInit(NetconfSession session) throws NetconfException;

    /**
     * Sets the value to attribute ietfSystem.
     *
     * @param ietfSystem value of ietfSystem
     * @param session An active NETCONF session
     * @param ncDs datastore type running, startup or candidate
     * @throws NetconfException if the session has any error
     */
    void setIetfSystem(IetfSystemOpParam ietfSystem, NetconfSession session, TargetConfig ncDs)
            throws NetconfException;

    /**
     * Service interface of setCurrentDatetime.
     *
     * @param date input of service interface setCurrentDatetime
     * @param session An active NETCONF session
     * @throws NetconfException if the session has any error
     */
    void setCurrentDatetime(OffsetDateTime date, NetconfSession session) throws NetconfException;

    /**
     * Service interface of systemRestart.
     *
     * @param inputVar input of service interface systemRestart
     * @param session An active NETCONF session
     * @throws NetconfException if the session has any error
     */
    void systemRestart(SystemRestartInput inputVar, NetconfSession session) throws NetconfException;

    /**
     * Service interface of systemShutdown.
     *
     * @param session An active NETCONF session
     * @throws NetconfException if the session has any error
     */
    void systemShutdown(NetconfSession session) throws NetconfException;

    /**
     * Service interface of doUpgradeAndReboot.
     *
     * @param inputVar input of service interface doUpgradeAndReboot
     * @param session An active NETCONF session
     * @return doUpgradeAndRebootOutput output of service interface doUpgradeAndReboot
     * @throws NetconfException if the session has any error
     */
    DoUpgradeAndRebootOutput doUpgradeAndReboot(DoUpgradeAndRebootInput inputVar, NetconfSession session)
            throws NetconfException;

    /**
     * Service interface of pullUpdateTarFromTftp.
     *
     * @param inputVar input of service interface pullUpdateTarFromTftp
     * @param session An active NETCONF session
     * @throws NetconfException if the session has any error
     */
    void pullUpdateTarFromTftp(PullUpdateTarFromTftpInput inputVar, NetconfSession session)
            throws NetconfException;

    /**
     * Service interface of readFromSyslog.
     *
     * @param inputVar input of service interface readFromSyslog
     * @param session An active NETCONF session
     * @return readFromSyslogOutput output of service interface readFromSyslog
     * @throws NetconfException if the session has any error
     */
    ReadFromSyslogOutput readFromSyslog(ReadFromSyslogInput inputVar, NetconfSession session)
            throws NetconfException;

}
