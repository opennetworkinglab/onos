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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Service;
import org.onosproject.drivers.microsemi.yang.MseaUniEvcServiceNetconfService;
import org.onosproject.netconf.DatastoreId;
import org.onosproject.netconf.NetconfException;
import org.onosproject.netconf.NetconfSession;
import org.onosproject.yang.gen.v1.mseaunievcservice.rev20160317.MseaUniEvcService;
import org.onosproject.yang.gen.v1.mseaunievcservice.rev20160317.MseaUniEvcServiceOpParam;
import org.onosproject.yang.gen.v1.mseaunievcservice.rev20160317.mseaunievcservice.MefServices;
import org.onosproject.yang.gen.v1.mseaunievcservice.rev20160317.mseaunievcservice.mefservices.profiles.BwpGroup;
import org.onosproject.yang.gen.v1.mseaunievcservice.rev20160317.mseaunievcservice.mefservices.uni.UniSideInterfaceAssignmentEnum;
import org.onosproject.yang.model.DefaultModelObjectData;
import org.onosproject.yang.model.ModelConverter;
import org.onosproject.yang.model.ModelObject;
import org.onosproject.yang.model.ModelObjectData;
import org.onosproject.yang.model.ResourceId;
import org.onosproject.yang.runtime.AnnotatedNodeInfo;
import org.onosproject.yang.runtime.CompositeData;
import org.onosproject.yang.runtime.DefaultAnnotatedNodeInfo;
import org.onosproject.yang.runtime.DefaultAnnotation;
import org.onosproject.yang.runtime.DefaultCompositeStream;

/**
 * Implementation of the MseaUniEvcServiceService YANG model service.
 */
@Component(immediate = true, inherit = true)
@Service
public class MseaUniEvcServiceManager extends AbstractYangServiceImpl
        implements MseaUniEvcServiceNetconfService {
    public static final String MSEA_UNI_EVC_SVC =
            "org.onosproject.drivers.microsemi.yang.mseaunievcservice";
    public static final String MSEA_UNI_EVC_SVC_NS =
            "http://www.microsemi.com/microsemi-edge-assure/msea-uni-evc-service";

    @Activate
    public void activate() {
        super.activate();
        appId = coreService.registerApplication(MSEA_UNI_EVC_SVC);
        log.info("MseaUniEvcServiceManager Started");
    }

    @Deactivate
    public void deactivate() {
        super.deactivate();
        log.info("MseaUniEvcServiceManager Stopped");
    }

    @Override
    public MseaUniEvcService getMseaUniEvcService(
            MseaUniEvcServiceOpParam mseaUniEvcService, NetconfSession session)
            throws NetconfException {

        return getConfigMseaUniEvcService(mseaUniEvcService, session, null);
    }

    @Override
    public MseaUniEvcService getConfigMseaUniEvcService(
            MseaUniEvcServiceOpParam mseaUniEvcService, NetconfSession session,
            DatastoreId targetDs) throws NetconfException {

        ModelObjectData moFilter = DefaultModelObjectData.builder()
                .addModelObject((ModelObject) mseaUniEvcService.mefServices()).build();

        ModelObjectData moReply = getConfigNetconfObject(moFilter, session, targetDs);

        MseaUniEvcService reply = new MseaUniEvcServiceOpParam();
        for (ModelObject mo:moReply.modelObjects()) {
            if (mo instanceof MefServices) {
                reply.mefServices((MefServices) mo);
            }
        }
        return reply;
    }

    /**
     * Modify the configuration.
     */
    @Override
    public boolean setMseaUniEvcService(MseaUniEvcServiceOpParam mseaUniEvcService,
                 NetconfSession session, DatastoreId ncDs) throws NetconfException {
        ModelObjectData moEdit = DefaultModelObjectData.builder()
                .addModelObject((ModelObject) mseaUniEvcService.mefServices()).build();

        return setNetconfObject(moEdit, session, ncDs, null);
    }

    /**
     * Delete the configuration.
     */
    @Override
    public boolean deleteMseaUniEvcService(MseaUniEvcServiceOpParam mseaUniEvcService,
                NetconfSession session, DatastoreId ncDs) throws NetconfException {
        ModelObjectData moEdit = DefaultModelObjectData.builder()
                .addModelObject((ModelObject) mseaUniEvcService.mefServices()).build();

        ArrayList anis = new ArrayList<AnnotatedNodeInfo>();
        for (BwpGroup bwpGrp:mseaUniEvcService.mefServices().profiles().bwpGroup()) {

            ResourceId.Builder ridBuilder = ResourceId.builder()
                    .addBranchPointSchema("/", null)
                    .addBranchPointSchema("mef-services", MSEA_UNI_EVC_SVC_NS)
                    .addBranchPointSchema("profiles", MSEA_UNI_EVC_SVC_NS)
                    .addBranchPointSchema("bwp-group", MSEA_UNI_EVC_SVC_NS)
                    .addKeyLeaf("group-index", MSEA_UNI_EVC_SVC_NS, bwpGrp.groupIndex());

            AnnotatedNodeInfo ani = DefaultAnnotatedNodeInfo.builder()
                    .resourceId(ridBuilder.build())
                    .addAnnotation(new DefaultAnnotation(NC_OPERATION, OP_DELETE))
                    .build();

            anis.add(ani);
        }


        return setNetconfObject(moEdit, session, ncDs, anis);
    }


    @Override
    public MseaUniEvcService getmseaUniEvcCeVlanMaps(
            NetconfSession session, DatastoreId ncDs)
            throws NetconfException {
        if (session == null) {
            throw new NetconfException("Session is null when calling getMseaSaFiltering()");
        }

        String xmlResult = session.getConfig(ncDs, evcFilterQuery());
        xmlResult = removeRpcReplyData(xmlResult);

        DefaultCompositeStream resultDcs = new DefaultCompositeStream(
                null, new ByteArrayInputStream(xmlResult.getBytes()));
        CompositeData compositeData = xSer.decode(resultDcs, yCtx);

        ModelObjectData moReply = ((ModelConverter) yangModelRegistry).createModel(compositeData.resourceData());

        MseaUniEvcService reply = new MseaUniEvcServiceOpParam();
        for (ModelObject mo:moReply.modelObjects()) {
            if (mo instanceof MefServices) {
                reply.mefServices((MefServices) mo);
            }
        }
        return reply;
    }

    @Override
    public void removeEvcUniFlowEntries(
            Map<Integer, String> ceVlanUpdates,
            Map<Integer, List<Short>> flowVlanIds,
            NetconfSession session, DatastoreId targetDs,
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
