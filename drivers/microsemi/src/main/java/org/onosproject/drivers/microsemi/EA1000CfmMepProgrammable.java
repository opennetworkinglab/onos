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
import static org.slf4j.LoggerFactory.getLogger;

import java.time.Duration;
import java.util.Collection;

import org.onlab.packet.IpAddress;
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
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MdIdCharStr;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MdIdDomainName;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MdIdMacUint;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MdIdNone;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MepId;
import org.onosproject.incubator.net.l2monitoring.cfm.service.CfmConfigException;
import org.onosproject.incubator.net.l2monitoring.cfm.service.CfmMdService;
import org.onosproject.incubator.net.l2monitoring.cfm.service.CfmMepProgrammable;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.driver.AbstractHandlerBehaviour;
import org.onosproject.netconf.DatastoreId;
import org.onosproject.netconf.NetconfController;
import org.onosproject.netconf.NetconfException;
import org.onosproject.netconf.NetconfSession;
import org.onosproject.yang.gen.v1.ietfinettypes.rev20130715.ietfinettypes.DomainName;
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
import org.onosproject.yang.gen.v1.mseacfm.rev20160229.mseacfm.mefcfm.maintenancedomain.mdnameandtypecombo.DefaultMacAddressAndUint;
import org.onosproject.yang.gen.v1.mseacfm.rev20160229.mseacfm.mefcfm.maintenancedomain.mdnameandtypecombo.DefaultNameCharacterString;
import org.onosproject.yang.gen.v1.mseacfm.rev20160229.mseacfm.mefcfm.maintenancedomain.mdnameandtypecombo.DefaultNameDomainName;
import org.onosproject.yang.gen.v1.mseacfm.rev20160229.mseacfm.mefcfm.maintenancedomain.mdnameandtypecombo.DefaultNameNone;
import org.onosproject.yang.gen.v1.mseacfm.rev20160229.mseacfm.mefcfm.maintenancedomain.mdnameandtypecombo.namedomainname.NameDomainNameUnion;
import org.onosproject.yang.gen.v1.mseacfm.rev20160229.mseacfm.remotemepstatetype.RemoteMepStateTypeEnum;
import org.onosproject.yang.gen.v1.mseacfm.rev20160229.mseacfm.targetaddressgroup.addresstype.DefaultMacAddress;
import org.onosproject.yang.gen.v1.mseacfm.rev20160229.mseacfm.targetaddressgroup.addresstype.DefaultMepId;
import org.onosproject.yang.gen.v1.mseacfm.rev20160229.mseacfm.transmitloopback.DefaultTransmitLoopbackInput;
import org.onosproject.yang.gen.v1.mseacfm.rev20160229.mseacfm.transmitloopback.TransmitLoopbackInput;
import org.onosproject.yang.gen.v1.mseacfm.rev20160229.mseacfm.transmitloopback.transmitloopbackinput.DefaultTargetAddress;
import org.onosproject.yang.gen.v1.mseacfm.rev20160229.mseacfm.transmitloopback.transmitloopbackinput.TargetAddress;
import org.onosproject.yang.gen.v1.mseasoamfm.rev20160229.mseasoamfm.mefcfm.maintenancedomain.maintenanceassociation.maintenanceassociationendpoint.AugmentedMseaCfmMaintenanceAssociationEndPoint;
import org.onosproject.yang.gen.v1.mseasoamfm.rev20160229.mseasoamfm.mefcfm.maintenancedomain.maintenanceassociation.maintenanceassociationendpoint.DefaultAugmentedMseaCfmMaintenanceAssociationEndPoint;
import org.onosproject.yang.gen.v1.mseatypes.rev20160229.mseatypes.Identifier45;
import org.onosproject.yang.gen.v1.mseatypes.rev20160229.mseatypes.MacAddressAndUintStr;
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
    private static final int REMOTEMEPLIST_MIN_COUNT = 2;
    private static final int REMOTEMEPLIST_MAX_COUNT = 9;
    private static final int COMPONENT_LIST_SIZE = 1;
    private static final int VIDLIST_SIZE_MIN = 1;
    private static final int MEP_PORT_MIN = 0;
    private static final int MEP_PORT_MAX = 1;
    private final Logger log = getLogger(getClass());

    public EA1000CfmMepProgrammable() {
        log.debug("Loaded handler behaviour EA1000CfmMepProgrammable");
    }

    @Override
    public boolean createMep(MdId mdName, MaIdShort maName, Mep mep)
            throws CfmConfigException {
        NetconfController controller = checkNotNull(handler().get(NetconfController.class));
        NetconfSession session = controller.getDevicesMap()
                        .get(handler().data().deviceId()).getSession();
        MseaCfmNetconfService mseaCfmService =
                checkNotNull(handler().get(MseaCfmNetconfService.class));

        MaintenanceAssociationEndPoint yangMep = buildYangMepFromApiMep(mep);

        CfmMdService cfmMdService = checkNotNull(handler().get(CfmMdService.class));
        MaintenanceDomain md = cfmMdService.getMaintenanceDomain(mdName).get();
        MaintenanceAssociation ma = cfmMdService.getMaintenanceAssociation(mdName, maName).get();

        if (!ma.remoteMepIdList().contains(mep.mepId())) {
            throw new CfmConfigException("Mep Id " + mep.mepId() +
                    " is not present in the remote Mep list for MA " + ma.maId() +
                    ". This is required for EA1000.");
        } else if (md.mdNumericId() <= 0 || md.mdNumericId() > NUMERIC_ID_MAX) {
            throw new CfmConfigException("Numeric id of MD " + mdName + " must"
                    + " be between 1 and 64 inclusive for EA1000");
        } else if (ma.maNumericId() <= 0 || ma.maNumericId() > NUMERIC_ID_MAX) {
            throw new CfmConfigException("Numeric id of MA " + maName + " must"
                    + " be between 1 and 64 inclusive for EA1000");
        }

        org.onosproject.yang.gen.v1.mseacfm.rev20160229.mseacfm.mefcfm.maintenancedomain
        .MaintenanceAssociation yangMa = buildYangMaFromApiMa(ma);
        yangMa.addToMaintenanceAssociationEndPoint(yangMep);

        org.onosproject.yang.gen.v1.mseacfm.rev20160229.mseacfm
        .mefcfm.MaintenanceDomain yangMd = buildYangMdFromApiMd(md);
        yangMd.addToMaintenanceAssociation(yangMa);

        MefCfm mefCfm = new DefaultMefCfm();
        mefCfm.addToMaintenanceDomain(yangMd);

        MseaCfmOpParam mseaCfmOpParam = new MseaCfmOpParam();
        mseaCfmOpParam.mefCfm(mefCfm);
        try {
            mseaCfmService.setMseaCfm(mseaCfmOpParam, session, DatastoreId.RUNNING);
            log.info("Created MEP {} on device {}", mdName + "/" + maName +
                    "/" + mep.mepId(), handler().data().deviceId());
            return true;
        } catch (NetconfException e) {
            log.error("Unable to create MEP {}/{}/{} on device {}",
                    mdName, maName, mep.mepId(), handler().data().deviceId());
            throw new CfmConfigException("Unable to create MEP :" + e.getMessage());
        }
    }

    @Override
    public Collection<MepEntry> getAllMeps(MdId mdName, MaIdShort maName) throws CfmConfigException {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public MepEntry getMep(MdId mdName, MaIdShort maName, MepId mepId)
            throws CfmConfigException {
        NetconfController controller = checkNotNull(handler().get(NetconfController.class));
        if (handler().data().deviceId() == null) {
            throw new CfmConfigException("Device is not ready - connecting or "
                    + "disconnected for MEP " + mdName + "/" + maName + "/" + mepId);
        }
        NetconfSession session = controller.getDevicesMap().get(handler().data().deviceId()).getSession();
        MseaCfmNetconfService mseaCfmService = checkNotNull(handler().get(MseaCfmNetconfService.class));

        try {
            MseaCfm mseacfm =
                    mseaCfmService.getMepFull(mdName, maName, mepId, session);
            if (mseacfm != null && mseacfm.mefCfm() != null &&
                    mseacfm.mefCfm().maintenanceDomain() != null) {
                for (org.onosproject.yang.gen.v1.mseacfm.rev20160229.
                        mseacfm.mefcfm.MaintenanceDomain replyMd :
                                        mseacfm.mefCfm().maintenanceDomain()) {
                    for (org.onosproject.yang.gen.v1.mseacfm.rev20160229.
                            mseacfm.mefcfm.maintenancedomain.
                            MaintenanceAssociation replyMa :
                                            replyMd.maintenanceAssociation()) {
                        for (MaintenanceAssociationEndPoint replyMep :
                                    replyMa.maintenanceAssociationEndPoint()) {
                            return buildApiMepEntryFromYangMep(
                                    replyMep, handler().data().deviceId(), mdName, maName);
                        }
                    }
                }
            }
            log.warn("Mep " + mepId + " not found on device " + handler().data().deviceId());
            return null;
        } catch (NetconfException e) {
            log.error("Unable to get MEP {}/{}/{} on device {}",
                    mdName, maName, mepId, handler().data().deviceId());
            throw new CfmConfigException("Unable to get MEP :" + e.getMessage());
        }
    }

    @Override
    public boolean deleteMep(MdId mdName, MaIdShort maName, MepId mepId) throws CfmConfigException {

        NetconfController controller = checkNotNull(handler().get(NetconfController.class));
        NetconfSession session = controller.getDevicesMap().get(handler().data().deviceId()).getSession();
        MseaCfmNetconfService mseaCfmService = checkNotNull(handler().get(MseaCfmNetconfService.class));

        MaintenanceAssociationEndPoint mep =
                new DefaultMaintenanceAssociationEndPoint();
        mep.mepIdentifier(MepIdType.of(mepId.id()));

        org.onosproject.yang.gen.v1.mseacfm.rev20160229.mseacfm.mefcfm.maintenancedomain
            .MaintenanceAssociation yangMa = new DefaultMaintenanceAssociation();
        yangMa.maNameAndTypeCombo(MaNameUtil.getYangMaNameFromApiMaId(maName));
        yangMa.addToMaintenanceAssociationEndPoint(mep);

        org.onosproject.yang.gen.v1.mseacfm.rev20160229.mseacfm.mefcfm.MaintenanceDomain yangMd =
            new DefaultMaintenanceDomain();
        yangMd.mdNameAndTypeCombo(getYangMdNameFromApiMdId(mdName));
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
            log.error("Unable to delete MEP {}/{}/{} on device {}",
                    mdName, maName, mepId, handler().data().deviceId());
            throw new CfmConfigException("Unable to delete MEP :" + e.getMessage());
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

    private org.onosproject.yang.gen.v1.mseacfm.rev20160229.mseacfm.mefcfm
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

    protected static MdNameAndTypeCombo getYangMdNameFromApiMdId(MdId mdId)
            throws CfmConfigException {
        MdNameAndTypeCombo mdName;
        if (mdId instanceof MdIdDomainName) {
            boolean isIpAddr = false;
            try {
                if (IpAddress.valueOf(mdId.mdName()) != null) {
                    isIpAddr = true;
                }
            } catch (IllegalArgumentException e) {
                //continue
            }
            if (isIpAddr) {
                mdName = new DefaultNameDomainName();
                ((DefaultNameDomainName) mdName).nameDomainName(NameDomainNameUnion.of(
                                org.onosproject.yang.gen.v1.ietfinettypes.rev20130715.ietfinettypes.
                                IpAddress.fromString(mdId.mdName())));
            } else {
                mdName = new DefaultNameDomainName();
                ((DefaultNameDomainName) mdName).nameDomainName(NameDomainNameUnion
                                    .of(DomainName.fromString(mdId.mdName())));
            }
        } else if (mdId instanceof MdIdMacUint) {
            mdName = new DefaultMacAddressAndUint();
            ((DefaultMacAddressAndUint) mdName).nameMacAddressAndUint(MacAddressAndUintStr.fromString(mdId.mdName()));
        } else if (mdId instanceof MdIdNone) {
            mdName = new DefaultNameNone();
        } else if (mdId instanceof MdIdCharStr) {
            mdName = new DefaultNameCharacterString();
            ((DefaultNameCharacterString) mdName).name(Identifier45.fromString(mdId.mdName()));
        } else {
            throw new CfmConfigException("Unexpected error creating MD " +
                    mdId.getClass().getSimpleName());
        }
        return mdName;
    }

    private org.onosproject.yang.gen.v1.mseacfm.rev20160229.mseacfm.mefcfm
            .maintenancedomain.MaintenanceAssociation buildYangMaFromApiMa(
                        MaintenanceAssociation apiMa) throws CfmConfigException {

        MaNameAndTypeCombo maName = MaNameUtil.getYangMaNameFromApiMaId(apiMa.maId());

        org.onosproject.yang.gen.v1.mseacfm.rev20160229.mseacfm.mefcfm.maintenancedomain
                .MaintenanceAssociation yamgMa = new DefaultMaintenanceAssociation();
        yamgMa.maNameAndTypeCombo(maName);

        if (apiMa.remoteMepIdList() == null || apiMa.remoteMepIdList().size() < REMOTEMEPLIST_MIN_COUNT
                || apiMa.remoteMepIdList().size() > REMOTEMEPLIST_MAX_COUNT) {
            throw new CfmConfigException("EA1000 requires between " +
                    REMOTEMEPLIST_MIN_COUNT + " and " + REMOTEMEPLIST_MAX_COUNT +
                    " remote meps in an MA");
        }
        for (MepId rmep:apiMa.remoteMepIdList()) {
            yamgMa.addToRemoteMeps(MepIdType.of(rmep.id()));
        }

        if (apiMa.ccmInterval() != null) {
            switch (apiMa.ccmInterval()) {
            case INTERVAL_3MS:
                yamgMa.ccmInterval(CcmIntervalEnum.YANGAUTOPREFIX3_3MS);
                break;
            case INTERVAL_10MS:
                yamgMa.ccmInterval(CcmIntervalEnum.YANGAUTOPREFIX10MS);
                break;
            case INTERVAL_100MS:
                yamgMa.ccmInterval(CcmIntervalEnum.YANGAUTOPREFIX100MS);
                break;
            case INTERVAL_1S:
                yamgMa.ccmInterval(CcmIntervalEnum.YANGAUTOPREFIX1S);
                break;
            default:
                throw new CfmConfigException("EA1000 only supports "
                        + "3ms, 10ms, 100ms and 1s for CCM Interval. Rejecting: "
                        + apiMa.ccmInterval().name());
            }
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

    private MaintenanceAssociationEndPoint buildYangMepFromApiMep(Mep mep)
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
            MdId mdName, MaIdShort maName) throws CfmConfigException {
        MepId mepId = MepId.valueOf((short) yangMep.mepIdentifier().uint16());
        MepEntry.MepEntryBuilder builder = DefaultMepEntry.builder(mepId,
                deviceId,
                (yangMep.yangAutoPrefixInterface() == InterfaceEnum.ETH0) ?
                        PortNumber.portNumber(0) : PortNumber.portNumber(1),
                MepDirection.DOWN_MEP, //Always down for EA1000
                mdName, maName);

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
                builder = (MepEntry.MepEntryBuilder) builder.addToActiveRemoteMepList(
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

        if (augmentedyangMep != null) {
            if (augmentedyangMep.lastDefectSent() != null) {
                builder = (MepEntry.MepEntryBuilder) builder
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
}
