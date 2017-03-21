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
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;

import org.onosproject.drivers.microsemi.yang.MseaCfmNetconfService;
import org.onosproject.drivers.microsemi.yang.MseaCfmNetconfService.DmEntryParts;
import org.onosproject.drivers.microsemi.yang.utils.IetfYangTypesUtils;
import org.onosproject.drivers.microsemi.yang.utils.MepIdUtil;
import org.onosproject.incubator.net.l2monitoring.cfm.Mep.Priority;
import org.onosproject.incubator.net.l2monitoring.cfm.MepTsCreate;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MaIdShort;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MdId;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MepId;
import org.onosproject.incubator.net.l2monitoring.cfm.service.CfmConfigException;
import org.onosproject.incubator.net.l2monitoring.cfm.service.CfmMdService;
import org.onosproject.incubator.net.l2monitoring.soam.SoamConfigException;
import org.onosproject.incubator.net.l2monitoring.soam.SoamDmProgrammable;
import org.onosproject.incubator.net.l2monitoring.soam.SoamId;
import org.onosproject.incubator.net.l2monitoring.soam.StartTime.StartTimeOption;
import org.onosproject.incubator.net.l2monitoring.soam.StopTime.StopTimeOption;
import org.onosproject.incubator.net.l2monitoring.soam.delay.DefaultDelayMeasurementEntry;
import org.onosproject.incubator.net.l2monitoring.soam.delay.DefaultDelayMeasurementStatCurrent;
import org.onosproject.incubator.net.l2monitoring.soam.delay.DefaultDelayMeasurementStatHistory;
import org.onosproject.incubator.net.l2monitoring.soam.delay.DelayMeasurementCreate;
import org.onosproject.incubator.net.l2monitoring.soam.delay.DelayMeasurementCreate.DmType;
import org.onosproject.incubator.net.l2monitoring.soam.delay.DelayMeasurementCreate.MeasurementOption;
import org.onosproject.incubator.net.l2monitoring.soam.delay.DelayMeasurementCreate.Version;
import org.onosproject.incubator.net.l2monitoring.soam.delay.DelayMeasurementEntry;
import org.onosproject.incubator.net.l2monitoring.soam.delay.DelayMeasurementEntry.DmEntryBuilder;
import org.onosproject.incubator.net.l2monitoring.soam.delay.DelayMeasurementEntry.SessionStatus;
import org.onosproject.incubator.net.l2monitoring.soam.delay.DelayMeasurementStatCurrent;
import org.onosproject.incubator.net.l2monitoring.soam.delay.DelayMeasurementStatCurrent.DmStatCurrentBuilder;
import org.onosproject.incubator.net.l2monitoring.soam.delay.DelayMeasurementStatHistory;
import org.onosproject.incubator.net.l2monitoring.soam.delay.DelayMeasurementStatHistory.DmStatHistoryBuilder;
import org.onosproject.incubator.net.l2monitoring.soam.loss.LossMeasurementCreate;
import org.onosproject.incubator.net.l2monitoring.soam.loss.LossMeasurementEntry;
import org.onosproject.incubator.net.l2monitoring.soam.loss.LossMeasurementStatCurrent;
import org.onosproject.net.driver.AbstractHandlerBehaviour;
import org.onosproject.netconf.DatastoreId;
import org.onosproject.netconf.NetconfController;
import org.onosproject.netconf.NetconfException;
import org.onosproject.netconf.NetconfSession;
import org.onosproject.yang.gen.v1.mseacfm.rev20160229.MseaCfm;
import org.onosproject.yang.gen.v1.mseacfm.rev20160229.MseaCfmOpParam;
import org.onosproject.yang.gen.v1.mseacfm.rev20160229.mseacfm.DefaultMefCfm;
import org.onosproject.yang.gen.v1.mseacfm.rev20160229.mseacfm.MefCfm;
import org.onosproject.yang.gen.v1.mseacfm.rev20160229.mseacfm.mefcfm.DefaultMaintenanceDomain;
import org.onosproject.yang.gen.v1.mseacfm.rev20160229.mseacfm.mefcfm.maintenancedomain.DefaultMaintenanceAssociation;
import org.onosproject.yang.gen.v1.mseacfm.rev20160229.mseacfm.mefcfm.maintenancedomain.maintenanceassociation.DefaultMaintenanceAssociationEndPoint;
import org.onosproject.yang.gen.v1.mseacfm.rev20160229.mseacfm.mefcfm.maintenancedomain.maintenanceassociation.MaintenanceAssociationEndPoint;
import org.onosproject.yang.gen.v1.mseasoampm.rev20160229.mseasoampm.delaymeasurementbinsgroup.bins.FrameDelay;
import org.onosproject.yang.gen.v1.mseasoampm.rev20160229.mseasoampm.delaymeasurementbinsgroup.bins.InterFrameDelayVariation;
import org.onosproject.yang.gen.v1.mseasoampm.rev20160229.mseasoampm.mefcfm.maintenancedomain.maintenanceassociation.maintenanceassociationendpoint.AugmentedMseaCfmMaintenanceAssociationEndPoint;
import org.onosproject.yang.gen.v1.mseasoampm.rev20160229.mseasoampm.mefcfm.maintenancedomain.maintenanceassociation.maintenanceassociationendpoint.DefaultAugmentedMseaCfmMaintenanceAssociationEndPoint;
import org.onosproject.yang.gen.v1.mseasoampm.rev20160229.mseasoampm.mefcfm.maintenancedomain.maintenanceassociation.maintenanceassociationendpoint.augmentedmseacfmmaintenanceassociationendpoint.DefaultDelayMeasurements;
import org.onosproject.yang.gen.v1.mseasoampm.rev20160229.mseasoampm.mefcfm.maintenancedomain.maintenanceassociation.maintenanceassociationendpoint.augmentedmseacfmmaintenanceassociationendpoint.DelayMeasurements;
import org.onosproject.yang.gen.v1.mseasoampm.rev20160229.mseasoampm.mefcfm.maintenancedomain.maintenanceassociation.maintenanceassociationendpoint.augmentedmseacfmmaintenanceassociationendpoint.delaymeasurements.DefaultDelayMeasurement;
import org.onosproject.yang.gen.v1.mseasoampm.rev20160229.mseasoampm.mefcfm.maintenancedomain.maintenanceassociation.maintenanceassociationendpoint.augmentedmseacfmmaintenanceassociationendpoint.delaymeasurements.DelayMeasurement;
import org.onosproject.yang.gen.v1.mseasoampm.rev20160229.mseasoampm.mefcfm.maintenancedomain.maintenanceassociation.maintenanceassociationendpoint.augmentedmseacfmmaintenanceassociationendpoint.delaymeasurements.delaymeasurement.HistoryStats;
import org.onosproject.yang.gen.v1.mseasoampm.rev20160229.mseasoampm.mefcfm.maintenancedomain.maintenanceassociation.maintenanceassociationendpoint.augmentedmseacfmmaintenanceassociationendpoint.delaymeasurements.delaymeasurement.MeasurementEnable;
import org.onosproject.yang.gen.v1.mseasoampm.rev20160229.mseasoampm.mefcfm.maintenancedomain.maintenanceassociation.maintenanceassociationendpoint.augmentedmseacfmmaintenanceassociationendpoint.delaymeasurements.delaymeasurement.MessagePeriodEnum;
import org.onosproject.yang.gen.v1.mseasoampm.rev20160229.mseasoampm.remotemepgroup.remotemep.DefaultMepId;
import org.onosproject.yang.gen.v1.mseatypes.rev20160229.mseatypes.MepIdType;
import org.onosproject.yang.gen.v1.mseatypes.rev20160229.mseatypes.PriorityType;
import org.slf4j.Logger;

/**
 * Implementation of SoamDmProgrammable for Microsemi EA1000.
 */
public class EA1000SoamDmProgrammable extends AbstractHandlerBehaviour
        implements SoamDmProgrammable {
    private static final Logger log = getLogger(EA1000SoamDmProgrammable.class);
    private static final int MAX_DMS = 2;

    public EA1000SoamDmProgrammable() {
        log.debug("Loaded handler behaviour EA1000SoamDmProgrammable");
    }

    @Override
    public Collection<DelayMeasurementEntry> getAllDms(
            MdId mdName, MaIdShort maName, MepId mepId)
                    throws CfmConfigException, SoamConfigException {
        return getAllDmsOrOneDm(mdName, maName, mepId, null, DmEntryParts.ALL_PARTS);
    }

    @Override
    public DelayMeasurementEntry getDm(MdId mdName, MaIdShort maName,
            MepId mepId, SoamId dmId) throws CfmConfigException, SoamConfigException {
        Collection<DelayMeasurementEntry> allDms =
                getAllDmsOrOneDm(mdName, maName, mepId, dmId, DmEntryParts.ALL_PARTS);

        if (allDms != null && allDms.size() >= 1) {
            return allDms.toArray(new DelayMeasurementEntry[1])[0];
        }
        return null;
    }

    @Override
    public DelayMeasurementStatCurrent getDmCurrentStat(
            MdId mdName, MaIdShort maName, MepId mepId, SoamId dmId)
                    throws CfmConfigException, SoamConfigException {
        Collection<DelayMeasurementEntry> dms =
                getAllDmsOrOneDm(mdName, maName, mepId, dmId, DmEntryParts.CURRENT_ONLY);

        //There should be only one
        if (dms != null && dms.size() == 1) {
            return dms.toArray((new DelayMeasurementEntry[1]))[0].currentResult();
        }
        return null;
    }

    @Override
    public Collection<DelayMeasurementStatHistory> getDmHistoricalStats(
            MdId mdName, MaIdShort maName, MepId mepId, SoamId dmId)
                    throws CfmConfigException, SoamConfigException {
        Collection<DelayMeasurementEntry> dms =
                getAllDmsOrOneDm(mdName, maName, mepId, dmId, DmEntryParts.HISTORY_ONLY);

        //There should only be one in the result
        if (dms != null && dms.size() == 1) {
            return dms.toArray(new DelayMeasurementEntry[1])[0].historicalResults();
        }
        return new ArrayList<>();
    }

    @Override
    public Optional<SoamId> createDm(
            MdId mdName, MaIdShort maName, MepId mepId, DelayMeasurementCreate dm)
            throws CfmConfigException, SoamConfigException {
        NetconfController controller = checkNotNull(handler().get(NetconfController.class));
        NetconfSession session = controller.getDevicesMap().get(handler().data().deviceId()).getSession();
        MseaCfmNetconfService mseaCfmService = checkNotNull(handler().get(MseaCfmNetconfService.class));

        MseaCfm mepEssentials;
        try {
            mepEssentials = mseaCfmService.getMepEssentials(
                                        mdName, maName, mepId, session);
        } catch (NetconfException e) {
            throw new CfmConfigException(e);
        }
        short mdNumber = mepEssentials.mefCfm().maintenanceDomain().get(0).id();
        short maNumber = mepEssentials.mefCfm().maintenanceDomain().get(0)
                .maintenanceAssociation().get(0).id();
        MaintenanceAssociationEndPoint currentMep =
                mepEssentials.mefCfm().maintenanceDomain().get(0)
                .maintenanceAssociation().get(0)
                .maintenanceAssociationEndPoint().get(0);
        AugmentedMseaCfmMaintenanceAssociationEndPoint currAugMep =
                currentMep.augmentation(DefaultAugmentedMseaCfmMaintenanceAssociationEndPoint.class);

        if (dm.startTime() != null && !dm.startTime().option().equals(StartTimeOption.IMMEDIATE)) {
            throw new SoamConfigException(
                    "Only start time: IMMEDIATE is supported on EA1000");
        } else if (dm.stopTime() != null && !dm.stopTime().option().equals(StopTimeOption.NONE)) {
            throw new SoamConfigException(
                    "Only stop time: NONE is supported on EA1000");
        }

        MessagePeriodEnum mpEnum = MessagePeriodEnum.YANGAUTOPREFIX1000MS;
        if (dm.messagePeriod() != null) {
            if (dm.messagePeriod().toMillis() == 1000) {
                mpEnum = MessagePeriodEnum.YANGAUTOPREFIX1000MS;
            } else if (dm.messagePeriod().toMillis() == 100) {
                mpEnum = MessagePeriodEnum.YANGAUTOPREFIX100MS;
            } else if (dm.messagePeriod().toMillis() == 10) {
                mpEnum = MessagePeriodEnum.YANGAUTOPREFIX10MS;
            } else if (dm.messagePeriod().toMillis() == 3) {
                mpEnum = MessagePeriodEnum.YANGAUTOPREFIX3MS;
            } else {
                throw new SoamConfigException("EA1000 supports only Message "
                    + "Periods 1000ms,100ms, 10ms and 3ms for Delay Measurements");
            }
        }

        short lastDmId = 0;
        short newDmId = 1;
        if (currAugMep != null && currAugMep.delayMeasurements() != null) {
            Iterator<DelayMeasurement> dmIterator =
                    currAugMep.delayMeasurements().delayMeasurement().iterator();
            while (dmIterator.hasNext()) {
                lastDmId = dmIterator.next().dmId();
            }

            if (lastDmId == 0) {
                //Indicates that no DM was found under this MEP.
                //We will just create the next one as 1
                log.info("Creating DM 1");
                newDmId = 1;
            } else if (lastDmId == 1) {
                log.info("Creating DM 2");
                newDmId = 2;
            } else if (lastDmId == MAX_DMS) {
                log.warn("Maximum number of DMs (2) have been created on MEP {}/{}/{}"
                        + "on device {} - delete DMs before creating more",
                        mdName.mdName(), maName.maName(), mepId.id(),
                        handler().data().deviceId());
                throw new CfmConfigException("Maximum number of DMs (2) exist on MEP. "
                        + "Please call abort on a DM before creating more");
            }
        }


        DelayMeasurement dmBuilder = new DefaultDelayMeasurement();
        dmBuilder.dmId((short) newDmId);
        DefaultMepId dMepId = new DefaultMepId();
        dMepId.mepId(MepIdType.of(dm.remoteMepId().id()));
        dmBuilder.remoteMep(dMepId);

        BitSet measurementEnable = getMeasurementEnabledSet(dm.measurementsEnabled());
        if (measurementEnable != null && !measurementEnable.isEmpty()) {
            dmBuilder.measurementEnable(measurementEnable);
        }
        dmBuilder.administrativeState(true);
        dmBuilder.priority(PriorityType.of((short) dm.priority().ordinal()));
        dmBuilder.messagePeriod(mpEnum);

        if (dm.numberIntervalsStored() != null) {
            //Here we pass in num intervals stored - for EA1000 32 are always
            //stored so it's not controllable - instead we set number returned
            dmBuilder.numberIntervalsReturned(dm.numberIntervalsStored());
        }

        if (dm.measurementInterval() != null) {
            dmBuilder.measurementInterval(dm.measurementInterval().toMinutes());
        }
        if (dm.frameSize() != null) {
            dmBuilder.frameSize(dm.frameSize());
        }
        DefaultAugmentedMseaCfmMaintenanceAssociationEndPoint augmentedMep =
                new DefaultAugmentedMseaCfmMaintenanceAssociationEndPoint();
        DelayMeasurements dms = new DefaultDelayMeasurements();
        dms.addToDelayMeasurement(dmBuilder);
        augmentedMep.delayMeasurements(dms);

        MaintenanceAssociationEndPoint mep =
                new DefaultMaintenanceAssociationEndPoint();
        mep.mepIdentifier(MepIdType.of(mepId.id()));
        mep.addAugmentation(augmentedMep);


        org.onosproject.yang.gen.v1.mseacfm.rev20160229.mseacfm.mefcfm.maintenancedomain
            .MaintenanceAssociation yangMa = new DefaultMaintenanceAssociation();
        yangMa.id(maNumber);
        yangMa.addToMaintenanceAssociationEndPoint(mep);

        org.onosproject.yang.gen.v1.mseacfm.rev20160229.mseacfm.mefcfm
                .MaintenanceDomain yangMd = new DefaultMaintenanceDomain();
        yangMd.id(mdNumber);
        yangMd.addToMaintenanceAssociation(yangMa);

        MefCfm mefCfm = new DefaultMefCfm();
        mefCfm.addToMaintenanceDomain(yangMd);

        MseaCfmOpParam mseaCfmOpParam = new MseaCfmOpParam();
        mseaCfmOpParam.mefCfm(mefCfm);

        try {
            mseaCfmService.setMseaCfm(mseaCfmOpParam, session, DatastoreId.RUNNING);
            return Optional.empty();
        } catch (NetconfException e) {
            log.error("Unable to create DM {}/{}/{} on device {}",
                    mdName, maName, mepId, handler().data().deviceId());
            throw new CfmConfigException("Unable to create DM :" + e.getMessage());
        }

    }

    @Override
    public void abortDm(MdId mdName, MaIdShort maName, MepId mepId, SoamId dmId)
            throws CfmConfigException {
        NetconfController controller = checkNotNull(handler().get(NetconfController.class));
        NetconfSession session = controller.getDevicesMap().get(handler().data()
                .deviceId()).getSession();
        MseaCfmNetconfService mseaCfmService = checkNotNull(handler()
                .get(MseaCfmNetconfService.class));
        CfmMdService cfmMdService = checkNotNull(handler().get(CfmMdService.class));


        org.onosproject.yang.gen.v1.mseasoampm.rev20160229.mseasoampm.mefcfm.maintenancedomain
        .maintenanceassociation.maintenanceassociationendpoint
        .augmentedmseacfmmaintenanceassociationendpoint.delaymeasurements
        .DelayMeasurement dm = new DefaultDelayMeasurement();
         dm.dmId(dmId.id().shortValue());

        DefaultAugmentedMseaCfmMaintenanceAssociationEndPoint augmentedMep =
                new DefaultAugmentedMseaCfmMaintenanceAssociationEndPoint();
        DelayMeasurements ddms = new DefaultDelayMeasurements();
        ddms.addToDelayMeasurement(dm);
        augmentedMep.delayMeasurements(ddms);

        MaintenanceAssociationEndPoint mep =
                new DefaultMaintenanceAssociationEndPoint();
        mep.mepIdentifier(MepIdType.of(mepId.id()));
        mep.addAugmentation(augmentedMep);


        short mdNumericId = cfmMdService.getMaintenanceDomain(mdName).get().mdNumericId();
        short maNumericId = cfmMdService
                .getMaintenanceAssociation(mdName, maName).get().maNumericId();

        DefaultMaintenanceAssociation yangMa = new DefaultMaintenanceAssociation();
        yangMa.id(maNumericId);
        yangMa.addToMaintenanceAssociationEndPoint(mep);

        DefaultMaintenanceDomain yangMd = new DefaultMaintenanceDomain();
        yangMd.id(mdNumericId);
        yangMd.addToMaintenanceAssociation(yangMa);

        MefCfm mefCfm = new DefaultMefCfm();
        mefCfm.addToMaintenanceDomain(yangMd);

        MseaCfmOpParam mseaCfmOpParam = new MseaCfmOpParam();
        mseaCfmOpParam.mefCfm(mefCfm);

        try {
            mseaCfmService.deleteMseaCfmDm(mseaCfmOpParam, session, DatastoreId.RUNNING);
        } catch (NetconfException e) {
            log.error("Unable to delete DM {}/{}/{}/{} on device {}",
                    mdName, maName, mepId, dm.dmId(), handler().data().deviceId());
            throw new CfmConfigException("Unable to delete DM :" + e.getMessage());
        }
    }

    @Override
    public void abortDm(MdId mdName, MaIdShort maName, MepId mepId)
            throws CfmConfigException {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void clearDelayHistoryStats(MdId mdName, MaIdShort maName,
            MepId mepId) throws CfmConfigException {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void clearDelayHistoryStats(MdId mdName, MaIdShort maName,
            MepId mepId, SoamId dmId) throws CfmConfigException {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public Collection<LossMeasurementEntry> getAllLms(MdId mdName,
            MaIdShort maName, MepId mepId) throws CfmConfigException {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public LossMeasurementEntry getLm(MdId mdName, MaIdShort maName,
            MepId mepId, SoamId lmId) throws CfmConfigException {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public LossMeasurementStatCurrent getLmCurrentStat(MdId mdName,
            MaIdShort maName, MepId mepId, SoamId lmId) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public Collection<LossMeasurementStatCurrent> getLmHistoricalStats(
            MdId mdName, MaIdShort maName, MepId mepId, SoamId lmId) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public Optional<SoamId> createLm(MdId mdName, MaIdShort maName, MepId mepId,
                             LossMeasurementCreate lm) throws CfmConfigException {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void abortLm(MdId mdName, MaIdShort maName, MepId mepId)
            throws CfmConfigException {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void abortLm(MdId mdName, MaIdShort maName, MepId mepId, SoamId lmId)
            throws CfmConfigException {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void clearLossHistoryStats(MdId mdName, MaIdShort maName,
            MepId mepId) throws CfmConfigException {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void clearLossHistoryStats(MdId mdName, MaIdShort maName,
            MepId mepId, SoamId lmId) throws CfmConfigException {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void createTestSignal(MdId mdName, MaIdShort maName, MepId mepId,
            MepTsCreate tsCreate) throws CfmConfigException {
        throw new UnsupportedOperationException("Not supported by EA1000");
    }

    @Override
    public void abortTestSignal(MdId mdName, MaIdShort maName, MepId mepId)
            throws CfmConfigException {
        throw new UnsupportedOperationException("Not supported by EA1000");
    }

    private static DelayMeasurementEntry buildApiDmFromYangDm(DelayMeasurement dm,
            MdId mdName, MaIdShort maName, MepId mepId)
                    throws SoamConfigException, CfmConfigException {

        org.onosproject.yang.gen.v1.mseasoampm.rev20160229.mseasoampm.remotemepgroup.remotemep
        .MepId rmep = MepIdUtil.convertRemoteMepId(dm.remoteMep());

        DmEntryBuilder dmBuilder = (DmEntryBuilder) DefaultDelayMeasurementEntry.builder(
                SoamId.valueOf(dm.dmId()), DmType.DMDMM, Version.Y17312011,
                MepId.valueOf((short) ((MepIdType) rmep.mepId()).uint16()),
                Priority.values()[dm.priority().uint8()]);
        if (dm.sessionStatus() != null) {
            dmBuilder = dmBuilder.sessionStatus(SessionStatus.valueOf(
                    dm.sessionStatus().enumeration().name()));
        }

        if (dm.frameDelayTwoWay() != null) {
            dmBuilder = dmBuilder.frameDelayTwoWay(Duration.ofNanos(
                    dm.frameDelayTwoWay().uint32() * 1000));
        }
        if (dm.interFrameDelayVariationTwoWay() != null) {
            dmBuilder = dmBuilder.interFrameDelayVariationTwoWay(Duration.ofNanos(
                    dm.interFrameDelayVariationTwoWay().uint32() * 1000));
        }

        if (dm.frameSize() != 0) {
            dmBuilder = (DmEntryBuilder) dmBuilder.frameSize((short) dm.frameSize());
        }

        if (dm.messagePeriod() != null) {
            switch (dm.messagePeriod()) {
                case YANGAUTOPREFIX1000MS:
                    dmBuilder = (DmEntryBuilder) dmBuilder.messagePeriod(Duration.ofMillis(1000));
                    break;
                case YANGAUTOPREFIX100MS:
                    dmBuilder = (DmEntryBuilder) dmBuilder.messagePeriod(Duration.ofMillis(100));
                    break;
                case YANGAUTOPREFIX10MS:
                    dmBuilder = (DmEntryBuilder) dmBuilder.messagePeriod(Duration.ofMillis(10));
                    break;
                case YANGAUTOPREFIX3MS:
                    dmBuilder = (DmEntryBuilder) dmBuilder.messagePeriod(Duration.ofMillis(3));
                    break;
                default:
                    throw new SoamConfigException("EA1000 supports only 1000,"
                            + "100, 10 and 3ms for Message Period on DM");
            }
        }

        Collection<MeasurementOption> moSet =
            EA1000SoamDmProgrammable.getMeasurementOptions(dm.measurementEnable());
        moSet.forEach(dmBuilder::addToMeasurementsEnabled);

        dmBuilder = dmBuilder
                .currentResult(buildApiDmCurrFromYangDmCurr(dm, mdName, maName, mepId));

        for (DelayMeasurementStatHistory historyStat:
                            buildApiDmHistFromYangDm(dm, mdName, maName, mepId)) {
            dmBuilder = dmBuilder.addToHistoricalResults(historyStat);
        }

        return dmBuilder.build();
    }

    private static Collection<DelayMeasurementStatHistory> buildApiDmHistFromYangDm(
            DelayMeasurement dm, MdId mdName, MaIdShort maName, MepId mepId)
                    throws SoamConfigException, CfmConfigException {

        Collection<DelayMeasurementStatHistory> historyStatsCollection = new ArrayList<>();
        if (dm.historyStats() != null) {
            for (HistoryStats dmHistory:dm.historyStats()) {
                DmStatHistoryBuilder historyBuilder =
                        DefaultDelayMeasurementStatHistory.builder(
                                SoamId.valueOf((int) dmHistory.id()),
                                Duration.ofMillis(dmHistory.elapsedTime() * 10), //Values are in 1/100th sec
                                dmHistory.suspectStatus() != null ?
                                        dmHistory.suspectStatus().yangAutoPrefixBoolean() : false);
                historyBuilder = historyBuilder.endTime(
                        IetfYangTypesUtils.fromYangDateTimeToInstant(dmHistory.endTime()));

                if (dmHistory.frameDelayTwoWayMin() != null) {
                    historyBuilder = (DmStatHistoryBuilder) historyBuilder
                            .frameDelayTwoWayMin(Duration.ofNanos(dmHistory.frameDelayTwoWayMin().uint32() * 1000));
                }

                if (dmHistory.frameDelayTwoWayMax() != null) {
                    historyBuilder = (DmStatHistoryBuilder) historyBuilder
                            .frameDelayTwoWayMax(Duration.ofNanos(dmHistory.frameDelayTwoWayMax().uint32() * 1000));
                }

                if (dmHistory.frameDelayTwoWayAverage() != null) {
                    historyBuilder = (DmStatHistoryBuilder) historyBuilder
                           .frameDelayTwoWayAvg(Duration.ofNanos(dmHistory.frameDelayTwoWayAverage().uint32() * 1000));
                }

                if (dmHistory.interFrameDelayVariationTwoWayMin() != null) {
                    historyBuilder = (DmStatHistoryBuilder) historyBuilder
                            .interFrameDelayVariationTwoWayMin(Duration.ofNanos(
                                    dmHistory.interFrameDelayVariationTwoWayMin().uint32() * 1000));
                }

                if (dmHistory.interFrameDelayVariationTwoWayMax() != null) {
                    historyBuilder = (DmStatHistoryBuilder) historyBuilder
                            .interFrameDelayVariationTwoWayMax(Duration.ofNanos(
                                    dmHistory.interFrameDelayVariationTwoWayMax().uint32() * 1000));
                }

                if (dmHistory.interFrameDelayVariationTwoWayAverage() != null) {
                    historyBuilder = (DmStatHistoryBuilder) historyBuilder
                            .interFrameDelayVariationTwoWayAvg(Duration.ofNanos(
                                    dmHistory.interFrameDelayVariationTwoWayAverage().uint32() * 1000));
                }

                if (dmHistory.soamPdusReceived() != null) {
                    historyBuilder = (DmStatHistoryBuilder) historyBuilder
                            .soamPdusReceived(Integer.valueOf((int) dmHistory.soamPdusReceived().uint32()));
                }

                if (dmHistory.bins() != null && dmHistory.bins().frameDelay() != null) {
                    Map<Duration, Integer> frameDelayTwoWayBins = new HashMap<>();
                    for (FrameDelay fdBin:dmHistory.bins().frameDelay()) {
                        frameDelayTwoWayBins.put(
                                Duration.ofNanos(fdBin.lowerBound().uint32() * 1000),
                                Integer.valueOf((int) fdBin.counter().uint32()));
                    }
                    historyBuilder = (DmStatHistoryBuilder) historyBuilder
                            .frameDelayTwoWayBins(frameDelayTwoWayBins);
                }

                if (dmHistory.bins() != null && dmHistory.bins().interFrameDelayVariation() != null) {
                    Map<Duration, Integer> ifdvTwoWayBins = new HashMap<>();
                    for (InterFrameDelayVariation ifdvBin:dmHistory.bins().interFrameDelayVariation()) {
                        ifdvTwoWayBins.put(
                                Duration.ofNanos(ifdvBin.lowerBound().uint32() * 1000),
                                Integer.valueOf((int) ifdvBin.counter().uint32()));
                    }
                    historyBuilder =
                            (DmStatHistoryBuilder) historyBuilder.interFrameDelayVariationTwoWayBins(ifdvTwoWayBins);
                }

                historyStatsCollection.add((DelayMeasurementStatHistory) historyBuilder.build());
            }
        }
        return historyStatsCollection;
    }

    private static DelayMeasurementStatCurrent buildApiDmCurrFromYangDmCurr(
            DelayMeasurement dm, MdId mdName, MaIdShort maName, MepId mepId)
                    throws SoamConfigException, CfmConfigException {
        if (dm == null || dm.currentStats() == null || mdName == null ||
                maName == null || mepId == null) {
            return null;
        }

        DmStatCurrentBuilder statCurrBuilder =
           DefaultDelayMeasurementStatCurrent.builder(
                Duration.ofMillis(dm.currentStats().elapsedTime() * 10), //Values are in 1/100th sec
                dm.currentStats().suspectStatus() != null ?
                    dm.currentStats().suspectStatus().yangAutoPrefixBoolean() : false);
        statCurrBuilder = statCurrBuilder.startTime(
                IetfYangTypesUtils.fromYangDateTimeToInstant(dm.currentStats().startTime()));

        if (dm.currentStats().frameDelayTwoWayMin() != null) {
            statCurrBuilder = (DmStatCurrentBuilder) statCurrBuilder
                    .frameDelayTwoWayMin(Duration.ofNanos(
                            dm.currentStats().frameDelayTwoWayMin().uint32() * 1000));
        }

        if (dm.currentStats().frameDelayTwoWayMax() != null) {
            statCurrBuilder = (DmStatCurrentBuilder) statCurrBuilder
                    .frameDelayTwoWayMax(Duration.ofNanos(
                            dm.currentStats().frameDelayTwoWayMax().uint32() * 1000));
        }

        if (dm.currentStats().frameDelayTwoWayAverage() != null) {
            statCurrBuilder = (DmStatCurrentBuilder) statCurrBuilder
                   .frameDelayTwoWayAvg(Duration.ofNanos(
                           dm.currentStats().frameDelayTwoWayAverage().uint32() * 1000));
        }

        if (dm.currentStats().interFrameDelayVariationTwoWayMin() != null) {
            statCurrBuilder = (DmStatCurrentBuilder) statCurrBuilder
                    .interFrameDelayVariationTwoWayMin(Duration.ofNanos(
                            dm.currentStats().interFrameDelayVariationTwoWayMin().uint32() * 1000));
        }

        if (dm.currentStats().interFrameDelayVariationTwoWayMax() != null) {
            statCurrBuilder = (DmStatCurrentBuilder) statCurrBuilder
                    .interFrameDelayVariationTwoWayMax(Duration.ofNanos(
                            dm.currentStats().interFrameDelayVariationTwoWayMax().uint32() * 1000));
        }

        if (dm.currentStats().interFrameDelayVariationTwoWayAverage() != null) {
            statCurrBuilder = (DmStatCurrentBuilder) statCurrBuilder
                    .interFrameDelayVariationTwoWayAvg(Duration.ofNanos(
                            dm.currentStats().interFrameDelayVariationTwoWayAverage().uint32() * 1000));
        }

        if (dm.currentStats().soamPdusReceived() != null) {
            statCurrBuilder = (DmStatCurrentBuilder) statCurrBuilder
                    .soamPdusReceived(Integer.valueOf((int) dm.currentStats().soamPdusReceived().uint32()));
        }

        if (dm.currentStats().bins() != null && dm.currentStats().bins().frameDelay() != null) {
            Map<Duration, Integer> frameDelayTwoWayBins = new HashMap<>();
            for (FrameDelay fdBin:dm.currentStats().bins().frameDelay()) {
                frameDelayTwoWayBins.put(
                        Duration.ofNanos(fdBin.lowerBound().uint32() * 1000),
                        Integer.valueOf((int) fdBin.counter().uint32()));
            }
            statCurrBuilder = (DmStatCurrentBuilder) statCurrBuilder.frameDelayTwoWayBins(frameDelayTwoWayBins);
        }

        if (dm.currentStats().bins() != null && dm.currentStats().bins().interFrameDelayVariation() != null) {
            Map<Duration, Integer> ifdvTwoWayBins = new HashMap<>();
            for (InterFrameDelayVariation ifdvBin:dm.currentStats().bins().interFrameDelayVariation()) {
                ifdvTwoWayBins.put(
                        Duration.ofNanos(ifdvBin.lowerBound().uint32() * 1000),
                        Integer.valueOf((int) ifdvBin.counter().uint32()));
            }
            statCurrBuilder =
                    (DmStatCurrentBuilder) statCurrBuilder.interFrameDelayVariationTwoWayBins(ifdvTwoWayBins);
        }

        return (DelayMeasurementStatCurrent) statCurrBuilder.build();
    }

    private Collection<DelayMeasurementEntry> getAllDmsOrOneDm(
            MdId mdName, MaIdShort maName, MepId mepId, SoamId dmId, DmEntryParts parts)
                    throws CfmConfigException, SoamConfigException {
        NetconfController controller =
                checkNotNull(handler().get(NetconfController.class));
        NetconfSession session =
                controller.getDevicesMap().get(handler().data().deviceId()).getSession();
        MseaCfmNetconfService mseaCfmService =
                checkNotNull(handler().get(MseaCfmNetconfService.class));

        Collection<DelayMeasurementEntry> dmResults = new ArrayList<>();
        try {
            MseaCfm mseacfm =
                    mseaCfmService.getSoamDm(mdName, maName, mepId, dmId, parts, session);
            for (org.onosproject.yang.gen.v1.mseacfm.rev20160229.mseacfm.mefcfm
                    .MaintenanceDomain replyMd:mseacfm.mefCfm().maintenanceDomain()) {
                for (org.onosproject.yang.gen.v1.mseacfm.rev20160229.mseacfm
                        .mefcfm.maintenancedomain.MaintenanceAssociation replyMa:
                            replyMd.maintenanceAssociation()) {
                    for (MaintenanceAssociationEndPoint replyMep:
                            replyMa.maintenanceAssociationEndPoint()) {
                        AugmentedMseaCfmMaintenanceAssociationEndPoint augmentedMep =
                                replyMep.augmentation(
                                        DefaultAugmentedMseaCfmMaintenanceAssociationEndPoint.class);
                        if (augmentedMep == null ||
                                augmentedMep.delayMeasurements() == null ||
                                augmentedMep.delayMeasurements()
                                        .delayMeasurement().isEmpty()) {
                            log.info("No Delay Measurements retrieved from MEP " +
                                        mdName + "/" + maName + "/" + mepId);
                        } else {
                            for (org.onosproject.yang.gen.v1.mseasoampm.rev20160229
                                    .mseasoampm.mefcfm.maintenancedomain.maintenanceassociation
                                    .maintenanceassociationendpoint
                                    .augmentedmseacfmmaintenanceassociationendpoint
                                    .delaymeasurements.DelayMeasurement dm
                                        :augmentedMep.delayMeasurements().delayMeasurement()) {
                                dmResults.add(buildApiDmFromYangDm(dm, mdName, maName, mepId));
                            }
                        }
                    }
                }
            }
            return dmResults;
        } catch (NetconfException e) {
            log.error("Unable to get MEP {}/{}/{} on device {}",
                    mdName, maName, mepId, handler().data().deviceId());
            throw new CfmConfigException("Unable to create MEP :" + e.getMessage());
        }
    }


    protected static BitSet getMeasurementEnabledSet(
            Collection<MeasurementOption> measEnabled) throws SoamConfigException {
        BitSet measurementEnable = new BitSet();
        try {
            measEnabled.forEach(mo -> {
                    MeasurementEnable me = MeasurementEnable.valueOf(mo.name());
                    measurementEnable.set(me.measurementEnable());
            });
        } catch (IllegalArgumentException e) {
            throw new SoamConfigException(
                    "Measurement Option is not supported on EA1000: ", e);
        }

        return measurementEnable;
    }

    protected static Collection<MeasurementOption> getMeasurementOptions(BitSet meBs) {
        Collection<MeasurementOption> meList = new ArrayList<>();
        if (meBs != null && !meBs.isEmpty()) {
            for (int i = 0; i < meBs.size(); i++) {
                if (meBs.get(i)) {
                    meList.add(MeasurementOption.valueOf(MeasurementEnable.of(i).name()));
                }
            }
        }

        return meList;
    }
}