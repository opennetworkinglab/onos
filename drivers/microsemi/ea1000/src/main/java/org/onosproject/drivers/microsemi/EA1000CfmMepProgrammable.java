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
package org.onosproject.drivers.microsemi;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onosproject.drivers.microsemi.yang.utils.MaNameUtil.getApiMaIdFromYangMaName;
import static org.onosproject.drivers.microsemi.yang.utils.MdNameUtil.getApiMdIdFromYangMdName;
import static org.onosproject.drivers.microsemi.yang.utils.MdNameUtil.getYangMdNameFromApiMdId;
import static org.slf4j.LoggerFactory.getLogger;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.NoSuchElementException;
import java.util.Optional;

import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
import org.onlab.util.HexString;
import org.onosproject.drivers.microsemi.yang.MseaCfmNetconfService;
import org.onosproject.drivers.microsemi.yang.utils.MaNameUtil;
import org.onosproject.incubator.net.l2monitoring.cfm.Component;
import org.onosproject.incubator.net.l2monitoring.cfm.DefaultMepEntry;
import org.onosproject.incubator.net.l2monitoring.cfm.DefaultMepLbEntry;
import org.onosproject.incubator.net.l2monitoring.cfm.DefaultRemoteMepEntry;
import org.onosproject.incubator.net.l2monitoring.cfm.MaintenanceAssociation;
import org.onosproject.incubator.net.l2monitoring.cfm.MaintenanceDomain;
import org.onosproject.incubator.net.l2monitoring.cfm.Mep;
import org.onosproject.incubator.net.l2monitoring.cfm.Mep.MepDirection;
import org.onosproject.incubator.net.l2monitoring.cfm.Mep.Priority;
import org.onosproject.incubator.net.l2monitoring.cfm.MepEntry;
import org.onosproject.incubator.net.l2monitoring.cfm.MepLbCreate;
import org.onosproject.incubator.net.l2monitoring.cfm.MepLbEntry.MepLbEntryBuilder;
import org.onosproject.incubator.net.l2monitoring.cfm.MepLtCreate;
import org.onosproject.incubator.net.l2monitoring.cfm.RemoteMepEntry;
import org.onosproject.incubator.net.l2monitoring.cfm.RemoteMepEntry.InterfaceStatusTlvType;
import org.onosproject.incubator.net.l2monitoring.cfm.RemoteMepEntry.PortStatusTlvType;
import org.onosproject.incubator.net.l2monitoring.cfm.RemoteMepEntry.RemoteMepEntryBuilder;
import org.onosproject.incubator.net.l2monitoring.cfm.RemoteMepEntry.RemoteMepState;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MaIdShort;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MdId;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MepId;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MepKeyId;
import org.onosproject.incubator.net.l2monitoring.cfm.service.CfmConfigException;
import org.onosproject.incubator.net.l2monitoring.cfm.service.CfmMdService;
import org.onosproject.incubator.net.l2monitoring.cfm.service.CfmMepProgrammable;
import org.onosproject.incubator.net.l2monitoring.cfm.service.CfmMepService;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.driver.AbstractHandlerBehaviour;
import org.onosproject.netconf.DatastoreId;
import org.onosproject.netconf.NetconfController;
import org.onosproject.netconf.NetconfException;
import org.onosproject.netconf.NetconfSession;
import org.onosproject.yang.gen.v1.mseacfm.rev20160229.MseaCfm;
import org.onosproject.yang.gen.v1.mseacfm.rev20160229.MseaCfmOpParam;
import org.onosproject.yang.gen.v1.mseacfm.rev20160229.mseacfm.DefaultMefCfm;
import org.onosproject.yang.gen.v1.mseacfm.rev20160229.mseacfm.MefCfm;
import org.onosproject.yang.gen.v1.mseacfm.rev20160229.mseacfm.abortloopback.AbortLoopbackInput;
import org.onosproject.yang.gen.v1.mseacfm.rev20160229.mseacfm.abortloopback.DefaultAbortLoopbackInput;
import org.onosproject.yang.gen.v1.mseacfm.rev20160229.mseacfm.faultalarmdefectbitstype.Bits;
import org.onosproject.yang.gen.v1.mseacfm.rev20160229.mseacfm.mefcfm.DefaultMaintenanceDomain;
import org.onosproject.yang.gen.v1.mseacfm.rev20160229.mseacfm.mefcfm.maintenancedomain.DefaultMaintenanceAssociation;
import org.onosproject.yang.gen.v1.mseacfm.rev20160229.mseacfm.mefcfm.maintenancedomain.MdNameAndTypeCombo;
import org.onosproject.yang.gen.v1.mseacfm.rev20160229.mseacfm.mefcfm.maintenancedomain.maintenanceassociation.CcmIntervalEnum;
import org.onosproject.yang.gen.v1.mseacfm.rev20160229.mseacfm.mefcfm.maintenancedomain.maintenanceassociation.ComponentList;
import org.onosproject.yang.gen.v1.mseacfm.rev20160229.mseacfm.mefcfm.maintenancedomain.maintenanceassociation.DefaultComponentList;
import org.onosproject.yang.gen.v1.mseacfm.rev20160229.mseacfm.mefcfm.maintenancedomain.maintenanceassociation.DefaultMaintenanceAssociationEndPoint;
import org.onosproject.yang.gen.v1.mseacfm.rev20160229.mseacfm.mefcfm.maintenancedomain.maintenanceassociation.MaNameAndTypeCombo;
import org.onosproject.yang.gen.v1.mseacfm.rev20160229.mseacfm.mefcfm.maintenancedomain.maintenanceassociation.MaintenanceAssociationEndPoint;
import org.onosproject.yang.gen.v1.mseacfm.rev20160229.mseacfm.mefcfm.maintenancedomain.maintenanceassociation.componentlist.TagTypeEnum;
import org.onosproject.yang.gen.v1.mseacfm.rev20160229.mseacfm.mefcfm.maintenancedomain.maintenanceassociation.maintenanceassociationendpoint.ContinuityCheck;
import org.onosproject.yang.gen.v1.mseacfm.rev20160229.mseacfm.mefcfm.maintenancedomain.maintenanceassociation.maintenanceassociationendpoint.DefaultContinuityCheck;
import org.onosproject.yang.gen.v1.mseacfm.rev20160229.mseacfm.mefcfm.maintenancedomain.maintenanceassociation.maintenanceassociationendpoint.InterfaceEnum;
import org.onosproject.yang.gen.v1.mseacfm.rev20160229.mseacfm.remotemepstatetype.RemoteMepStateTypeEnum;
import org.onosproject.yang.gen.v1.mseacfm.rev20160229.mseacfm.targetaddressgroup.addresstype.DefaultMacAddress;
import org.onosproject.yang.gen.v1.mseacfm.rev20160229.mseacfm.targetaddressgroup.addresstype.DefaultMepId;
import org.onosproject.yang.gen.v1.mseacfm.rev20160229.mseacfm.transmitloopback.DefaultTransmitLoopbackInput;
import org.onosproject.yang.gen.v1.mseacfm.rev20160229.mseacfm.transmitloopback.TransmitLoopbackInput;
import org.onosproject.yang.gen.v1.mseacfm.rev20160229.mseacfm.transmitloopback.transmitloopbackinput.DefaultTargetAddress;
import org.onosproject.yang.gen.v1.mseacfm.rev20160229.mseacfm.transmitloopback.transmitloopbackinput.TargetAddress;
import org.onosproject.yang.gen.v1.mseasoamfm.rev20160229.mseasoamfm.mefcfm.maintenancedomain.maintenanceassociation.maintenanceassociationendpoint.AugmentedMseaCfmMaintenanceAssociationEndPoint;
import org.onosproject.yang.gen.v1.mseasoamfm.rev20160229.mseasoamfm.mefcfm.maintenancedomain.maintenanceassociation.maintenanceassociationendpoint.DefaultAugmentedMseaCfmMaintenanceAssociationEndPoint;
import org.onosproject.yang.gen.v1.mseatypes.rev20160229.mseatypes.MdLevelType;
import org.onosproject.yang.gen.v1.mseatypes.rev20160229.mseatypes.MepIdType;
import org.onosproject.yang.gen.v1.mseatypes.rev20160229.mseatypes.PriorityType;
import org.onosproject.yang.gen.v1.mseatypes.rev20160229.mseatypes.VlanIdType;
import org.slf4j.Logger;

/**
 * Implementation of CfmMepProgrammable for Microsemi EA1000.
 */
public class EA1000CfmMepProgrammable extends AbstractHandlerBehaviour
    implements CfmMepProgrammable {

    private static final int NUMERIC_ID_MAX = 64;
    private static final int COMPONENT_LIST_SIZE = 1;
    private static final int VIDLIST_SIZE_MIN = 1;
    private static final int MEP_PORT_MIN = 0;
    private static final int MEP_PORT_MAX = 1;
    public static final String MUST_1_64_MSG = " must be between 1 and 64 inclusive for EA1000";
    private final Logger log = getLogger(getClass());

    /**
     * Creates the instance of the EA1000CfmMepProgrammable.
     */
    public EA1000CfmMepProgrammable() {
        log.debug("Loaded handler behaviour EA1000CfmMepProgrammable");
    }

    @Override
    public boolean createMep(MdId mdName, MaIdShort maName, Mep mep)
            throws CfmConfigException {
        NetconfController controller = checkNotNull(handler().get(NetconfController.class));
        NetconfSession session = checkNotNull(controller.getDevicesMap()
                                .get(handler().data().deviceId()).getSession());
        MseaCfmNetconfService mseaCfmService =
                checkNotNull(handler().get(MseaCfmNetconfService.class));
        CfmMepService cfmMepService =
                checkNotNull(handler().get(CfmMepService.class));

        MaintenanceAssociationEndPoint yangMep = buildYangMepFromApiMep(mep);

        CfmMdService cfmMdService = checkNotNull(handler().get(CfmMdService.class));
        MseaCfmOpParam mseaCfmOpParam = getMaYangObject(cfmMdService, mdName, maName);

        mseaCfmOpParam.mefCfm().maintenanceDomain().get(0)
                .maintenanceAssociation().get(0).addToMaintenanceAssociationEndPoint(yangMep);
        //Add this mepId to the list of remoteMeps on the device
        mseaCfmOpParam.mefCfm().maintenanceDomain().get(0)
                .maintenanceAssociation().get(0).addToRemoteMeps(MepIdType.of(mep.mepId().value()));

        //Add all of the existing meps on this MD/MA to the remote meps list
        cfmMepService.getAllMeps(mdName, maName).forEach(m -> mseaCfmOpParam.mefCfm()
                .maintenanceDomain().get(0).maintenanceAssociation().get(0)
                .addToRemoteMeps(MepIdType.of(m.mepId().value())));
        try {
            mseaCfmService.setMseaCfm(mseaCfmOpParam, session, DatastoreId.RUNNING);
            log.info("Created MEP {} on device {}", mdName + "/" + maName +
                    "/" + mep.mepId(), handler().data().deviceId());

            return true;
        } catch (NetconfException e) {
            log.error("Unable to create MEP {}/{}/{} on device {}",
                    mdName, maName, mep.mepId(), handler().data().deviceId(), e);
            throw new CfmConfigException("Unable to create MEP :" + e.getMessage(), e);
        }
    }

    @Override
    public MepEntry getMep(MdId mdName, MaIdShort maName, MepId mepId)
            throws CfmConfigException {
        NetconfController controller = checkNotNull(handler().get(NetconfController.class));
        if (handler().data().deviceId() == null) {
            throw new CfmConfigException("Device is not ready - connecting or "
                    + "disconnected for MEP " + mdName + "/" + maName + "/" + mepId);
        }
        NetconfSession session = checkNotNull(controller.getDevicesMap()
                                .get(handler().data().deviceId()).getSession());
        MseaCfmNetconfService mseaCfmService = checkNotNull(handler().get(MseaCfmNetconfService.class));

        try {
            MseaCfm mseacfm =
                    mseaCfmService.getMepFull(mdName, maName, mepId, session);
            Collection<MepEntry> mepEntries = getMepEntriesFromYangResponse(mseacfm);
            if (mepEntries == null || mepEntries.size() != 1) {
                log.warn("Mep " + mepId + " not found on device " + handler().data().deviceId());
                return null;
            } else {
                return mepEntries.stream().findFirst().get();
            }
        } catch (NetconfException e) {
            log.error("Unable to get MEP {}/{}/{} on device {}",
                    mdName, maName, mepId, handler().data().deviceId(), e);
            throw new CfmConfigException("Unable to get MEP :" + e.getMessage(), e);
        }
    }

    private Collection<MepEntry> getMepEntriesFromYangResponse(MseaCfm mseacfm)
            throws CfmConfigException {

        Collection<MepEntry> mepEntries = new ArrayList<>();
        if (mseacfm == null || mseacfm.mefCfm() == null || mseacfm.mefCfm().maintenanceDomain() == null) {
            return mepEntries;
        }

        for (org.onosproject.yang.gen.v1.mseacfm.rev20160229.
                mseacfm.mefcfm.MaintenanceDomain replyMd:mseacfm.mefCfm().maintenanceDomain()) {
            for (org.onosproject.yang.gen.v1.mseacfm.rev20160229.
                    mseacfm.mefcfm.maintenancedomain.
                    MaintenanceAssociation replyMa:replyMd.maintenanceAssociation()) {
                for (MaintenanceAssociationEndPoint replyMep:replyMa.maintenanceAssociationEndPoint()) {
                    mepEntries.add(buildApiMepEntryFromYangMep(
                        replyMep, handler().data().deviceId(), replyMd, replyMa));
                }
            }
        }
        return mepEntries;
    }

    @Override
    public boolean deleteMep(MdId mdName, MaIdShort maName, MepId mepId,
                    Optional<MaintenanceDomain> oldMd) throws CfmConfigException {

        NetconfController controller = checkNotNull(handler().get(NetconfController.class));
        NetconfSession session = checkNotNull(controller.getDevicesMap()
                .get(handler().data().deviceId()).getSession());
        MseaCfmNetconfService mseaCfmService = checkNotNull(handler().get(MseaCfmNetconfService.class));
        CfmMdService mdService = checkNotNull(handler().get(CfmMdService.class));

        MaintenanceAssociationEndPoint mep =
                new DefaultMaintenanceAssociationEndPoint();
        mep.mepIdentifier(MepIdType.of(mepId.id()));

        org.onosproject.yang.gen.v1.mseacfm.rev20160229.mseacfm.mefcfm.maintenancedomain
            .MaintenanceAssociation yangMa = new DefaultMaintenanceAssociation();
        Short maNumericId = null;
        try {
            maNumericId =
                    mdService.getMaintenanceAssociation(mdName, maName).get().maNumericId();
            yangMa.id(maNumericId);
        } catch (NoSuchElementException | IllegalArgumentException e) {
            //The MA and/or MD have probably been deleted
            // try to get numeric id values from oldMd
            log.debug("Could not get MD/MA details from MD service during deletion of MEP {}." +
                    "Continuing with values from event", new MepKeyId(mdName, maName, mepId), e);
            yangMa.id(getMaNumericId(oldMd.get(), maName));
        }

        yangMa.addToMaintenanceAssociationEndPoint(mep);

        org.onosproject.yang.gen.v1.mseacfm.rev20160229.mseacfm.mefcfm.MaintenanceDomain yangMd =
            new DefaultMaintenanceDomain();
        Short mdNumericId = null;
        try {
            mdNumericId = mdService.getMaintenanceDomain(mdName).get().mdNumericId();
            yangMd.id(mdNumericId);
        } catch (NoSuchElementException | IllegalArgumentException e) {
            //The MD has probably been deleted
            // try to get numeric id values from oldMd
            log.debug("Could not get MD details from MD service during deletion of MEP {}." +
                    "Continuing with values from event", new MepKeyId(mdName, maName, mepId), e);
            yangMd.id(oldMd.get().mdNumericId());
        }
        yangMd.addToMaintenanceAssociation(yangMa);

        MefCfm mefCfm = new DefaultMefCfm();
        mefCfm.addToMaintenanceDomain(yangMd);

        MseaCfmOpParam mseaCfmOpParam = new MseaCfmOpParam();
        mseaCfmOpParam.mefCfm(mefCfm);

        try {
            mseaCfmService.deleteMseaMep(mseaCfmOpParam, session, DatastoreId.RUNNING);
            log.info("Deleted MEP {} on device {}", mdName + "/" + maName +
                    "/" + mepId, handler().data().deviceId());
            return true;
        } catch (NetconfException e) {
            log.error("Unable to delete MEP {} ({}) on device {}",
                    mdName + "/" + maName + "/" + mepId,
                    mdNumericId + "/" + maNumericId, handler().data().deviceId(), e);
            throw new CfmConfigException("Unable to delete MEP :" + e.getMessage());
        }

    }

    @Override
    public boolean createMdOnDevice(MdId mdId) throws CfmConfigException {
        NetconfController controller =
                checkNotNull(handler().get(NetconfController.class));
        NetconfSession session = checkNotNull(controller.getDevicesMap()
                .get(handler().data().deviceId()).getSession());

        CfmMdService cfmMdService = checkNotNull(handler().get(CfmMdService.class));
        MaintenanceDomain md = cfmMdService.getMaintenanceDomain(mdId).get();

        org.onosproject.yang.gen.v1.mseacfm.rev20160229.mseacfm
                .mefcfm.MaintenanceDomain yangMd = buildYangMdFromApiMd(md);

        if (md.mdNumericId() <= 0 || md.mdNumericId() > NUMERIC_ID_MAX) {
            throw new CfmConfigException("Numeric id of MD " + mdId + MUST_1_64_MSG);
        }

        for (MaintenanceAssociation ma:md.maintenanceAssociationList()) {
            if (ma.maNumericId() <= 0 || ma.maNumericId() > NUMERIC_ID_MAX) {
                throw new CfmConfigException("Numeric id of MA " + mdId + MUST_1_64_MSG);
            }
            org.onosproject.yang.gen.v1.mseacfm.rev20160229.mseacfm.mefcfm.maintenancedomain
                    .MaintenanceAssociation yangMa = buildYangMaFromApiMa(ma);
            yangMd.addToMaintenanceAssociation(yangMa);
        }

        MefCfm mefCfm = new DefaultMefCfm();
        mefCfm.addToMaintenanceDomain(yangMd);

        MseaCfmOpParam mseaCfmOpParam = new MseaCfmOpParam();
        mseaCfmOpParam.mefCfm(mefCfm);

        MseaCfmNetconfService mseaCfmService =
                checkNotNull(handler().get(MseaCfmNetconfService.class));

        try {
            boolean created = mseaCfmService.setMseaCfm(mseaCfmOpParam, session, DatastoreId.RUNNING);
            log.info("Created MD {} on device {}", mdId.mdName(),
                    handler().data().deviceId());
            return created;
        } catch (NetconfException e) {
            log.error("Unable to create MD {} on device {}",
                    mdId.mdName(), handler().data().deviceId(), e);
            throw new CfmConfigException("Unable to create MD :" + e.getMessage(), e);
        }
    }

    @Override
    public boolean createMaOnDevice(MdId mdId, MaIdShort maId) throws CfmConfigException {
        NetconfController controller =
                checkNotNull(handler().get(NetconfController.class));
        NetconfSession session = checkNotNull(controller.getDevicesMap()
                .get(handler().data().deviceId()).getSession());

        CfmMdService mdService = checkNotNull(handler().get(CfmMdService.class));
        MseaCfmOpParam mseaCfmOpParam = getMaYangObject(mdService, mdId, maId);
        MseaCfmNetconfService mseaCfmService =
                checkNotNull(handler().get(MseaCfmNetconfService.class));

        try {
            boolean created = mseaCfmService.setMseaCfm(mseaCfmOpParam, session, DatastoreId.RUNNING);
            log.info("Created MA {} on device {}", mdId.mdName() + "/" + maId.maName(),
                    handler().data().deviceId());
            return created;
        } catch (NetconfException e) {
            log.error("Unable to create MA {} on device {}",
                    mdId.mdName() + "/" + maId.maName(), handler().data().deviceId(), e);
            throw new CfmConfigException("Unable to create MA :" + e.getMessage(), e);
        }
    }

    private static MseaCfmOpParam getMaYangObject(CfmMdService cfmMdService,
                        MdId mdName, MaIdShort maName) throws CfmConfigException {
        MaintenanceDomain md = cfmMdService.getMaintenanceDomain(mdName).get();
        MaintenanceAssociation ma = cfmMdService.getMaintenanceAssociation(mdName, maName).get();

        org.onosproject.yang.gen.v1.mseacfm.rev20160229.mseacfm.mefcfm.maintenancedomain
                .MaintenanceAssociation yangMa = buildYangMaFromApiMa(ma);

        org.onosproject.yang.gen.v1.mseacfm.rev20160229.mseacfm
                .mefcfm.MaintenanceDomain yangMd = buildYangMdFromApiMd(md);
        yangMd.addToMaintenanceAssociation(yangMa);

        if (md.mdNumericId() <= 0 || md.mdNumericId() > NUMERIC_ID_MAX) {
            throw new CfmConfigException("Numeric id of MD " + mdName + MUST_1_64_MSG);
        } else if (ma.maNumericId() <= 0 || ma.maNumericId() > NUMERIC_ID_MAX) {
            throw new CfmConfigException("Numeric id of MA " + maName + MUST_1_64_MSG);
        }

        MefCfm mefCfm = new DefaultMefCfm();
        mefCfm.addToMaintenanceDomain(yangMd);

        MseaCfmOpParam mseaCfmOpParam = new MseaCfmOpParam();
        mseaCfmOpParam.mefCfm(mefCfm);

        return mseaCfmOpParam;
    }

    @Override
    public boolean deleteMdOnDevice(MdId mdId, Optional<MaintenanceDomain> oldMd)
            throws CfmConfigException {
        NetconfController controller =
                checkNotNull(handler().get(NetconfController.class));
        NetconfSession session = controller.getDevicesMap()
                .get(handler().data().deviceId()).getSession();

        //First check if this MD is known to ONOS if it is does it have MAs and
        // do they have any Meps known to ONOS. If there are Meps throw an exception -
        // the Meps should have been deleted first
        //If there are none known to ONOS we do not check for Meps on the actual device
        // - there might might be some orphaned ones down there - we want to delete these
        CfmMdService mdService = checkNotNull(handler().get(CfmMdService.class));
        MseaCfmNetconfService mseaCfmService =
                checkNotNull(handler().get(MseaCfmNetconfService.class));

        MdNameAndTypeCombo mdName = getYangMdNameFromApiMdId(mdId);
        org.onosproject.yang.gen.v1.mseacfm.rev20160229.mseacfm.mefcfm.MaintenanceDomain yangMd =
                new DefaultMaintenanceDomain();
        Short mdNumericId = null;
        try {
            mdNumericId = mdService.getMaintenanceDomain(mdId).get().mdNumericId();
            yangMd.id(mdNumericId);
        } catch (NoSuchElementException e) {
            log.debug("Cannot get numericId of MD from service - getting from oldValue", e);
            yangMd.id(oldMd.get().mdNumericId());
        }

        MefCfm mefCfm = new DefaultMefCfm();
        mefCfm.addToMaintenanceDomain(yangMd);

        MseaCfmOpParam mseaCfmOpParam = new MseaCfmOpParam();
        mseaCfmOpParam.mefCfm(mefCfm);

        try {
            boolean deleted = mseaCfmService.deleteMseaMd(mseaCfmOpParam, session, DatastoreId.RUNNING);
            log.info("Deleted MD {} on device {}", mdName, handler().data().deviceId());
            return deleted;
        } catch (NetconfException e) {
            log.error("Unable to delete MD {} ({}) on device {}",
                    mdName, mdNumericId, handler().data().deviceId(), e);
            throw new CfmConfigException("Unable to delete MD :" + e.getMessage(), e);
        }

    }

    @Override
    public boolean deleteMaOnDevice(MdId mdId, MaIdShort maId, Optional<MaintenanceDomain> oldMd)
            throws CfmConfigException {
        NetconfController controller =
                checkNotNull(handler().get(NetconfController.class));
        NetconfSession session = controller.getDevicesMap()
                .get(handler().data().deviceId()).getSession();

        CfmMdService mdService = checkNotNull(handler().get(CfmMdService.class));

        MseaCfmNetconfService mseaCfmService =
                checkNotNull(handler().get(MseaCfmNetconfService.class));

        org.onosproject.yang.gen.v1.mseacfm.rev20160229.mseacfm.mefcfm.maintenancedomain
                .MaintenanceAssociation yangMa = new DefaultMaintenanceAssociation();
        Short maNumericId = null;
        try {
            maNumericId =
                    mdService.getMaintenanceAssociation(mdId, maId).get().maNumericId();
            yangMa.id(maNumericId);
        } catch (NoSuchElementException e) {
            log.debug("Cannot get numericId of MA from service - getting from oldValue", e);
            yangMa.id(getMaNumericId(oldMd.get(), maId));
        }

        org.onosproject.yang.gen.v1.mseacfm.rev20160229.mseacfm.mefcfm.MaintenanceDomain yangMd =
                new DefaultMaintenanceDomain();
        Short mdNumericId = null;
        try {
            mdNumericId = mdService.getMaintenanceDomain(mdId).get().mdNumericId();
            yangMd.id(mdNumericId);
        } catch (NoSuchElementException e) {
            log.debug("Cannot get numericId of MD from service - getting from oldValue", e);
            yangMd.id(oldMd.get().mdNumericId());
        }
        yangMd.addToMaintenanceAssociation(yangMa);

        MefCfm mefCfm = new DefaultMefCfm();
        mefCfm.addToMaintenanceDomain(yangMd);

        MseaCfmOpParam mseaCfmOpParam = new MseaCfmOpParam();
        mseaCfmOpParam.mefCfm(mefCfm);

        try {
            boolean deleted = mseaCfmService.deleteMseaMa(mseaCfmOpParam, session, DatastoreId.RUNNING);
            log.info("Deleted MA {} ({})on device {}", mdId.mdName() + "/" + maId.maName(),
                    mdNumericId + "/" + maNumericId, handler().data().deviceId());
            return deleted;
        } catch (NetconfException e) {
            log.error("Unable to delete MA {} ({}) on device {}",
                    mdId.mdName() + "/" + maId.maName(),
                    mdNumericId + "/" + maNumericId, handler().data().deviceId(), e);
            throw new CfmConfigException("Unable to delete MA :" + e.getMessage(), e);
        }
    }

    @Override
    public boolean createMaRemoteMepOnDevice(MdId mdId, MaIdShort maId, MepId remoteMep) throws CfmConfigException {
        return crDelMaRemoteMep(mdId, maId, remoteMep, true);
    }

    @Override
    public boolean deleteMaRemoteMepOnDevice(MdId mdId, MaIdShort maId, MepId remoteMep) throws CfmConfigException {
        return crDelMaRemoteMep(mdId, maId, remoteMep, false);
    }

    private boolean crDelMaRemoteMep(MdId mdId, MaIdShort maId, MepId remoteMep,
                                     boolean isCreate) throws CfmConfigException {
        NetconfController controller =
                checkNotNull(handler().get(NetconfController.class));
        NetconfSession session = controller.getDevicesMap()
                .get(handler().data().deviceId()).getSession();

        CfmMdService mdService = checkNotNull(handler().get(CfmMdService.class));

        Short mdNumericId = mdService.getMaintenanceDomain(mdId).get().mdNumericId();
        Short maNumericId =
                mdService.getMaintenanceAssociation(mdId, maId).get().maNumericId();

        MseaCfmNetconfService mseaCfmService =
                checkNotNull(handler().get(MseaCfmNetconfService.class));

        org.onosproject.yang.gen.v1.mseacfm.rev20160229.mseacfm.mefcfm.maintenancedomain
                .MaintenanceAssociation yangMa = new DefaultMaintenanceAssociation();
        yangMa.id(maNumericId);
        yangMa.addToRemoteMeps(MepIdType.of(remoteMep.value()));

        org.onosproject.yang.gen.v1.mseacfm.rev20160229.mseacfm.mefcfm.MaintenanceDomain yangMd =
                new DefaultMaintenanceDomain();
        yangMd.id(mdNumericId);
        yangMd.addToMaintenanceAssociation(yangMa);

        MefCfm mefCfm = new DefaultMefCfm();
        mefCfm.addToMaintenanceDomain(yangMd);

        MseaCfmOpParam mseaCfmOpParam = new MseaCfmOpParam();
        mseaCfmOpParam.mefCfm(mefCfm);

        try {
            boolean result;
            if (isCreate) {
                result = mseaCfmService.setMseaCfm(mseaCfmOpParam, session, DatastoreId.RUNNING);
            } else {
                result = mseaCfmService.deleteMseaMaRMep(mseaCfmOpParam, session, DatastoreId.RUNNING);
            }
            log.info("{} Remote MEP {} in MA {} on device {}", isCreate ? "Created" : "Deleted",
                    remoteMep, mdId.mdName() + "/" + maId.maName(), handler().data().deviceId());
            return result;
        } catch (NetconfException e) {
            log.error("Unable to {} RemoteMep {} in MA {} on device {}",
                    isCreate ? "create" : "delete", remoteMep, mdId.mdName() + "/" + maId.maName(),
                    handler().data().deviceId(), e);
            throw new CfmConfigException("Unable to " + (isCreate ? "create" : "delete")
                    + " Remote Mep:" + e.getMessage(), e);
        }
    }


    @Override
    public void transmitLoopback(MdId mdName, MaIdShort maName, MepId mepId,
            MepLbCreate lbCreate) throws CfmConfigException {
        NetconfController controller =
                checkNotNull(handler().get(NetconfController.class));
        NetconfSession session = controller.getDevicesMap()
                .get(handler().data().deviceId()).getSession();
        MseaCfmNetconfService mseaCfmService =
                checkNotNull(handler().get(MseaCfmNetconfService.class));
        CfmMdService mdService = checkNotNull(handler().get(CfmMdService.class));

        Short mdNumericId = mdService.getMaintenanceDomain(mdName).get().mdNumericId();
        Short maNumericId =
                mdService.getMaintenanceAssociation(mdName, maName).get().maNumericId();

        TransmitLoopbackInput lb = new DefaultTransmitLoopbackInput();
        lb.maintenanceDomain(mdNumericId);
        lb.maintenanceAssociation(maNumericId);
        lb.maintenanceAssociationEndPoint(mepId.id());
        if (lbCreate.numberMessages() != null) {
            lb.numberOfMessages(lbCreate.numberMessages());
        }
        if (lbCreate.vlanDropEligible() != null) {
            lb.vlanDropEligible(lbCreate.vlanDropEligible());
        }

        if (lbCreate.remoteMepId() != null) {
            org.onosproject.yang.gen.v1.mseacfm.rev20160229.mseacfm.targetaddressgroup.addresstype
            .MepId yangMepId = new DefaultMepId();
            yangMepId.mepId(MepIdType.of(lbCreate.remoteMepId().id()));
            TargetAddress ta = new DefaultTargetAddress();
            ta.addressType(yangMepId);
            lb.targetAddress(ta);
        } else if (lbCreate.remoteMepAddress() != null) {
            org.onosproject.yang.gen.v1.mseacfm.rev20160229.mseacfm.targetaddressgroup.addresstype
            .MacAddress yangMacAddress = new DefaultMacAddress();
            yangMacAddress.macAddress(
                    org.onosproject.yang.gen.v1.ietfyangtypes.rev20130715.ietfyangtypes
                    .MacAddress.of(lbCreate.remoteMepAddress().toString()));
            TargetAddress ta = new DefaultTargetAddress();
            ta.addressType(yangMacAddress);
            lb.targetAddress(ta);
        } else {
            throw new CfmConfigException("Either a remote MEP ID or Remote MEP "
                    + "MacAddress must be specified when calling Transmit Loopback");
        }

        if (lbCreate.dataTlvHex() != null && !lbCreate.dataTlvHex().isEmpty()) {
            lb.dataTlv(HexString.fromHexString(lbCreate.dataTlvHex()));
        }
        if (lbCreate.vlanPriority() != null) {
            lb.vlanPriority(PriorityType.of((short) lbCreate.vlanPriority().ordinal()));
        }

        try {
            mseaCfmService.transmitLoopback(lb, session);
            log.info("Transmit Loopback called on MEP {} on device {}",
                    mdName + "/" + maName + "/" + mepId,
                    handler().data().deviceId());
        } catch (NetconfException e) {
            throw new CfmConfigException(e);
        }
    }

    @Override
    public void abortLoopback(MdId mdName, MaIdShort maName, MepId mepId)
            throws CfmConfigException {
        NetconfController controller =
                checkNotNull(handler().get(NetconfController.class));
        NetconfSession session = controller.getDevicesMap()
                .get(handler().data().deviceId()).getSession();
        MseaCfmNetconfService mseaCfmService =
                checkNotNull(handler().get(MseaCfmNetconfService.class));
        CfmMdService mdService = checkNotNull(handler().get(CfmMdService.class));

        Short mdNumericId = mdService.getMaintenanceDomain(mdName).get().mdNumericId();
        Short maNumericId =
                mdService.getMaintenanceAssociation(mdName, maName).get().maNumericId();

        AbortLoopbackInput lbAbort = new DefaultAbortLoopbackInput();
        lbAbort.maintenanceDomain(mdNumericId);
        lbAbort.maintenanceAssociation(maNumericId);
        lbAbort.maintenanceAssociationEndPoint(mepId.id());

        try {
            mseaCfmService.abortLoopback(lbAbort, session);
            log.info("Loopback on MEP {} on device {} aborted",
                    mdName + "/" + maName + "/" + mepId,
                    handler().data().deviceId());
        } catch (NetconfException e) {
            throw new CfmConfigException(e);
        }
    }

    @Override
    public void transmitLinktrace(MdId mdName, MaIdShort maName, MepId mepId,
            MepLtCreate ltCreate) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    private static org.onosproject.yang.gen.v1.mseacfm.rev20160229.mseacfm.mefcfm
            .MaintenanceDomain buildYangMdFromApiMd(MaintenanceDomain md)
            throws CfmConfigException {
        MdNameAndTypeCombo mdName = getYangMdNameFromApiMdId(md.mdId());

        org.onosproject.yang.gen.v1.mseacfm.rev20160229.mseacfm.mefcfm
                .MaintenanceDomain mdYang = new DefaultMaintenanceDomain();
        mdYang.id(md.mdNumericId());
        mdYang.mdNameAndTypeCombo(mdName);
        mdYang.mdLevel(MdLevelType.of((short) md.mdLevel().ordinal()));

        return mdYang;
    }

    private static org.onosproject.yang.gen.v1.mseacfm.rev20160229.mseacfm.mefcfm
            .maintenancedomain.MaintenanceAssociation buildYangMaFromApiMa(
                        MaintenanceAssociation apiMa) throws CfmConfigException {

        MaNameAndTypeCombo maName = MaNameUtil.getYangMaNameFromApiMaId(apiMa.maId());

        org.onosproject.yang.gen.v1.mseacfm.rev20160229.mseacfm.mefcfm.maintenancedomain
                .MaintenanceAssociation yamgMa = new DefaultMaintenanceAssociation();
        yamgMa.maNameAndTypeCombo(maName);

        for (MepId rmep:apiMa.remoteMepIdList()) {
            yamgMa.addToRemoteMeps(MepIdType.of(rmep.id()));
        }

        if (apiMa.ccmInterval() != null) {
            yamgMa.ccmInterval(getYangCcmIntervalFromApi(apiMa.ccmInterval()));
        }

        if (apiMa.componentList() == null || apiMa.componentList().size() != COMPONENT_LIST_SIZE) {
            throw new CfmConfigException("EA1000 supports only 1 Component in an MA");
        }

        Component maComponent = apiMa.componentList().iterator().next();
        if (maComponent.vidList() == null || maComponent.vidList().size() < VIDLIST_SIZE_MIN) {
            throw new CfmConfigException("EA1000 requires at least 1 VID in "
                    + "the Component of an MA");
        }
        ComponentList compList = new DefaultComponentList();
        for (VlanId vid:maComponent.vidList()) {
            compList.addToVid(VlanIdType.of(vid.toShort()));
        }

        if (maComponent.tagType() != null) {
            switch (maComponent.tagType()) {
            case VLAN_STAG:
                compList.tagType(TagTypeEnum.VLAN_STAG);
                break;
            case VLAN_CTAG:
                compList.tagType(TagTypeEnum.VLAN_CTAG);
                break;
            case VLAN_NONE:
            default:
                compList.tagType(TagTypeEnum.VLAN_NONE);
                break;
            }
        }

        yamgMa.componentList(compList);
        yamgMa.id(apiMa.maNumericId());
        return yamgMa;
    }

    private static CcmIntervalEnum getYangCcmIntervalFromApi(
            MaintenanceAssociation.CcmInterval cci) throws CfmConfigException {
        switch (cci) {
            case INTERVAL_3MS:
                return CcmIntervalEnum.YANGAUTOPREFIX3_3MS;
            case INTERVAL_10MS:
                return CcmIntervalEnum.YANGAUTOPREFIX10MS;
            case INTERVAL_100MS:
                return CcmIntervalEnum.YANGAUTOPREFIX100MS;
            case INTERVAL_1S:
                return CcmIntervalEnum.YANGAUTOPREFIX1S;
            default:
                throw new CfmConfigException("EA1000 only supports "
                        + "3ms, 10ms, 100ms and 1s for CCM Interval. Rejecting: "
                        + cci.name());
        }
    }

    private static MaintenanceAssociationEndPoint buildYangMepFromApiMep(Mep mep)
            throws CfmConfigException {
        MaintenanceAssociationEndPoint mepBuilder =
                                    new DefaultMaintenanceAssociationEndPoint();
        mepBuilder.mepIdentifier(MepIdType.of(mep.mepId().id()));
        ContinuityCheck cc = new DefaultContinuityCheck();
        cc.cciEnabled(mep.cciEnabled());
        mepBuilder.continuityCheck(cc);
        mepBuilder.ccmLtmPriority(
                        PriorityType.of((short) mep.ccmLtmPriority().ordinal()));
        mepBuilder.administrativeState(mep.administrativeState());

        if (mep.direction() == MepDirection.UP_MEP) {
            throw new CfmConfigException("EA1000 only supports DOWN Meps");
        }

        if (mep.port() == null || mep.port().toLong() < MEP_PORT_MIN
                                    || mep.port().toLong() > MEP_PORT_MAX) {
            throw new CfmConfigException("EA1000 has only ports 0 and 1. "
                    + "Rejecting Port: " + mep.port());
        }
        mepBuilder.yangAutoPrefixInterface(
                (mep.port().toLong() == 0) ? InterfaceEnum.ETH0 : InterfaceEnum.ETH1);

        return mepBuilder;
    }

    private MepEntry buildApiMepEntryFromYangMep(
            MaintenanceAssociationEndPoint yangMep, DeviceId deviceId,
            org.onosproject.yang.gen.v1.mseacfm.rev20160229.
                    mseacfm.mefcfm.MaintenanceDomain replyMd,
            org.onosproject.yang.gen.v1.mseacfm.rev20160229.
                    mseacfm.mefcfm.maintenancedomain.MaintenanceAssociation replyMa)
            throws CfmConfigException {
        MepId mepId = MepId.valueOf((short) yangMep.mepIdentifier().uint16());
        MepEntry.MepEntryBuilder builder = DefaultMepEntry.builder(mepId,
                deviceId,
                (yangMep.yangAutoPrefixInterface() == InterfaceEnum.ETH0) ?
                        PortNumber.portNumber(0) : PortNumber.portNumber(1),
                MepDirection.DOWN_MEP, //Always down for EA1000
                getApiMdIdFromYangMdName(replyMd.mdNameAndTypeCombo()),
                getApiMaIdFromYangMaName(replyMa.maNameAndTypeCombo()));

        if (yangMep.loopback() != null) {
            MepLbEntryBuilder lbEntryBuilder = DefaultMepLbEntry.builder();
            if (yangMep.loopback().repliesReceived() != null) {
                lbEntryBuilder = lbEntryBuilder.countLbrReceived(
                        yangMep.loopback().repliesReceived().uint32());
            }
            if (yangMep.loopback().repliesTransmitted() != null) {
                lbEntryBuilder = lbEntryBuilder.countLbrTransmitted(
                        yangMep.loopback().repliesTransmitted().uint32());
            }
            builder.loopbackAttributes(lbEntryBuilder.build());
        }

        if (yangMep.remoteMepDatabase() != null &&
                yangMep.remoteMepDatabase().remoteMep() != null) {
            for (org.onosproject.yang.gen.v1.mseacfm.rev20160229.mseacfm.mefcfm
                    .maintenancedomain.maintenanceassociation
                    .maintenanceassociationendpoint.remotemepdatabase.RemoteMep
                    rmep:yangMep.remoteMepDatabase().remoteMep()) {
                builder = builder.addToActiveRemoteMepList(
                        getApiRemoteMepFromYangRemoteMep(rmep));
            }
        }

        if (yangMep.ccmLtmPriority() != null) {
            builder = (MepEntry.MepEntryBuilder) builder.ccmLtmPriority(
                    Priority.values()[yangMep.ccmLtmPriority().uint8()]);
        }

        //And the the state attributes
        builder = (MepEntry.MepEntryBuilder) builder
                .macAddress(MacAddress.valueOf(yangMep.macAddress().toString()))
                .administrativeState(yangMep.administrativeState())
                .cciEnabled(yangMep.continuityCheck().cciEnabled());

        AugmentedMseaCfmMaintenanceAssociationEndPoint augmentedyangMep = yangMep
                .augmentation(DefaultAugmentedMseaCfmMaintenanceAssociationEndPoint.class);

        if (augmentedyangMep != null && augmentedyangMep.lastDefectSent() != null)  {
            builder = builder
                .activeXconCcmDefect(augmentedyangMep.lastDefectSent().bits()
                            .get(Bits.CROSS_CONNECT_CCM.bits()))
                .activeErrorCcmDefect(augmentedyangMep.lastDefectSent().bits()
                        .get(Bits.INVALID_CCM.bits()))
                .activeMacStatusDefect(augmentedyangMep.lastDefectSent().bits()
                        .get(Bits.REMOTE_MAC_ERROR.bits()))
                .activeRdiCcmDefect(augmentedyangMep.lastDefectSent().bits()
                        .get(Bits.REMOTE_RDI.bits()))
                .activeRemoteCcmDefect(augmentedyangMep.lastDefectSent().bits()
                        .get(Bits.REMOTE_INVALID_CCM.bits()));
        }

        return builder.buildEntry();
    }

    private RemoteMepEntry getApiRemoteMepFromYangRemoteMep(
            org.onosproject.yang.gen.v1.mseacfm.rev20160229.mseacfm.mefcfm.maintenancedomain.
            maintenanceassociation.maintenanceassociationendpoint.remotemepdatabase.
            RemoteMep yangRemoteMep) throws CfmConfigException {

        MepId remoteMepId = MepId.valueOf((short) yangRemoteMep.remoteMepId().uint16());
        RemoteMepStateTypeEnum state = RemoteMepStateTypeEnum.FAILED;
        if (yangRemoteMep.remoteMepState() != null) {
            state = yangRemoteMep.remoteMepState().enumeration();
        }
        RemoteMepEntryBuilder rmepBuilder = DefaultRemoteMepEntry.builder(
                    remoteMepId, RemoteMepState.valueOf("RMEP_" + state.name()))
                .rdi(yangRemoteMep.rdi());
        if (yangRemoteMep.macAddress() != null) {
            rmepBuilder = rmepBuilder.macAddress(
                    MacAddress.valueOf(yangRemoteMep.macAddress().toString()));
        }
        if (yangRemoteMep.failedOkTime() != null) {
            //Currently EA1000 is reporting this as 1/1000s even though yang type
            // is time ticks 1/100s - to be fixed
            rmepBuilder = rmepBuilder.failedOrOkTime(Duration.ofMillis(
                    yangRemoteMep.failedOkTime().uint32()));
        }
        if (yangRemoteMep.portStatusTlv() != null) {
            rmepBuilder = rmepBuilder.portStatusTlvType(PortStatusTlvType.valueOf(
                    "PS_" + yangRemoteMep.portStatusTlv().enumeration().name()));
        }
        if (yangRemoteMep.interfaceStatusTlv() != null) {
            rmepBuilder = rmepBuilder.interfaceStatusTlvType(InterfaceStatusTlvType.valueOf(
                    "IS_" + yangRemoteMep.interfaceStatusTlv().enumeration().name()));
        }
        return rmepBuilder.build();
    }

    private static short getMaNumericId(MaintenanceDomain md, MaIdShort maId) {
        return md.maintenanceAssociationList().stream()
                .filter(ma -> maId.equals(ma.maId()))
                .findFirst().get().maNumericId();
    }
}
