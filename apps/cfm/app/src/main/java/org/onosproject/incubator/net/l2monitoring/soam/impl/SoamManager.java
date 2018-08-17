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
package org.onosproject.incubator.net.l2monitoring.soam.impl;

import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.incubator.net.l2monitoring.cfm.MepEntry;
import org.onosproject.incubator.net.l2monitoring.cfm.MepTsCreate;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MaIdShort;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MdId;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MepId;
import org.onosproject.incubator.net.l2monitoring.cfm.service.CfmConfigException;
import org.onosproject.incubator.net.l2monitoring.cfm.service.CfmMepService;
import org.onosproject.incubator.net.l2monitoring.soam.SoamConfigException;
import org.onosproject.incubator.net.l2monitoring.soam.SoamDmProgrammable;
import org.onosproject.incubator.net.l2monitoring.soam.SoamId;
import org.onosproject.incubator.net.l2monitoring.soam.SoamService;
import org.onosproject.incubator.net.l2monitoring.soam.delay.DelayMeasurementCreate;
import org.onosproject.incubator.net.l2monitoring.soam.delay.DelayMeasurementEntry;
import org.onosproject.incubator.net.l2monitoring.soam.delay.DelayMeasurementStatCurrent;
import org.onosproject.incubator.net.l2monitoring.soam.delay.DelayMeasurementStatHistory;
import org.onosproject.incubator.net.l2monitoring.soam.loss.LossMeasurementCreate;
import org.onosproject.incubator.net.l2monitoring.soam.loss.LossMeasurementEntry;
import org.onosproject.incubator.net.l2monitoring.soam.loss.LossMeasurementStatCurrent;
import org.onosproject.net.DeviceId;
import org.onosproject.net.device.DeviceService;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Optional;

/**
 * ONOS application component.
 */
@Component(immediate = true, service = SoamService.class)
public class SoamManager implements SoamService {

    private final Logger log = LoggerFactory.getLogger(getClass());
    private static final String APP_ID = "org.onosproject.app.soam";

    private ApplicationId appId;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected CfmMepService cfmMepService;

    @Activate
    protected void activate() {
        appId = coreService.registerApplication(APP_ID);

        log.info("SOAM Service Started");
    }

    @Deactivate
    protected void deactivate() {
        log.info("SOAM Service Stopped");
    }

    @Override
    public Collection<DelayMeasurementEntry> getAllDms(
            MdId mdName, MaIdShort maName, MepId mepId)
                    throws CfmConfigException, SoamConfigException {
        MepEntry mep = cfmMepService.getMep(mdName, maName, mepId);
        if (mep == null || mep.deviceId() == null) {
            throw new CfmConfigException("MEP :"
                    + mdName + "/" + maName + "/" + mepId + " does not exist");
        } else if (deviceService.getDevice(mep.deviceId()) == null) {
            throw new CfmConfigException("Device " + mep.deviceId() + " from MEP :"
                    + mdName + "/" + maName + "/" + mepId + " does not exist");
        } else if (!deviceService.getDevice(mep.deviceId()).is(SoamDmProgrammable.class)) {
            throw new CfmConfigException("Device " + mep.deviceId() + " from MEP :"
                    + mdName + "/" + maName + "/" + mepId +
                    " does not implement SoamDmProgrammable");
        }
        log.debug("Retrieving DMs for MD {}, MA {}, MEP {} on Device {}",
                mdName, maName, mepId, mep.deviceId());

        return deviceService.getDevice(mep.deviceId())
                .as(SoamDmProgrammable.class).getAllDms(mdName, maName, mepId);
    };

    @Override
    public DelayMeasurementEntry getDm(MdId mdName, MaIdShort maName, MepId mepId, SoamId dmId)
                throws CfmConfigException, SoamConfigException {
        MepEntry mep = cfmMepService.getMep(mdName, maName, mepId);
        if (mep == null || mep.deviceId() == null) {
            throw new CfmConfigException("MEP :"
                    + mdName + "/" + maName + "/" + mepId + " does not exist");
        } else if (deviceService.getDevice(mep.deviceId()) == null) {
            throw new CfmConfigException("Device " + mep.deviceId() + " from MEP :"
                    + mdName + "/" + maName + "/" + mepId + " does not exist");
        } else if (!deviceService.getDevice(mep.deviceId()).is(SoamDmProgrammable.class)) {
            throw new CfmConfigException("Device " + mep.deviceId() + " from MEP :"
                    + mdName + "/" + maName + "/" + mepId +
                    " does not implement SoamDmProgrammable");
        }
        log.debug("Retrieving DM for DM {} in MD {}, MA {}, MEP {} on Device {}",
                dmId, mdName, maName, mepId, mep.deviceId());
        return deviceService.getDevice(mep.deviceId())
            .as(SoamDmProgrammable.class).getDm(mdName, maName, mepId, dmId);
    }

    @Override
    public DelayMeasurementStatCurrent getDmCurrentStat(MdId mdName,
            MaIdShort maName, MepId mepId, SoamId dmId)
                    throws CfmConfigException, SoamConfigException {
        MepEntry mep = cfmMepService.getMep(mdName, maName, mepId);
        if (mep == null || mep.deviceId() == null) {
            throw new CfmConfigException("MEP :"
                    + mdName + "/" + maName + "/" + mepId + " does not exist");
        } else if (deviceService.getDevice(mep.deviceId()) == null) {
            throw new CfmConfigException("Device " + mep.deviceId() + " from MEP :"
                    + mdName + "/" + maName + "/" + mepId + " does not exist");
        } else if (!deviceService.getDevice(mep.deviceId()).is(SoamDmProgrammable.class)) {
            throw new CfmConfigException("Device " + mep.deviceId() + " from MEP :"
                    + mdName + "/" + maName + "/" + mepId +
                    " does not implement SoamDmProgrammable");
        }
        log.debug("Retrieving Current Stats for DM {} in MD {}, MA {}, MEP {} "
                + "on Device {}", dmId, mdName, maName, mepId, mep.deviceId());
        return deviceService.getDevice(mep.deviceId())
            .as(SoamDmProgrammable.class).getDmCurrentStat(mdName, maName, mepId, dmId);
    }

    @Override
    public Collection<DelayMeasurementStatHistory> getDmHistoricalStats(
            MdId mdName, MaIdShort maName, MepId mepId, SoamId dmId)
                    throws SoamConfigException, CfmConfigException {
        MepEntry mep = cfmMepService.getMep(mdName, maName, mepId);
        if (mep == null || mep.deviceId() == null) {
            throw new CfmConfigException("MEP :"
                    + mdName + "/" + maName + "/" + mepId + " does not exist");
        } else if (deviceService.getDevice(mep.deviceId()) == null) {
            throw new CfmConfigException("Device " + mep.deviceId() + " from MEP :"
                    + mdName + "/" + maName + "/" + mepId + " does not exist");
        } else if (!deviceService.getDevice(mep.deviceId()).is(SoamDmProgrammable.class)) {
            throw new CfmConfigException("Device " + mep.deviceId() + " from MEP :"
                    + mdName + "/" + maName + "/" + mepId +
                    " does not implement SoamDmProgrammable");
        }
        log.debug("Retrieving History Stats for DM {} in MD {}, MA {}, MEP {} "
                + "on Device {}", dmId, mdName, maName, mepId, mep.deviceId());
        return deviceService.getDevice(mep.deviceId())
            .as(SoamDmProgrammable.class).getDmHistoricalStats(mdName, maName, mepId, dmId);
    }

    @Override
    public Optional<SoamId> createDm(MdId mdName, MaIdShort maName, MepId mepId,
                                    DelayMeasurementCreate dmNew)
                    throws CfmConfigException, SoamConfigException {
        DeviceId mepDeviceId = cfmMepService.getMep(mdName, maName, mepId).deviceId();
        if (mepDeviceId == null) {
            throw new CfmConfigException("Unable to create DM. MEP :"
                    + mdName + "/" + maName + "/" + mepId + " does not exist");
        } else if (deviceService.getDevice(mepDeviceId) == null) {
            throw new CfmConfigException("Device " + mepDeviceId + " from MEP :"
                    + mdName + "/" + maName + "/" + mepId + " does not exist");
        } else if (!deviceService.getDevice(mepDeviceId).is(SoamDmProgrammable.class)) {
            throw new CfmConfigException("Device " + mepDeviceId + " from MEP :"
                    + mdName + "/" + maName + "/" + mepId +
                    " does not implement SoamDmProgrammable");
        }
        log.debug("Creating new DM in MD {}, MA {}, MEP {} on Device {}",
                mdName, maName, mepId, mepDeviceId);
        return deviceService.getDevice(mepDeviceId)
            .as(SoamDmProgrammable.class).createDm(mdName, maName, mepId, dmNew);
    }

    @Override
    public void abortDm(MdId mdName, MaIdShort maName, MepId mepId)
            throws CfmConfigException {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void abortDm(MdId mdName, MaIdShort maName, MepId mepId, SoamId dmId)
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
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void abortTestSignal(MdId mdName, MaIdShort maName, MepId mepId)
            throws CfmConfigException {
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
