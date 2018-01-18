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

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Service;
import org.onosproject.drivers.microsemi.yang.MseaSaFilteringNetconfService;
import org.onosproject.netconf.DatastoreId;
import org.onosproject.netconf.NetconfException;
import org.onosproject.netconf.NetconfSession;
import org.onosproject.yang.gen.v1.mseasafiltering.rev20160412.MseaSaFiltering;
import org.onosproject.yang.gen.v1.mseasafiltering.rev20160412.MseaSaFilteringOpParam;
import org.onosproject.yang.gen.v1.mseasafiltering.rev20160412.mseasafiltering.SourceIpaddressFiltering;
import org.onosproject.yang.gen.v1.mseasafiltering.rev20160412.mseasafiltering.sourceipaddressfiltering.interfaceeth0.SourceAddressRange;
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

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of the MseaSaFiltering YANG model service.
 */
@Component(immediate = true, inherit = true)
@Service
public class MseaSaFilteringManager extends AbstractYangServiceImpl
    implements MseaSaFilteringNetconfService {
    public static final String MSEA_SA_FILTERING =
            "org.onosproject.drivers.microsemi.yang.mseasafiltering";
    public static final String MSEA_SA_FILTERING_NS =
            "http://www.microsemi.com/microsemi-edge-assure/msea-sa-filtering";

    @Activate
    public void activate() {
        super.activate();
        appId = coreService.registerApplication(MSEA_SA_FILTERING);
        log.info("MseaSaFilteringManager Started");
    }

    @Deactivate
    public void deactivate() {
        super.deactivate();
        log.info("MseaSaFilteringManager Stopped");
    }

    /**
     * Get a filtered subset of the model.
     * This is meant to filter the current live model
     * against the attribute(s) given in the argument
     * and return the filtered model.
     */
    @Override
    public MseaSaFiltering getMseaSaFiltering(
            MseaSaFilteringOpParam mseaSaFilteringFilter, NetconfSession session)
            throws NetconfException {
        ModelObjectData moQuery = DefaultModelObjectData.builder()
                .addModelObject((ModelObject) mseaSaFilteringFilter
                        .sourceIpaddressFiltering())
                .build();


        ModelObjectData moReply = getNetconfObject(moQuery, session);

        MseaSaFiltering reply = new MseaSaFilteringOpParam();
        for (ModelObject mo:moReply.modelObjects()) {
            if (mo instanceof SourceIpaddressFiltering) {
                reply.sourceIpaddressFiltering((SourceIpaddressFiltering) mo);
            }
        }
        return reply;
    }

    /**
     * Get a filtered subset of the config model (from running)
     * This is meant to filter the current live model
     * against the attribute(s) given in the argument
     * and return the filtered model.
     */
    @Override
    public List<SourceAddressRange> getConfigMseaSaFilterIds(NetconfSession session)
            throws NetconfException {

        String xmlResult = session.getConfig(DatastoreId.RUNNING, saFilterQuery());
        xmlResult = removeRpcReplyData(xmlResult);

        DefaultCompositeStream resultDcs = new DefaultCompositeStream(
                null, new ByteArrayInputStream(xmlResult.getBytes()));
        CompositeData compositeData = xSer.decode(resultDcs, yCtx);

        ModelObjectData moReply = ((ModelConverter) yangModelRegistry).createModel(compositeData.resourceData());

        MseaSaFiltering reply = new MseaSaFilteringOpParam();
        for (ModelObject mo:moReply.modelObjects()) {
            if (mo instanceof SourceIpaddressFiltering) {
                reply.sourceIpaddressFiltering((SourceIpaddressFiltering) mo);
            }
        }
        if (reply.sourceIpaddressFiltering() != null &&
                reply.sourceIpaddressFiltering().interfaceEth0() != null) {
            return reply.sourceIpaddressFiltering().interfaceEth0().sourceAddressRange();
        } else {
            return new ArrayList<SourceAddressRange>();
        }
    }

    /**
     * Call NETCONF edit-config with a configuration.
     */
    @Override
    public boolean setMseaSaFiltering(MseaSaFilteringOpParam mseaSaFiltering,
           NetconfSession session, DatastoreId ncDs) throws NetconfException {

        ModelObjectData moQuery = DefaultModelObjectData.builder()
                .addModelObject((ModelObject) mseaSaFiltering
                        .sourceIpaddressFiltering()).build();

        return setNetconfObject(moQuery, session, ncDs, null);
    }

    @Override
    public boolean deleteMseaSaFilteringRange(MseaSaFilteringOpParam mseaSaFiltering,
                                      NetconfSession session, DatastoreId ncDs) throws NetconfException {

        ModelObjectData moQuery = DefaultModelObjectData.builder()
                .addModelObject((ModelObject) mseaSaFiltering
                        .sourceIpaddressFiltering()).build();

        ArrayList anis = new ArrayList<AnnotatedNodeInfo>();
        if (mseaSaFiltering.sourceIpaddressFiltering().interfaceEth0() != null &&
                mseaSaFiltering.sourceIpaddressFiltering().interfaceEth0().sourceAddressRange() != null) {

            for (SourceAddressRange sar:mseaSaFiltering.sourceIpaddressFiltering()
                    .interfaceEth0().sourceAddressRange()) {
                String sarRangeIdStr = String.valueOf(sar.rangeId());

                ResourceId.Builder ridBuilder = ResourceId.builder()
                        .addBranchPointSchema("/", null)
                        .addBranchPointSchema("source-ipaddress-filtering", MSEA_SA_FILTERING_NS)
                        .addBranchPointSchema("interface-eth0", MSEA_SA_FILTERING_NS)
                        .addBranchPointSchema("source-address-range", MSEA_SA_FILTERING_NS)
                        .addKeyLeaf("range-id", MSEA_SA_FILTERING_NS, sarRangeIdStr);

                AnnotatedNodeInfo ani = DefaultAnnotatedNodeInfo.builder()
                        .resourceId(ridBuilder.build())
                        .addAnnotation(new DefaultAnnotation(NC_OPERATION, OP_DELETE))
                        .build();

                anis.add(ani);
            }
        } else {
            //Delete all
            ResourceId.Builder ridBuilder = ResourceId.builder()
                    .addBranchPointSchema("/", null)
                    .addBranchPointSchema("source-ipaddress-filtering", MSEA_SA_FILTERING_NS);
            AnnotatedNodeInfo ani = DefaultAnnotatedNodeInfo.builder()
                    .resourceId(ridBuilder.build())
                    .addAnnotation(new DefaultAnnotation(NC_OPERATION, OP_DELETE))
                    .build();
            anis.add(ani);
        }

        return setNetconfObject(moQuery, session, ncDs, anis);
    }


    private static String saFilterQuery() {
        StringBuilder sb = new StringBuilder("<source-ipaddress-filtering " +
                "xmlns=\"http://www.microsemi.com/microsemi-edge-assure/msea-sa-filtering\">");
        sb.append("<interface-eth0>");
        sb.append("<filter-admin-state>blacklist</filter-admin-state>");
        sb.append("<source-address-range>");
        sb.append("<range-id/>");
        sb.append("</source-address-range>");
        sb.append("</interface-eth0>");
        sb.append("</source-ipaddress-filtering>");
        return sb.toString();
    }
}
