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
package org.onosproject.incubator.net.l2monitoring.cfm.impl;

import static org.slf4j.LoggerFactory.getLogger;

import java.util.ArrayList;
import java.util.Collection;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onosproject.core.CoreService;
import org.onosproject.core.IdGenerator;
import org.onosproject.event.AbstractListenerManager;
import org.onosproject.incubator.net.l2monitoring.cfm.Mep;
import org.onosproject.incubator.net.l2monitoring.cfm.MepEntry;
import org.onosproject.incubator.net.l2monitoring.cfm.MepLbCreate;
import org.onosproject.incubator.net.l2monitoring.cfm.MepLtCreate;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MaIdShort;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MdId;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MepId;
import org.onosproject.incubator.net.l2monitoring.cfm.service.CfmConfigException;
import org.onosproject.incubator.net.l2monitoring.cfm.service.CfmMdService;
import org.onosproject.incubator.net.l2monitoring.cfm.service.CfmMepEvent;
import org.onosproject.incubator.net.l2monitoring.cfm.service.CfmMepListener;
import org.onosproject.incubator.net.l2monitoring.cfm.service.CfmMepProgrammable;
import org.onosproject.incubator.net.l2monitoring.cfm.service.CfmMepService;
import org.onosproject.mastership.MastershipService;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.device.DeviceEvent;
import org.onosproject.net.device.DeviceListener;
import org.onosproject.net.device.DeviceService;
import org.slf4j.Logger;

/**
 * Provides implementation of the CFM North and South Bound Interfaces.
 */
@Component(immediate = true)
@Service
public class CfmMepManager
    extends AbstractListenerManager<CfmMepEvent, CfmMepListener>
    implements CfmMepService {

    private final Logger log = getLogger(getClass());

    private final DeviceListener deviceListener = new InternalDeviceListener();

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected MastershipService mastershipService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CfmMdService cfmMdService;

    private static final int DEFAULT_POLL_FREQUENCY = 30;
    private int fallbackMepPollFrequency = DEFAULT_POLL_FREQUENCY;

    private IdGenerator idGenerator;

    //FIXME Get rid of this hack - we will use this in memory to emulate
    // a store for the short term.
    //Note: This is not distributed and will not work in a clustered system
    //TODO Create a MepStore for this
    private Collection<Mep> mepCollection;


    @Activate
    public void activate() {
        //FIXME Get rid of this local list
        mepCollection = new ArrayList<>();

        eventDispatcher.addSink(CfmMepEvent.class, listenerRegistry);
        deviceService.addListener(deviceListener);
        idGenerator = coreService.getIdGenerator("mep-ids");
        log.info("CFM MEP Manager Started");
    }

    @Deactivate
    public void deactivate() {
        deviceService.removeListener(deviceListener);
        eventDispatcher.removeSink(CfmMepEvent.class);
        log.info("CFM MEP Manager Stopped");
        mepCollection.clear();
    }

    @Override
    public Collection<MepEntry> getAllMeps(MdId mdName, MaIdShort maName)
            throws CfmConfigException {
        //Will throw IllegalArgumentException if ma does not exist
        cfmMdService.getMaintenanceAssociation(mdName, maName);

        Collection<MepEntry> mepEntryCollection = new ArrayList<>();

        for (Mep mep:mepCollection) {
            if (mep.mdId().equals(mdName) && mep.maId().equals(maName)) {
                DeviceId mepDeviceId = mep.deviceId();
                if (deviceService.getDevice(mepDeviceId) == null) {
                    log.warn("Device not found/available " + mepDeviceId +
                        " for MEP: " + mdName + "/" + maName + "/" + mep.mepId());
                    continue;
                } else if (!deviceService.getDevice(mepDeviceId)
                                        .is(CfmMepProgrammable.class)) {
                    throw new CfmConfigException("Device " + mepDeviceId +
                            " does not support CfmMepProgrammable behaviour.");
                }

                log.debug("Retrieving MEP results for Mep {} in MD {}, MA {} "
                    + "on Device {}", mep.mepId(), mdName, maName, mepDeviceId);
                mepEntryCollection.add(deviceService
                                        .getDevice(mepDeviceId)
                                        .as(CfmMepProgrammable.class)
                                        .getMep(mdName, maName, mep.mepId()));
            }
        }

        return mepEntryCollection;
    }

    @Override
    public MepEntry getMep(MdId mdName, MaIdShort maName, MepId mepId) throws CfmConfigException {
        //Will throw IllegalArgumentException if ma does not exist
        cfmMdService.getMaintenanceAssociation(mdName, maName);

        for (Mep mep : mepCollection) {
            if (mep.mdId().equals(mdName) && mep.maId().equals(maName)
                    && mep.mepId().equals(mepId)) {

                DeviceId mepDeviceId = mep.deviceId();
                if (deviceService.getDevice(mepDeviceId) == null) {
                    throw new CfmConfigException("Device not found " + mepDeviceId);
                } else if (!deviceService.getDevice(mepDeviceId).is(CfmMepProgrammable.class)) {
                    throw new CfmConfigException("Device " + mepDeviceId +
                            " does not support CfmMepProgrammable behaviour.");
                }

                log.debug("Retrieving MEP reults for Mep {} in MD {}, MA {} on Device {}",
                        mep.mepId(), mdName, maName, mepDeviceId);

                return deviceService.getDevice(mepDeviceId)
                        .as(CfmMepProgrammable.class).getMep(mdName, maName, mepId);
            }
        }
        return null;
    }

    @Override
    public boolean deleteMep(MdId mdName, MaIdShort maName, MepId mepId) throws CfmConfigException {
        //Will throw IllegalArgumentException if ma does not exist
        cfmMdService.getMaintenanceAssociation(mdName, maName);

        for (Mep mep : mepCollection) {
            if (mep.mdId().equals(mdName) && mep.maId().equals(maName)
                    && mep.mepId().equals(mepId)) {
                Device mepDevice = deviceService.getDevice(mep.deviceId());
                if (mepDevice == null || !mepDevice.is(CfmMepProgrammable.class)) {
                    throw new CfmConfigException("Unexpeced fault on device drier for "
                            + mep.deviceId());
                }
                boolean deleted = false;
                try {
                     deleted = mepDevice.as(CfmMepProgrammable.class)
                            .deleteMep(mdName, maName, mepId);
                } catch (CfmConfigException e) {
                    log.warn("MEP could not be deleted on device - perhaps it "
                            + "does not exist. Continuing");
                    mepCollection.remove(mep);
                    return false;
                }
                if (deleted) {
                    mepCollection.remove(mep);
                    return true;
                } else {
                    return false;
                }
            }
        }
        return false;
    }

    @Override
    public boolean createMep(MdId mdName, MaIdShort maName, Mep newMep) throws CfmConfigException {
        log.debug("Creating MEP " + newMep.mepId() + " on MD {}, MA {} on Device {}",
                mdName, maName, newMep.deviceId().toString());
        for (Mep mep : mepCollection) {
            if (mep.mdId().equals(mdName) && mep.maId().equals(maName)
                    && mep.mepId().equals(newMep.mepId())) {
                return false;
            }
        }

        //Will throw IllegalArgumentException if ma does not exist
        cfmMdService.getMaintenanceAssociation(mdName, maName);

        DeviceId mepDeviceId = newMep.deviceId();
        if (deviceService.getDevice(mepDeviceId) == null) {
            throw new CfmConfigException("Device not found " + mepDeviceId);
        } else if (!deviceService.getDevice(mepDeviceId).is(CfmMepProgrammable.class)) {
            throw new CfmConfigException("Device " + mepDeviceId + " does not support CfmMepProgrammable behaviour.");
        }

        boolean deviceResult =
                deviceService.getDevice(mepDeviceId).as(CfmMepProgrammable.class).createMep(mdName, maName, newMep);
        log.debug("MEP created on {}", mepDeviceId);
        if (deviceResult) {
            return mepCollection.add(newMep);
        } else {
            return deviceResult;
        }
    }

    @Override
    public void transmitLoopback(MdId mdName, MaIdShort maName,
            MepId mepId, MepLbCreate lbCreate) throws CfmConfigException {
        for (Mep mep : mepCollection) {
            if (mep.mdId().equals(mdName) && mep.maId().equals(maName)
                    && mep.mepId().equals(mepId)) {
                log.debug("Transmitting Loopback on MEP {}/{}/{} on Device {}",
                        mdName, maName, mepId, mep.deviceId());
                deviceService.getDevice(mep.deviceId())
                    .as(CfmMepProgrammable.class)
                    .transmitLoopback(mdName, maName, mepId, lbCreate);
                return;
            }
        }
        throw new CfmConfigException("Mep " + mdName + "/" + maName + "/"
                + mepId + " not found when calling Transmit Loopback");
    }

    @Override
    public void abortLoopback(MdId mdName, MaIdShort maName, MepId mepId)
            throws CfmConfigException {
        for (Mep mep : mepCollection) {
            if (mep.mdId().equals(mdName) && mep.maId().equals(maName)
                    && mep.mepId().equals(mepId)) {
                log.debug("Aborting Loopback on MEP {}/{}/{} on Device {}",
                        mdName, maName, mepId, mep.deviceId());
                deviceService.getDevice(mep.deviceId())
                    .as(CfmMepProgrammable.class)
                    .abortLoopback(mdName, maName, mepId);
                return;
            }
        }
        throw new CfmConfigException("Mep " + mdName + "/" + maName + "/"
                + mepId + " not found when calling Transmit Loopback");
    }

    @Override
    public void transmitLinktrace(MdId mdName, MaIdShort maName, MepId mepId,
            MepLtCreate ltCreate) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    private class InternalDeviceListener implements DeviceListener {
        @Override
        public void event(DeviceEvent event) {
            switch (event.type()) {
                case DEVICE_REMOVED:
                case DEVICE_AVAILABILITY_CHANGED:
                    DeviceId deviceId = event.subject().id();
                    if (!deviceService.isAvailable(deviceId)) {
                        log.warn("Device {} has been removed or changed", deviceId);
                    }
                    break;
                default:
                    break;
            }
        }
    }
}
