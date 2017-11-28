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
package org.onosproject.drivers.microsemi.yang.impl;

import java.io.ByteArrayInputStream;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Service;
import org.onosproject.drivers.microsemi.yang.IetfSystemNetconfService;
import org.onosproject.netconf.DatastoreId;
import org.onosproject.netconf.NetconfException;
import org.onosproject.netconf.NetconfSession;
import org.onosproject.yang.gen.v1.ietfsystem.rev20140806.ietfsystem.DefaultSystem;
import org.onosproject.yang.gen.v1.ietfsystem.rev20140806.ietfsystem.DefaultSystemState;
import org.onosproject.yang.gen.v1.ietfsystem.rev20140806.IetfSystem;
import org.onosproject.yang.gen.v1.ietfsystem.rev20140806.IetfSystemOpParam;
import org.onosproject.yang.model.DefaultModelObjectData;
import org.onosproject.yang.model.ModelConverter;
import org.onosproject.yang.model.ModelObject;
import org.onosproject.yang.model.ModelObjectData;
import org.onosproject.yang.runtime.CompositeData;
import org.onosproject.yang.runtime.DefaultCompositeStream;

/**
 * Implementation of the IetfService YANG model service.
 */
@Component(immediate = true, inherit = true)
@Service
public class IetfSystemManager extends AbstractYangServiceImpl
    implements IetfSystemNetconfService {

    protected static final String IETF_SYSTEM = "org.onosproject.drivers.microsemi.yang.ietfsystem";

    @Activate
    public void activate() {
        super.activate();
        appId = coreService.registerApplication(IETF_SYSTEM);
        log.info("IetfSystemManager Started");
    }

    @Deactivate
    public void deactivate() {
        super.deactivate();
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

        ModelObjectData moQuery = DefaultModelObjectData.builder()
                .addModelObject((ModelObject) ietfSystemFilter.system())
                .build();

        ModelObjectData moReply = getNetconfObject(moQuery, session);

        IetfSystemOpParam ietfSystem = new IetfSystemOpParam();
        for (ModelObject mo:moReply.modelObjects()) {
            if (mo instanceof DefaultSystem) {
                ietfSystem.system((DefaultSystem) mo);
            } else if (mo instanceof DefaultSystemState) {
                ietfSystem.systemState((DefaultSystemState) mo);
            }
        }

        return ietfSystem;
    }

    @Override
    public IetfSystem getIetfSystemInit(NetconfSession session) throws NetconfException {
        if (session == null) {
            throw new NetconfException("Session is null when calling getIetfSystemInit()");
        }

        String xmlResult = session.get(getInitRequestBuilder(), null);

        xmlResult = removeRpcReplyData(xmlResult);
        DefaultCompositeStream resultDcs = new DefaultCompositeStream(
                null, new ByteArrayInputStream(xmlResult.getBytes()));
        CompositeData compositeData = xSer.decode(resultDcs, yCtx);

        ModelObjectData mod = ((ModelConverter) yangModelRegistry).createModel(compositeData.resourceData());

        IetfSystemOpParam ietfSystem = new IetfSystemOpParam();
        for (ModelObject mo:mod.modelObjects()) {
            if (mo instanceof DefaultSystem) {
                ietfSystem.system((DefaultSystem) mo);
            } else if (mo instanceof DefaultSystemState) {
                ietfSystem.systemState((DefaultSystemState) mo);
            }
        }

        return ietfSystem;
    }

    /**
     * Call NETCONF edit-config with a configuration.
     */
    @Override
    public boolean setIetfSystem(IetfSystemOpParam ietfSystem, NetconfSession session,
                              DatastoreId ncDs) throws NetconfException {
        ModelObjectData mo = DefaultModelObjectData.builder()
                                .addModelObject(ietfSystem).build();
        return setNetconfObject(mo, session, ncDs, null);
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
    public void systemShutdown(NetconfSession session) {
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
