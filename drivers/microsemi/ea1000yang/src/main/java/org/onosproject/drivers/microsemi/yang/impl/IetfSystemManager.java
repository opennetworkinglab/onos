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
package org.onosproject.drivers.microsemi.yang.impl;

import static org.onosproject.yms.ych.YangProtocolEncodingFormat.XML;
import static org.onosproject.yms.ydt.YmsOperationType.QUERY_REPLY;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Service;
import org.onosproject.drivers.microsemi.yang.IetfSystemNetconfService;
import org.onosproject.netconf.NetconfException;
import org.onosproject.netconf.NetconfSession;
import org.onosproject.netconf.TargetConfig;
import org.onosproject.yang.gen.v1.http.www.microsemi.com.microsemi.edge.assure.msea.system.rev20160505.IetfSystemMicrosemiService;
import org.onosproject.yang.gen.v1.http.www.microsemi.com.microsemi.edge.assure.msea.system.rev20160505.ietfsystemmicrosemi.doupgradeandreboot.DoUpgradeAndRebootInput;
import org.onosproject.yang.gen.v1.http.www.microsemi.com.microsemi.edge.assure.msea.system.rev20160505.ietfsystemmicrosemi.doupgradeandreboot.DoUpgradeAndRebootOutput;
import org.onosproject.yang.gen.v1.http.www.microsemi.com.microsemi.edge.assure.msea.system.rev20160505.ietfsystemmicrosemi.pullupdatetarfromtftp.PullUpdateTarFromTftpInput;
import org.onosproject.yang.gen.v1.http.www.microsemi.com.microsemi.edge.assure.msea.system.rev20160505.ietfsystemmicrosemi.readfromsyslog.ReadFromSyslogInput;
import org.onosproject.yang.gen.v1.http.www.microsemi.com.microsemi.edge.assure.msea.system.rev20160505.ietfsystemmicrosemi.readfromsyslog.ReadFromSyslogOutput;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.system.rev20140806.IetfSystem;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.system.rev20140806.IetfSystemOpParam;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.system.rev20140806.IetfSystemService;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.system.rev20140806.ietfsystem.systemrestart.SystemRestartInput;

/**
 * Implementation of the IetfService YANG model service.
 */
@Component(immediate = true, inherit = true)
@Service
public class IetfSystemManager extends AbstractYangServiceImpl
    implements IetfSystemNetconfService {

    protected final Pattern regexRemoveSystem =
            Pattern.compile("(<system).*(</system>)", Pattern.DOTALL);
    protected final Pattern regexRemoveSystemState =
            Pattern.compile("(<system-state).*(</system-state>)", Pattern.DOTALL);

    protected static final String IETF_SYSTEM = "org.onosproject.drivers.microsemi.yang.ietfsystem";

    @Activate
    public void activate() {
        appId = coreService.registerApplication(IETF_SYSTEM);
        ych = ymsService.getYangCodecHandler();
        ych.addDeviceSchema(IetfSystemService.class);
        ych.addDeviceSchema(IetfSystemMicrosemiService.class);
        log.info("IetfSystemManager Started");
    }

    @Deactivate
    public void deactivate() {
        super.deactivate();
        ymsService.unRegisterService(this, IetfSystemService.class);
        ymsService.unRegisterService(this, IetfSystemMicrosemiService.class);
        ych = null;
        log.info("IetfSystemManager Stopped");
    }

    /**
     * Get a filtered subset of the model.
     * This is meant to filter the current live model
     * against the attribute(s) given in the argument
     * and return the filtered model.
     * @throws NetconfException if the session has any error
     */
    @Override
    public IetfSystem getIetfSystem(IetfSystemOpParam ietfSystemFilter, NetconfSession session)
            throws NetconfException {
        return (IetfSystem) getNetconfObject(ietfSystemFilter, session);
    }

    @Override
    public IetfSystem getIetfSystemInit(NetconfSession session) throws NetconfException {
        if (session == null) {
            throw new NetconfException("Session is null when calling getIetfSystemInit()");
        }

        String xmlResult = session.get(getInitRequestBuilder(), null);

        //The result will be a <system> followed by <system-state>
        //YCH can decode only one at a time - split it and send half each time
        IetfSystem.IetfSystemBuilder iBuilder = new IetfSystemOpParam.IetfSystemBuilder();

        String xmlResultSystem = regexRemoveSystemState.matcher(xmlResult).replaceFirst("");
        List<Object> objectListState = ych.decode(xmlResultSystem, XML, QUERY_REPLY);
        if (objectListState != null && objectListState.size() > 0) {
            IetfSystem system = (IetfSystem) objectListState.get(0);
            iBuilder.system(system.system());
        }

        String xmlResultSystemState = regexRemoveSystem.matcher(xmlResult).replaceFirst("");
        List<Object> objectListSystemState = ych.decode(xmlResultSystemState, XML, QUERY_REPLY);
        if (objectListSystemState != null && objectListSystemState.size() > 0) {
            IetfSystem system = (IetfSystem) objectListSystemState.get(0);
            iBuilder.systemState(system.systemState());
        }

        return iBuilder.build();
    }

    /**
     * Call NETCONF edit-config with a configuration.
     */
    @Override
    public void setIetfSystem(IetfSystemOpParam ietfSystem, NetconfSession session, TargetConfig ncDs)
        throws NetconfException {
        setNetconfObject(ietfSystem, session, ncDs);
    }

    @Override
    public void setCurrentDatetime(OffsetDateTime date, NetconfSession session)
            throws NetconfException {
        String xmlQueryStr = getSetCurrentDatetimeBuilder(date);
        log.info("Sending <get> query on NETCONF session " + session.getSessionId() +
                ":\n" + xmlQueryStr);

        String xmlResult = session.doWrappedRpc(xmlQueryStr);
        log.info("Result from NETCONF RPC <set-current-datetime>: {}", xmlResult);
    }

    @Override
    public void systemRestart(SystemRestartInput inputVar, NetconfSession session) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void systemShutdown(NetconfSession session) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public DoUpgradeAndRebootOutput doUpgradeAndReboot(DoUpgradeAndRebootInput inputVar, NetconfSession session)
            throws NetconfException {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void pullUpdateTarFromTftp(PullUpdateTarFromTftpInput inputVar, NetconfSession session)
            throws NetconfException {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public ReadFromSyslogOutput readFromSyslog(ReadFromSyslogInput inputVar, NetconfSession session)
            throws NetconfException {
        throw new UnsupportedOperationException("Not yet implemented");
    }


    /**
     * Builds a request crafted to get the configuration required to create
     * details descriptions for the device.
     *
     * @return The request string.
     */
    private static String getInitRequestBuilder() {
        StringBuilder rpc = new StringBuilder();
        rpc.append("<system-state xmlns=\"urn:ietf:params:xml:ns:yang:ietf-system\" ");
        rpc.append("xmlns:sysms=\"http://www.microsemi.com/microsemi-edge-assure/msea-system\">");
        rpc.append("<platform>");
        rpc.append("<os-release/>");
        rpc.append("<sysms:device-identification>");
        rpc.append("<sysms:serial-number/>");
        rpc.append("</sysms:device-identification>");
        rpc.append("</platform>");
        rpc.append("<clock>");
        rpc.append("<current-datetime/>");
        rpc.append("</clock>");
        rpc.append("</system-state>");
        rpc.append("<system xmlns=\"urn:ietf:params:xml:ns:yang:ietf-system\" ");
        rpc.append("xmlns:sysms=\"http://www.microsemi.com/microsemi-edge-assure/msea-system\">");
        rpc.append("<sysms:longitude/>");
        rpc.append("<sysms:latitude/>");
        rpc.append("</system>");
        return rpc.toString();
    }

    private static String getSetCurrentDatetimeBuilder(OffsetDateTime date) {
        StringBuilder rpc = new StringBuilder();
        rpc.append("<set-current-datetime xmlns=\"urn:ietf:params:xml:ns:yang:ietf-system\">");
        rpc.append("<current-datetime>");
        rpc.append(date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssxxx")));
        rpc.append("</current-datetime>");
        rpc.append("</set-current-datetime>");

        return rpc.toString();
    }
}
