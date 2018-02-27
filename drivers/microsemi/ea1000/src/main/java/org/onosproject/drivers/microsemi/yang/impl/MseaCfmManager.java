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

import java.util.ArrayList;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Service;
import org.onosproject.drivers.microsemi.yang.MseaCfmNetconfService;
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
import org.onosproject.yang.gen.v1.mseacfm.rev20160229.mseacfm.DefaultMefCfm;
import org.onosproject.yang.gen.v1.mseacfm.rev20160229.mseacfm.abortloopback.AbortLoopbackInput;
import org.onosproject.yang.gen.v1.mseacfm.rev20160229.mseacfm.mefcfm.MaintenanceDomain;
import org.onosproject.yang.gen.v1.mseacfm.rev20160229.mseacfm.mefcfm.maintenancedomain.MaintenanceAssociation;
import org.onosproject.yang.gen.v1.mseacfm.rev20160229.mseacfm.mefcfm.maintenancedomain.maintenanceassociation.MaintenanceAssociationEndPoint;
import org.onosproject.yang.gen.v1.mseacfm.rev20160229.mseacfm.transmitlinktrace.TransmitLinktraceInput;
import org.onosproject.yang.gen.v1.mseacfm.rev20160229.mseacfm.transmitlinktrace.TransmitLinktraceOutput;
import org.onosproject.yang.gen.v1.mseacfm.rev20160229.mseacfm.transmitloopback.TransmitLoopbackInput;
import org.onosproject.yang.gen.v1.mseasoampm.rev20160229.mseasoampm.mefcfm.maintenancedomain.maintenanceassociation.maintenanceassociationendpoint.AugmentedMseaCfmMaintenanceAssociationEndPoint;
import org.onosproject.yang.gen.v1.mseasoampm.rev20160229.mseasoampm.mefcfm.maintenancedomain.maintenanceassociation.maintenanceassociationendpoint.DefaultAugmentedMseaCfmMaintenanceAssociationEndPoint;
import org.onosproject.yang.gen.v1.mseasoampm.rev20160229.mseasoampm.mefcfm.maintenancedomain.maintenanceassociation.maintenanceassociationendpoint.augmentedmseacfmmaintenanceassociationendpoint.delaymeasurements.DelayMeasurement;
import org.onosproject.yang.gen.v1.mseatypes.rev20160229.mseatypes.MepIdType;
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
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Implementation of the MseaCfmServiceNetconf YANG model service.
 */
@Component(immediate = true, inherit = true)
@Service
public class MseaCfmManager extends AbstractYangServiceImpl
    implements MseaCfmNetconfService {

    private static final String MSEA_CFM = "org.onosproject.drivers.microsemi.yang.mseacfmservice";

    private static final String MSEA_CFM_NS = "http://www.microsemi.com/microsemi-edge-assure/msea-cfm";
    private static final String MSEA_CFM_PM_NS = "http://www.microsemi.com/microsemi-edge-assure/msea-soam-pm";

    //FIXME Remove when the issue with Null bits on onos-yang-tools is sorted
    @Deprecated
    protected static final Pattern REGEX_EMPTY_ACTIVE_DEFECTS =
            Pattern.compile("(<active-defects)[ ]?(/>)", Pattern.DOTALL);
    //FIXME Remove when the issue with Null bits on onos-yang-tools is sorted
    @Deprecated
    protected static final Pattern REGEX_EMPTY_LAST_DEFECT_SENT =
            Pattern.compile("(<msea-soam-fm:last-defect-sent)[ ]?(/>)", Pattern.DOTALL);
    private static final String MEF_CFM = "mef-cfm";
    private static final String MAINTENANCE_DOMAIN = "maintenance-domain";
    public static final String ID = "id";
    private static final String MAINTENANCE_ASSOCIATION = "maintenance-association";
    private static final String TRANSMIT_LOOPBACK = "transmit-loopback";
    private static final String ABORT_LOOPBACK = "abort-loopback";
    private static final String MAINTENANCE_ASSOCIATION_END_POINT = "maintenance-association-end-point";
    private static final String MEP_ID = "mep-id";
    private static final String DELAY_MEASUREMENTS = "delay-measurements";
    private static final String DELAY_MEASUREMENT = "delay-measurement";
    private static final String DM_ID = "dm-id";
    private static final String MEP_IDENTIFIER = "mep-identifier";
    private static final String REMOTE_MEPS = "remote-meps";

    @Activate
    public void activate() {
        super.activate();
        appId = coreService.registerApplication(MSEA_CFM);
        log.info("MseaCfmService Started");
    }

    @Deactivate
    public void deactivate() {
        super.deactivate();
        log.info("MseaCfmService Stopped");
    }

    @Override
    public MseaCfm getMepEssentials(MdId mdId, MaIdShort maId, MepId mepId,
                                    NetconfSession session) throws NetconfException {
        if (session == null) {
            throw new NetconfException("Session is null when calling getMepEssentials()");
        }

        String xmlQueryStr = buildMepEssentialsQueryString(mdId, maId, mepId);
        log.debug("Sending <get> for MEP essentials" +
                " query on NETCONF session " + session.getSessionId() +
                ":\n" + xmlQueryStr);

        String xmlResult = session.get(xmlQueryStr, null);
        xmlResult = removeRpcReplyData(xmlResult);
        DefaultCompositeStream resultDcs = new DefaultCompositeStream(
                null, new ByteArrayInputStream(xmlResult.getBytes()));
        CompositeData compositeData = xSer.decode(resultDcs, yCtx);

        ModelObjectData mod = ((ModelConverter) yangModelRegistry).createModel(
                compositeData.resourceData());

        MseaCfmOpParam mseaCfm = new MseaCfmOpParam();
        for (ModelObject mo:mod.modelObjects()) {
            if (mo instanceof DefaultMefCfm) {
                mseaCfm.mefCfm((DefaultMefCfm) mo);
            }
        }
        return mseaCfm;
    }

    @Override
    public MseaCfm getMepFull(MdId mdId, MaIdShort maId, MepId mepId,
            NetconfSession session) throws NetconfException {
        if (session == null) {
            throw new NetconfException("Session is null when calling getMepFull()");
        }

        String xmlQueryStr = buildMepFullQueryString(mdId, maId, mepId);
        log.debug("Sending <get> for full MEP" +
                " query on NETCONF session " + session.getSessionId() +
                ":\n" + xmlQueryStr);

        String xmlResult = session.get(xmlQueryStr, null);
        xmlResult = removeRpcReplyData(xmlResult);
        xmlResult = removeEmptyActiveDefects(xmlResult);
        DefaultCompositeStream resultDcs = new DefaultCompositeStream(
                null, new ByteArrayInputStream(xmlResult.getBytes()));
        CompositeData compositeData = xSer.decode(resultDcs, yCtx);

        ModelObjectData mod = ((ModelConverter) yangModelRegistry).createModel(compositeData.resourceData());

        MseaCfmOpParam mseaCfm = new MseaCfmOpParam();
        for (ModelObject mo:mod.modelObjects()) {
            if (mo instanceof DefaultMefCfm) {
                mseaCfm.mefCfm((DefaultMefCfm) mo);
            }
        }

        return mseaCfm;
    }

    @Override
    public MseaCfm getMepIds(Optional<MdId> mdIdOptional, Optional<MaIdShort> maIdOptional,
             NetconfSession session, DatastoreId targetDs) throws NetconfException {

        ModelObjectData.Builder moQueryBldr = DefaultModelObjectData.builder();

        ArrayList<AnnotatedNodeInfo> annotations = new ArrayList<>();
        String xmlQueryStr = encodeMoToXmlStr(moQueryBldr.build(), annotations);

        log.debug("Sending <get> for full MEP" +
                " query on NETCONF session " + session.getSessionId() +
                ":\n" + xmlQueryStr);

        String xmlResult = session.get(xmlQueryStr, null);
        xmlResult = removeRpcReplyData(xmlResult);
        xmlResult = removeEmptyActiveDefects(xmlResult);
        DefaultCompositeStream resultDcs = new DefaultCompositeStream(
                null, new ByteArrayInputStream(xmlResult.getBytes()));
        CompositeData compositeData = xSer.decode(resultDcs, yCtx);

        ModelObjectData mod = ((ModelConverter) yangModelRegistry).createModel(compositeData.resourceData());

        MseaCfmOpParam mseaCfm = new MseaCfmOpParam();
        for (ModelObject mo:mod.modelObjects()) {
            if (mo instanceof DefaultMefCfm) {
                mseaCfm.mefCfm((DefaultMefCfm) mo);
            }
        }

        return mseaCfm;
    }

    @Override
    public MseaCfm getSoamDm(MdId mdName, MaIdShort maName, MepId mepId,
                             SoamId dmId, DmEntryParts parts, NetconfSession session)
                    throws NetconfException {
        String xmlQueryStr = buildDmQueryString(mdName, maName, mepId, dmId, parts);
        log.debug("Sending <get> for " +
                " query on NETCONF session " + session.getSessionId() +
                ":\n" + xmlQueryStr);

        String xmlResult = session.get(xmlQueryStr, null);
        xmlResult = removeRpcReplyData(xmlResult);
        DefaultCompositeStream resultDcs = new DefaultCompositeStream(
                null, new ByteArrayInputStream(xmlResult.getBytes()));
        CompositeData compositeData = xSer.decode(resultDcs, yCtx);

        ModelObjectData mod = ((ModelConverter) yangModelRegistry).createModel(compositeData.resourceData());

        MseaCfmOpParam mseaCfm = new MseaCfmOpParam();
        for (ModelObject mo:mod.modelObjects()) {
            if (mo instanceof DefaultMefCfm) {
                mseaCfm.mefCfm((DefaultMefCfm) mo);
            }
        }

        return mseaCfm;
    }

    @Override
    public boolean setMseaCfm(MseaCfmOpParam mseaCfm, NetconfSession session,
            DatastoreId targetDs) throws NetconfException {

        ModelObjectData moQuery = DefaultModelObjectData.builder()
                .addModelObject((ModelObject) mseaCfm.mefCfm()).build();
        return setNetconfObject(moQuery, session, targetDs, null);
    }

    @Override
    public boolean deleteMseaCfmDm(MseaCfmOpParam mseaCfm, NetconfSession session,
                            DatastoreId targetDs) throws NetconfException, CfmConfigException {

        if (mseaCfm.mefCfm() == null) {
            throw new CfmConfigException("mefCfm object must be present before Meps can be added");
        }
        ModelObjectData mseCfmDmList = DefaultModelObjectData.builder()
                .addModelObject(mseaCfm).build();

        ArrayList<AnnotatedNodeInfo> anis = new ArrayList<>();
        for (MaintenanceDomain md:mseaCfm.mefCfm().maintenanceDomain()) {
            if (md.id() == 0) {
                throw new CfmConfigException("An MD numeric ID must be given");
            }
            for (MaintenanceAssociation ma:md.maintenanceAssociation()) {
                if (ma.id() == 0) {
                    throw new CfmConfigException("An MA numeric ID must be given");
                }
                for (MaintenanceAssociationEndPoint mep:ma.maintenanceAssociationEndPoint()) {
                    AugmentedMseaCfmMaintenanceAssociationEndPoint mepAugment =
                        mep.augmentation(DefaultAugmentedMseaCfmMaintenanceAssociationEndPoint.class);
                    if (mepAugment != null && mepAugment.delayMeasurements() != null) {
                        for (DelayMeasurement dm:mepAugment.delayMeasurements().delayMeasurement()) {
                            ResourceId.Builder ridBuilder = ResourceId.builder()
                                    .addBranchPointSchema("/", null)
                                    .addBranchPointSchema(MEF_CFM, MSEA_CFM_NS)
                                    .addBranchPointSchema(MAINTENANCE_DOMAIN, MSEA_CFM_NS)
                                    .addKeyLeaf(ID, MSEA_CFM_NS, md.id())
                                    .addBranchPointSchema(MAINTENANCE_ASSOCIATION, MSEA_CFM_NS)
                                    .addKeyLeaf(ID, MSEA_CFM_NS, ma.id())
                                    .addBranchPointSchema(MAINTENANCE_ASSOCIATION_END_POINT, MSEA_CFM_NS)
                                    .addKeyLeaf(MEP_ID, MSEA_CFM_NS, mep.mepIdentifier())
                                    .addBranchPointSchema(DELAY_MEASUREMENTS, MSEA_CFM_PM_NS)
                                    .addBranchPointSchema(DELAY_MEASUREMENT, MSEA_CFM_PM_NS)
                                    .addKeyLeaf(DM_ID, MSEA_CFM_PM_NS, dm.dmId());
                            AnnotatedNodeInfo ani = DefaultAnnotatedNodeInfo.builder()
                                    .resourceId(ridBuilder.build())
                                    .addAnnotation(new DefaultAnnotation(NC_OPERATION, OP_DELETE))
                                    .build();
                            anis.add(ani);
                        }
                    }
                }
            }
        }

        return setNetconfObject(mseCfmDmList, session, targetDs, anis);
    }

    @Override
    public boolean deleteMseaMep(MseaCfmOpParam mseaCfm, NetconfSession session,
                                   DatastoreId targetDs) throws NetconfException, CfmConfigException {

        if (mseaCfm.mefCfm() == null) {
            throw new CfmConfigException("mefCfm object must be present before Meps can be added");
        }
        ModelObjectData mseCfmMepList = DefaultModelObjectData.builder()
                .addModelObject((ModelObject) mseaCfm.mefCfm()).build();

        ArrayList<AnnotatedNodeInfo> anis = new ArrayList<>();
        for (MaintenanceDomain md:mseaCfm.mefCfm().maintenanceDomain()) {
            if (md.id() == 0) {
                throw new CfmConfigException("An MD numeric ID must be given");
            }
            for (MaintenanceAssociation ma:md.maintenanceAssociation()) {
                if (ma.id() == 0) {
                    throw new CfmConfigException("An MA numeric ID must be given");
                }
                for (MaintenanceAssociationEndPoint mep:ma.maintenanceAssociationEndPoint()) {
                    ResourceId.Builder ridBuilder = ResourceId.builder()
                            .addBranchPointSchema("/", null)
                            .addBranchPointSchema(MEF_CFM, MSEA_CFM_NS)
                            .addBranchPointSchema(MAINTENANCE_DOMAIN, MSEA_CFM_NS)
                            .addKeyLeaf(ID, MSEA_CFM_NS, md.id())
                            .addBranchPointSchema(MAINTENANCE_ASSOCIATION, MSEA_CFM_NS)
                            .addKeyLeaf(ID, MSEA_CFM_NS, ma.id())
                            .addBranchPointSchema(MAINTENANCE_ASSOCIATION_END_POINT, MSEA_CFM_NS)
                            .addKeyLeaf(MEP_IDENTIFIER, MSEA_CFM_NS, mep.mepIdentifier().uint16());
                    AnnotatedNodeInfo ani = DefaultAnnotatedNodeInfo.builder()
                            .resourceId(ridBuilder.build())
                            .addAnnotation(new DefaultAnnotation(NC_OPERATION, OP_DELETE))
                            .build();
                    anis.add(ani);
                }
            }
        }

        return setNetconfObject(mseCfmMepList, session, targetDs, anis);
    }

    @Override
    public boolean deleteMseaMa(MseaCfmOpParam mseaCfm, NetconfSession session,
                                 DatastoreId targetDs) throws NetconfException, CfmConfigException {
        if (mseaCfm.mefCfm() == null) {
            throw new CfmConfigException("mefCfm object must be present before Meps can be added");
        }

        ModelObjectData mseCfmMepList = DefaultModelObjectData.builder()
                .addModelObject((ModelObject) mseaCfm.mefCfm()).build();
        ArrayList<AnnotatedNodeInfo> anis = new ArrayList<>();
        for (MaintenanceDomain md:mseaCfm.mefCfm().maintenanceDomain()) {
            if (md.id() == 0) {
                throw new CfmConfigException("An MD numeric ID must be given");
            }
            for (MaintenanceAssociation ma:md.maintenanceAssociation()) {
                if (ma.id() == 0) {
                    throw new CfmConfigException("An MA numeric ID must be given");
                }
                ResourceId.Builder ridBuilder = ResourceId.builder()
                        .addBranchPointSchema("/", null)
                        .addBranchPointSchema(MEF_CFM, MSEA_CFM_NS)
                        .addBranchPointSchema(MAINTENANCE_DOMAIN, MSEA_CFM_NS)
                        .addKeyLeaf(ID, MSEA_CFM_NS, md.id())
                        .addBranchPointSchema(MAINTENANCE_ASSOCIATION, MSEA_CFM_NS)
                        .addKeyLeaf(ID, MSEA_CFM_NS, ma.id());

                AnnotatedNodeInfo ani = DefaultAnnotatedNodeInfo.builder()
                .resourceId(ridBuilder.build())
                .addAnnotation(new DefaultAnnotation(NC_OPERATION, OP_DELETE))
                .build();
                anis.add(ani);
            }
        }

        return setNetconfObject(mseCfmMepList, session, targetDs, anis);
    }

    @Override
    public boolean deleteMseaMaRMep(MseaCfmOpParam mseaCfm, NetconfSession session,
                                 DatastoreId targetDs) throws NetconfException, CfmConfigException {
        if (mseaCfm.mefCfm() == null) {
            throw new CfmConfigException("mefCfm object must be present before Meps can be added");
        }

        ModelObjectData mseCfmMepList = DefaultModelObjectData.builder()
                .addModelObject((ModelObject) mseaCfm.mefCfm()).build();
        ArrayList<AnnotatedNodeInfo> anis = new ArrayList<>();
        for (MaintenanceDomain md:mseaCfm.mefCfm().maintenanceDomain()) {
            if (md.id() == 0) {
                throw new CfmConfigException("An MD numeric ID must be given");
            }
            for (MaintenanceAssociation ma:md.maintenanceAssociation()) {
                if (ma.id() == 0) {
                    throw new CfmConfigException("An MA numeric ID must be given");
                }
                for (MepIdType rmep:ma.remoteMeps()) {
                    ResourceId.Builder ridBuilder = ResourceId.builder()
                            .addBranchPointSchema("/", null)
                            .addBranchPointSchema(MEF_CFM, MSEA_CFM_NS)
                            .addBranchPointSchema(MAINTENANCE_DOMAIN, MSEA_CFM_NS)
                            .addKeyLeaf(ID, MSEA_CFM_NS, md.id())
                            .addBranchPointSchema(MAINTENANCE_ASSOCIATION, MSEA_CFM_NS)
                            .addKeyLeaf(ID, MSEA_CFM_NS, ma.id())
                            .addLeafListBranchPoint(REMOTE_MEPS, MSEA_CFM_NS,
                                    rmep.uint16());
                    AnnotatedNodeInfo ani = DefaultAnnotatedNodeInfo.builder()
                            .resourceId(ridBuilder.build())
                            .addAnnotation(new DefaultAnnotation(NC_OPERATION, OP_DELETE))
                            .build();
                    anis.add(ani);
                }
            }
        }

        return setNetconfObject(mseCfmMepList, session, targetDs, anis);
    }


    @Override
    public boolean deleteMseaMd(MseaCfmOpParam mseaCfm, NetconfSession session,
                                DatastoreId targetDs) throws NetconfException, CfmConfigException {
        if (mseaCfm.mefCfm() == null) {
            throw new CfmConfigException("mefCfm object must be present before Meps can be added");
        }
        ModelObjectData mseCfmMepList = DefaultModelObjectData.builder()
                .addModelObject((ModelObject) mseaCfm.mefCfm()).build();

        ArrayList<AnnotatedNodeInfo> anis = new ArrayList<>();
        for (MaintenanceDomain md:mseaCfm.mefCfm().maintenanceDomain()) {
            if (md.id() == 0) {
                throw new CfmConfigException("An MD numeric ID must be given");
            }
            ResourceId.Builder ridBuilder = ResourceId.builder()
                    .addBranchPointSchema("/", null)
                    .addBranchPointSchema(MEF_CFM, MSEA_CFM_NS)
                    .addBranchPointSchema(MAINTENANCE_DOMAIN, MSEA_CFM_NS)
                    .addKeyLeaf(ID, MSEA_CFM_NS, md.id());
            AnnotatedNodeInfo ani = DefaultAnnotatedNodeInfo.builder()
                    .resourceId(ridBuilder.build())
                    .addAnnotation(new DefaultAnnotation(NC_OPERATION, OP_DELETE))
                    .build();
            anis.add(ani);
        }

        return setNetconfObject(mseCfmMepList, session, targetDs, anis);
    }

    /**
     * Call RPCs on the device through NETCONF.
     */
    @Override
    public void transmitLoopback(TransmitLoopbackInput inputVar,
            NetconfSession session) throws NetconfException {

        ModelObjectData transLoopbackMo = DefaultModelObjectData.builder()
                .addModelObject((ModelObject) inputVar).build();

        customRpcNetconf(transLoopbackMo,
                TRANSMIT_LOOPBACK, session);
    }

    @Override
    public void abortLoopback(AbortLoopbackInput inputVar,
            NetconfSession session) throws NetconfException {
        ModelObjectData abortLoopbackMo = DefaultModelObjectData.builder()
                .addModelObject((ModelObject) inputVar).build();

        customRpcNetconf(abortLoopbackMo, ABORT_LOOPBACK, session);
    }

    @Override
    public TransmitLinktraceOutput transmitLinktrace(
            TransmitLinktraceInput inputVar, NetconfSession session)
            throws NetconfException {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    //FIXME Remove when the fix for null bits with onos-yang-tools
    // https://gerrit.onosproject.org/#/c/15777/ is available
    @Deprecated
    private static String removeEmptyActiveDefects(String rpcReplyXml) throws NetconfException {
        rpcReplyXml = REGEX_EMPTY_ACTIVE_DEFECTS.matcher(rpcReplyXml).replaceFirst("");
        rpcReplyXml = REGEX_EMPTY_LAST_DEFECT_SENT.matcher(rpcReplyXml).replaceFirst("");

        return rpcReplyXml;
    }

    @Deprecated //Replace this with a ModelObject defintion
    private String buildMepEssentialsQueryString(MdId mdId, MaIdShort maId,
            MepId mepId) {
        StringBuilder rpc = new StringBuilder();

        rpc.append("<mef-cfm xmlns=\"http://www.microsemi.com/microsemi-edge-assure/msea-cfm\" ");
        rpc.append(" xmlns:msea-soam-fm=\"http://www.microsemi.com/microsemi-edge-assure/msea-soam-fm\" ");
        rpc.append("xmlns:msea-soam-pm=\"http://www.microsemi.com/microsemi-edge-assure/msea-soam-pm\">\n");
        rpc.append("<maintenance-domain>\n");
        rpc.append("<id/>\n");
        rpc.append("<name>");
        rpc.append(mdId.mdName());
        rpc.append("</name>\n");
        rpc.append("<maintenance-association>\n");
        rpc.append("<id/>\n");
        rpc.append("<name>");
        rpc.append(maId.maName());
        rpc.append("</name>\n");
        rpc.append("<ccm-interval>10ms</ccm-interval>\n");
        rpc.append("<remote-meps/>\n");
        rpc.append("<component-list/>\n");
        rpc.append("<maintenance-association-end-point>\n");
        rpc.append("<mep-identifier>");
        rpc.append(mepId.id());
        rpc.append("</mep-identifier>\n");
        rpc.append("<mac-address/>\n");
        rpc.append("<remote-mep-database>\n");
        rpc.append("<remote-mep>\n");
        rpc.append("<remote-mep-id/>\n");
        rpc.append("</remote-mep>\n");
        rpc.append("</remote-mep-database>\n");
        rpc.append("<msea-soam-pm:delay-measurements>\n");
        rpc.append("<msea-soam-pm:delay-measurement>\n");
        rpc.append("<msea-soam-pm:dm-id/>\n");
        rpc.append("</msea-soam-pm:delay-measurement>\n");
        rpc.append("</msea-soam-pm:delay-measurements>\n");
        rpc.append("<msea-soam-pm:loss-measurements>\n");
        rpc.append("<msea-soam-pm:loss-measurement>\n");
        rpc.append("<msea-soam-pm:lm-id/>\n");
        rpc.append("</msea-soam-pm:loss-measurement>\n");
        rpc.append("</msea-soam-pm:loss-measurements>\n");
        rpc.append("</maintenance-association-end-point>\n");
        rpc.append("</maintenance-association>\n");
        rpc.append("</maintenance-domain>\n");
        rpc.append("</mef-cfm>");

        return rpc.toString();
    }

    @Deprecated //Replace this with a ModelObject defintion
    private String buildMepFullQueryString(MdId mdId, MaIdShort maId, MepId mepId) {
        StringBuilder rpc = new StringBuilder();

        rpc.append("<mef-cfm xmlns=\"http://www.microsemi.com/microsemi-edge-assure/msea-cfm\" ");
        rpc.append(" xmlns:msea-soam-fm=\"http://www.microsemi.com/microsemi-edge-assure/msea-soam-fm\" ");
        rpc.append("xmlns:msea-soam-pm=\"http://www.microsemi.com/microsemi-edge-assure/msea-soam-pm\">\n");
        rpc.append("<maintenance-domain>\n");
        rpc.append("<id/>\n");
        rpc.append("<name>");
        rpc.append(mdId.mdName());
        rpc.append("</name>\n");
        rpc.append("<maintenance-association>\n");
        rpc.append("<id/>\n");
        rpc.append("<name>");
        rpc.append(maId.maName());
        rpc.append("</name>\n");
        rpc.append("<maintenance-association-end-point>\n");
        rpc.append("<mep-identifier>");
        rpc.append(mepId.id());
        rpc.append("</mep-identifier>\n");
        rpc.append("<interface/>\n");
        //Direction will always be DOWN for EA1000
        rpc.append("<primary-vid/>\n");
        rpc.append("<administrative-state/>\n");
        rpc.append("<mac-address/>\n");
        rpc.append("<ccm-ltm-priority/>\n");
        rpc.append("<continuity-check/>\n"); //Container
        rpc.append("<loopback/>\n"); //Container
        rpc.append("<linktrace/>\n"); //Container
        rpc.append("<remote-mep-database/>\n"); //Container
        rpc.append("<msea-soam-fm:operational-state/>\n");
        rpc.append("<msea-soam-fm:connectivity-status/>\n");
        rpc.append("<msea-soam-fm:port-status/>\n");
        rpc.append("<msea-soam-fm:interface-status/>\n");
        rpc.append("<msea-soam-fm:last-defect-sent/>\n");
        rpc.append("<msea-soam-fm:rdi-transmit-status/>\n");
        rpc.append("</maintenance-association-end-point>\n");
        rpc.append("</maintenance-association>\n");
        rpc.append("</maintenance-domain>\n");
        rpc.append("</mef-cfm>");

        return rpc.toString();
    }

    @Deprecated //Replace this with a ModelObject defintion
    private String buildDmQueryString(MdId mdId, MaIdShort maId, MepId mepId,
            SoamId dmId, DmEntryParts parts) {
        StringBuilder rpc = new StringBuilder();

        rpc.append("<mef-cfm xmlns=\"http://www.microsemi.com/microsemi-edge-assure/msea-cfm\" ");
        rpc.append(" xmlns:msea-soam-fm=\"http://www.microsemi.com/microsemi-edge-assure/msea-soam-fm\" ");
        rpc.append("xmlns:msea-soam-pm=\"http://www.microsemi.com/microsemi-edge-assure/msea-soam-pm\">\n");
        rpc.append("<maintenance-domain>\n");
        rpc.append("<id/>\n");
        rpc.append("<name>");
        rpc.append(mdId.mdName());
        rpc.append("</name>\n");
        rpc.append("<maintenance-association>\n");
        rpc.append("<id/>\n");
        rpc.append("<name>");
        rpc.append(maId.maName());
        rpc.append("</name>\n");
        rpc.append("<maintenance-association-end-point>\n");
        rpc.append("<mep-identifier>");
        rpc.append(mepId.id());
        rpc.append("</mep-identifier>\n");
        if (dmId != null) {
            rpc.append("<msea-soam-pm:delay-measurements>");
            rpc.append("<msea-soam-pm:delay-measurement>\n");
            rpc.append("<msea-soam-pm:dm-id>");
            rpc.append(dmId.id());
            rpc.append("</msea-soam-pm:dm-id>\n");
            rpc.append("<msea-soam-pm:mep-id/>");
            rpc.append("<msea-soam-pm:mac-address/>");
            rpc.append("<msea-soam-pm:administrative-state/>\n");
            rpc.append("<msea-soam-pm:measurement-enable/>\n");
            rpc.append("<msea-soam-pm:message-period/>\n");
            rpc.append("<msea-soam-pm:priority/>\n");
            rpc.append("<msea-soam-pm:frame-size/>\n");
            rpc.append("<msea-soam-pm:measurement-interval/>\n");
            rpc.append("<msea-soam-pm:number-intervals-stored/>\n");
            rpc.append("<msea-soam-pm:session-status/>\n");
            rpc.append("<msea-soam-pm:frame-delay-two-way/>\n");
            rpc.append("<msea-soam-pm:inter-frame-delay-variation-two-way/>\n");
            if (parts != null && (parts.equals(DmEntryParts.CURRENT_ONLY) ||
                    parts.equals(DmEntryParts.ALL_PARTS))) {
                rpc.append("<msea-soam-pm:current-stats/>\n");
            }
            if (parts != null && (parts.equals(DmEntryParts.HISTORY_ONLY) ||
                    parts.equals(DmEntryParts.ALL_PARTS))) {
                rpc.append("<msea-soam-pm:history-stats/>\n");
            }
            rpc.append("</msea-soam-pm:delay-measurement>\n");
            rpc.append("</msea-soam-pm:delay-measurements>");
        } else {
            rpc.append("<msea-soam-pm:delay-measurements/>");
        }
        rpc.append("</maintenance-association-end-point>\n");
        rpc.append("</maintenance-association>\n");
        rpc.append("</maintenance-domain>\n");
        rpc.append("</mef-cfm>");

        return rpc.toString();
    }

    @Deprecated //Replace this with a ModelObject defintion
    private String buildAbortLoopbackQueryString(Short mdId, Short maId,
            Short mepId) {
        StringBuilder rpc = new StringBuilder();

        rpc.append("<abort-loopback xmlns=\"http://www.microsemi.com/microsemi-edge-assure/msea-cfm\">");
        rpc.append("<maintenance-domain>");
        rpc.append(mdId);
        rpc.append("</maintenance-domain>");
        rpc.append("<maintenance-association>");
        rpc.append(maId);
        rpc.append("</maintenance-association>");
        rpc.append("<maintenance-association-end-point>");
        rpc.append(mepId);
        rpc.append("</maintenance-association-end-point>");
        rpc.append("</abort-loopback>");

        return rpc.toString();
    }
}
