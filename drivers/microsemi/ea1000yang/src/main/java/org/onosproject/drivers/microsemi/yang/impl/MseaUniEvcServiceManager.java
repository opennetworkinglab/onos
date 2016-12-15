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
import static org.onosproject.yms.ydt.YmsOperationType.QUERY_CONFIG_REPLY;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Service;
import org.onosproject.drivers.microsemi.yang.MseaUniEvcServiceNetconfService;
import org.onosproject.netconf.NetconfException;
import org.onosproject.netconf.NetconfSession;
import org.onosproject.netconf.TargetConfig;
import org.onosproject.yang.gen.v1.http.www.microsemi.com.microsemi.edge.assure.msea.uni.evc.service.rev20160317.MseaUniEvcService;
import org.onosproject.yang.gen.v1.http.www.microsemi.com.microsemi.edge.assure.msea.uni.evc.service.rev20160317.MseaUniEvcServiceOpParam;
import org.onosproject.yang.gen.v1.http.www.microsemi.com.microsemi.edge.assure.msea.uni.evc.service.rev20160317.MseaUniEvcServiceService;
import org.onosproject.yang.gen.v1.http.www.microsemi.com.microsemi.edge.assure.msea.uni.evc.service.rev20160317.mseaunievcservice.mefservices.uni.UniSideInterfaceAssignmentEnum;

/**
 * Implementation of the MseaUniEvcServiceService YANG model service.
 */
@Component(immediate = true, inherit = true)
@Service
public class MseaUniEvcServiceManager extends AbstractYangServiceImpl
        implements MseaUniEvcServiceNetconfService {
    public static final String MSEA_SA_FILTERING = "org.onosproject.drivers.microsemi.yang.mseaunievcservice";

    @Activate
    public void activate() {
        appId = coreService.registerApplication(MSEA_SA_FILTERING);
        ych = ymsService.getYangCodecHandler();
        ych.addDeviceSchema(MseaUniEvcServiceService.class);
        log.info("MseaUniEvcServiceManager Started");
    }

    @Deactivate
    public void deactivate() {
        super.deactivate();
        ymsService.unRegisterService(this, MseaUniEvcServiceService.class);
        ych = null;
        log.info("MseaUniEvcServiceManager Stopped");
    }

    @Override
    public MseaUniEvcService getMseaUniEvcService(
            MseaUniEvcServiceOpParam mseaUniEvcService, NetconfSession session) throws NetconfException {
        return (MseaUniEvcService) getNetconfObject(mseaUniEvcService, session);
    }

    @Override
    public MseaUniEvcService getConfigMseaUniEvcService(
            MseaUniEvcServiceOpParam mseaUniEvcService, NetconfSession session, TargetConfig targetDs)
            throws NetconfException {
        return (MseaUniEvcService) getConfigNetconfObject(mseaUniEvcService, session, targetDs);
    }

    /**
     * Modify the configuration.
     */
    @Override
    public void setMseaUniEvcService(
            MseaUniEvcServiceOpParam mseaUniEvcService, NetconfSession session, TargetConfig ncDs)
                    throws NetconfException {
        setNetconfObject(mseaUniEvcService, session, ncDs);
    }

    @Override
    public MseaUniEvcService getmseaUniEvcCeVlanMaps(
            NetconfSession session, TargetConfig ncDs)
            throws NetconfException {
        if (session == null) {
            throw new NetconfException("Session is null when calling getMseaSaFiltering()");
        }

        String xmlResult = session.getConfig(ncDs, evcFilterQuery());

        List<Object> objectList = ych.decode(xmlResult, XML, QUERY_CONFIG_REPLY);
        if (objectList != null && objectList.size() > 0) {
            return (MseaUniEvcService) objectList.get(0);
        }

        return null;
    }

    @Override
    public void removeEvcUniFlowEntries(
            Map<Integer, String> ceVlanUpdates,
            Map<Integer, List<Short>> flowVlanIds,
            NetconfSession session, TargetConfig targetDs,
            UniSideInterfaceAssignmentEnum portAssign) throws NetconfException {

        List<Integer> evcAlreadyHandled = new ArrayList<>();
        StringBuilder xmlEvcUpdate = new StringBuilder(evcUniOpener());
        for (Integer evcKey:ceVlanUpdates.keySet()) {
            int evcId = (evcKey & ((1 << 8) - 100)) >> 2;
            if (evcAlreadyHandled.contains(new Integer(evcId))) {
                continue;
            }
            evcAlreadyHandled.add(evcId);
            int port = (evcKey & 3);
            String ceVlanMapThis = ceVlanUpdates.get(evcKey);
            String ceVlanMapOpposite = ceVlanUpdates.get(evcKey ^ 1);

            if ((ceVlanMapThis == null || ceVlanMapThis.isEmpty()) &&
                    (ceVlanMapOpposite == null || ceVlanMapOpposite.isEmpty())) {
                xmlEvcUpdate.append("<evc nc:operation=\"delete\">\n<evc-index>");
                xmlEvcUpdate.append(Integer.toString(evcId));
                xmlEvcUpdate.append("</evc-index>\n</evc>\n");
            } else {
                xmlEvcUpdate.append("<evc>\n<evc-index>");
                xmlEvcUpdate.append(Integer.toString(evcId));
                xmlEvcUpdate.append("</evc-index>\n<evc-per-uni>\n");
                if (port == 0 && portAssign == UniSideInterfaceAssignmentEnum.UNI_C_ON_OPTICS ||
                        port == 1 && portAssign == UniSideInterfaceAssignmentEnum.UNI_C_ON_HOST) {
                    if (ceVlanMapThis != null) {
                        xmlEvcUpdate.append("<evc-per-uni-c>\n<ce-vlan-map nc:operation=\"replace\">");
                        xmlEvcUpdate.append(ceVlanMapThis);
                        xmlEvcUpdate.append("</ce-vlan-map>\n");
                        xmlEvcUpdate.append(deleteFlowMapping(flowVlanIds.get(evcKey)));
                        xmlEvcUpdate.append("</evc-per-uni-c>\n");
                    }
                    if (ceVlanMapOpposite != null) {
                        xmlEvcUpdate.append("<evc-per-uni-n>\n<ce-vlan-map nc:operation=\"replace\">");
                        xmlEvcUpdate.append(ceVlanMapOpposite);
                        xmlEvcUpdate.append("</ce-vlan-map>\n");
                        xmlEvcUpdate.append(deleteFlowMapping(flowVlanIds.get(evcKey ^ 1)));
                        xmlEvcUpdate.append("</evc-per-uni-n>\n");
                    }
                } else {
                    if (ceVlanMapThis != null) {
                        xmlEvcUpdate.append("<evc-per-uni-n>\n<ce-vlan-map nc:operation=\"replace\">");
                        xmlEvcUpdate.append(ceVlanMapThis);
                        xmlEvcUpdate.append("</ce-vlan-map>\n");
                        xmlEvcUpdate.append(deleteFlowMapping(flowVlanIds.get(evcKey)));
                        xmlEvcUpdate.append("</evc-per-uni-n>\n");
                    }
                    if (ceVlanMapOpposite != null) {
                        xmlEvcUpdate.append("<evc-per-uni-c>\n<ce-vlan-map nc:operation=\"replace\">");
                        xmlEvcUpdate.append(ceVlanMapOpposite);
                        xmlEvcUpdate.append("</ce-vlan-map>\n");
                        xmlEvcUpdate.append(deleteFlowMapping(flowVlanIds.get(evcKey ^ 1)));
                        xmlEvcUpdate.append("</evc-per-uni-c>\n");
                    }
                }

                xmlEvcUpdate.append("</evc-per-uni>\n</evc>\n");
            }
        }
        xmlEvcUpdate.append("</uni>\n</mef-services>");

        log.info("Sending XML <edit-config> on NETCONF session " + session.getSessionId() +
                ":\n" + xmlEvcUpdate.toString());


        session.editConfig(targetDs, null, xmlEvcUpdate.toString());
    }


    private static String deleteFlowMapping(List<Short> vlanIds) {
        if (vlanIds == null || vlanIds.size() == 0) {
            return "";
        }
        StringBuilder fmXmlBuilder = new StringBuilder();
        for (long vlanId:vlanIds) {
            fmXmlBuilder.append("<flow-mapping nc:operation=\"delete\">\n");
            fmXmlBuilder.append("<ce-vlan-id>");
            fmXmlBuilder.append(String.valueOf(vlanId));
            fmXmlBuilder.append("</ce-vlan-id>\n");
            fmXmlBuilder.append("</flow-mapping>\n");
        }

        return fmXmlBuilder.toString();
    }

    private String evcFilterQuery() {
        StringBuilder sb = new StringBuilder("<mef-services "
                + "xmlns=\"http://www.microsemi.com/microsemi-edge-assure/msea-uni-evc-service\">");
        sb.append("<uni>");
        sb.append("<evc>");
        sb.append("<evc-index/>");
        sb.append("<evc-per-uni>");
        sb.append("<evc-per-uni-c>");
        sb.append("<ce-vlan-map/>");
        sb.append("<flow-mapping/>");
        sb.append("<ingress-bwp-group-index/>");
        sb.append("</evc-per-uni-c>");
        sb.append("<evc-per-uni-n>");
        sb.append("<ce-vlan-map/>");
        sb.append("<flow-mapping/>");
        sb.append("<ingress-bwp-group-index/>");
        sb.append("</evc-per-uni-n>");
        sb.append("</evc-per-uni>");
        sb.append("</evc>");
        sb.append("</uni>");
        sb.append("</mef-services>");

        return sb.toString();
    }

    private String evcUniOpener() {
        StringBuilder sb = new StringBuilder("<mef-services ");
        sb.append("xmlns=\"http://www.microsemi.com/microsemi-edge-assure/msea-uni-evc-service\">\n");
        sb.append("<uni>\n");

        return sb.toString();
    }
}