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

import org.onosproject.core.CoreService;
import org.onosproject.core.IdGenerator;
import org.onosproject.event.AbstractListenerManager;
import org.onosproject.event.Event;
import org.onosproject.incubator.net.l2monitoring.cfm.MaintenanceDomain;
import org.onosproject.incubator.net.l2monitoring.cfm.Mep;
import org.onosproject.incubator.net.l2monitoring.cfm.MepEntry;
import org.onosproject.incubator.net.l2monitoring.cfm.MepLbCreate;
import org.onosproject.incubator.net.l2monitoring.cfm.MepLtCreate;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MaIdShort;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MdId;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MepId;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MepKeyId;
import org.onosproject.incubator.net.l2monitoring.cfm.service.CfmConfigException;
import org.onosproject.incubator.net.l2monitoring.cfm.service.CfmMdService;
import org.onosproject.incubator.net.l2monitoring.cfm.service.CfmMepEvent;
import org.onosproject.incubator.net.l2monitoring.cfm.service.CfmMepListener;
import org.onosproject.incubator.net.l2monitoring.cfm.service.CfmMepProgrammable;
import org.onosproject.incubator.net.l2monitoring.cfm.service.CfmMepService;
import org.onosproject.incubator.net.l2monitoring.cfm.service.MdEvent;
import org.onosproject.incubator.net.l2monitoring.cfm.service.MdListener;
import org.onosproject.incubator.net.l2monitoring.cfm.service.MepStore;
import org.onosproject.incubator.net.l2monitoring.cfm.service.MepStoreDelegate;
import org.onosproject.mastership.MastershipService;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.device.DeviceEvent;
import org.onosproject.net.device.DeviceListener;
import org.onosproject.net.device.DeviceService;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static org.onlab.util.Tools.groupedThreads;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Provides implementation of the CFM North and South Bound Interfaces.
 */
@Component(immediate = true, service = CfmMepService.class)
public class CfmMepManager
    extends AbstractListenerManager<CfmMepEvent, CfmMepListener>
    implements CfmMepService {

    private final Logger log = getLogger(getClass());

    private InternalDeviceListener deviceListener = null;
    private InternalMdListener mdListener = null;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected MastershipService mastershipService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected CfmMdService cfmMdService;

    private static final int DEFAULT_POLL_FREQUENCY = 30;
    private int fallbackMepPollFrequency = DEFAULT_POLL_FREQUENCY;

    private InternalEventHandler eventHandler = new InternalEventHandler();
    private static final Object THREAD_SCHED_LOCK = new Object();
    private static int numOfEventsQueued = 0;
    private static int numOfEventsExecuted = 0;
    private static int numOfHandlerExecution = 0;
    private static int numOfHandlerScheduled = 0;

    private ScheduledExecutorService executorService = Executors
            .newScheduledThreadPool(1,
                    groupedThreads("CfmMepManager", "event-%d", log));

    @SuppressWarnings("unused")
    private static ScheduledFuture<?> eventHandlerFuture = null;
    @SuppressWarnings("rawtypes")
    private ConcurrentLinkedQueue<Event> eventQueue = new ConcurrentLinkedQueue<>();


    private IdGenerator idGenerator;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected MepStore mepStore;

    protected final MepStoreDelegate delegate = new InternalStoreDelegate();

    @Activate
    public void activate() {
        mepStore.setDelegate(delegate);

        deviceListener = new InternalDeviceListener();
        deviceService.addListener(deviceListener);
        mdListener = new InternalMdListener();
        cfmMdService.addListener(mdListener);
        eventDispatcher.addSink(CfmMepEvent.class, listenerRegistry);
        idGenerator = coreService.getIdGenerator("mep-ids");
        log.info("CFM MEP Manager Started");
    }

    @Deactivate
    public void deactivate() {
        deviceService.removeListener(deviceListener);
        cfmMdService.removeListener(mdListener);
        eventDispatcher.removeSink(CfmMepEvent.class);
        log.info("CFM MEP Manager Stopped");
        mepStore.unsetDelegate(delegate);
        deviceListener = null;
        mdListener = null;
    }

    @Override
    public Collection<MepEntry> getAllMeps(MdId mdName, MaIdShort maName)
            throws CfmConfigException {
        //Will throw IllegalArgumentException if ma does not exist
        cfmMdService.getMaintenanceAssociation(mdName, maName);

        Collection<Mep> mepStoreCollection = mepStore.getAllMeps();
        Collection<MepEntry> mepEntryCollection = new ArrayList<>();

        for (Mep mep : mepStoreCollection) {
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
    public Collection<Mep> getAllMepsByDevice(DeviceId deviceId) throws CfmConfigException {
        return mepStore.getMepsByDeviceId(deviceId);
    }

    @Override
    public MepEntry getMep(MdId mdName, MaIdShort maName, MepId mepId) throws CfmConfigException {
        MepKeyId key = new MepKeyId(mdName, maName, mepId);

        //Will throw IllegalArgumentException if ma does not exist
        cfmMdService.getMaintenanceAssociation(mdName, maName);

        Optional<Mep> mepOptional = mepStore.getMep(key);
        if (mepOptional.isPresent()) {
            Mep mep = mepOptional.get();
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
        } else {
            return null;
        }
    }

    @Override
    public boolean deleteMep(MdId mdName, MaIdShort maName, MepId mepId,
                             Optional<MaintenanceDomain> oldMd) throws CfmConfigException {
        MepKeyId key = new MepKeyId(mdName, maName, mepId);

        //Will throw IllegalArgumentException if ma does not exist
        cfmMdService.getMaintenanceAssociation(mdName, maName);

        //Get the device ID from the MEP
        Optional<Mep> deletedMep = mepStore.getMep(key);
        if (!deletedMep.isPresent()) {
            log.warn("MEP {} not found when deleting Mep", key);
            return false;
        }

        DeviceId mepDeviceId = deletedMep.get().deviceId();
        boolean deleted = mepStore.deleteMep(key);

        Device mepDevice = deviceService.getDevice(mepDeviceId);
        if (mepDevice == null || !mepDevice.is(CfmMepProgrammable.class)) {
            throw new CfmConfigException("Unexpeced fault on device driver for "
                    + mepDeviceId);
        }
        try {
            deleted = mepDevice.as(CfmMepProgrammable.class)
                    .deleteMep(mdName, maName, mepId, oldMd);
        } catch (CfmConfigException e) {
            log.warn("MEP could not be deleted on device - perhaps it "
                    + "does not exist. Continuing");
        }

        //Iterate through all other devices and remove as a Remote Mep
        int mepsOnMdCount = 0;
        int mepsOnMaCount = 0;
        List<DeviceId> alreadyHandledDevices = new ArrayList<>();
        for (Mep mep : mepStore.getAllMeps()) {
            if (mep.deviceId().equals(mepDeviceId) && mdName.equals(mep.mdId())) {
                mepsOnMdCount++;
                if (maName.equals(mep.maId())) {
                    mepsOnMaCount++;
                }
            }
            if (mep.deviceId().equals(mepDeviceId) || !mep.mdId().equals(mdName) ||
                    !mep.maId().equals(maName) ||
                    alreadyHandledDevices.contains(mep.deviceId())) {
                continue;
            }
            deviceService.getDevice(mep.deviceId())
                    .as(CfmMepProgrammable.class)
                    .deleteMaRemoteMepOnDevice(mdName, maName, mepId);
            alreadyHandledDevices.add(mep.deviceId());
            log.info("Deleted RMep entry on {} on device {}",
                    mdName.mdName() + "/" + maName.maName(), mep.deviceId());
        }

        //Also if this is the last MEP in this MA then delete this MA from device
        //If this is the last MA in this MD on device, then delete the MD from the device
        if (mepsOnMdCount == 0) {
            boolean deletedMd = deviceService.getDevice(mepDeviceId)
                    .as(CfmMepProgrammable.class).deleteMdOnDevice(mdName, oldMd);
            log.info("Deleted MD {} from Device {}", mdName.mdName(), mepDeviceId);
        } else if (mepsOnMaCount == 0) {
            boolean deletedMa = deviceService.getDevice(mepDeviceId)
                    .as(CfmMepProgrammable.class).deleteMaOnDevice(mdName, maName, oldMd);
            log.info("Deleted MA {} from Device {}",
                    mdName.mdName() + "/" + maName.maName(), mepDeviceId);
        }

        return deleted;
    }

    @Override
    public boolean createMep(MdId mdName, MaIdShort maName, Mep newMep) throws CfmConfigException {
        MepKeyId key = new MepKeyId(mdName, maName, newMep.mepId());
        log.debug("Creating MEP " + newMep.mepId() + " on MD {}, MA {} on Device {}",
                mdName, maName, newMep.deviceId().toString());
        if (mepStore.getMep(key).isPresent()) {
            return false;
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
            boolean alreadyExisted = mepStore.createUpdateMep(key, newMep);

            //Add to other Remote Mep List on other devices
            for (Mep mep:mepStore.getMepsByMdMa(mdName, maName)) {
                List<DeviceId> alreadyHandledDevices = new ArrayList<>();
                if (mep.deviceId().equals(mepDeviceId) ||
                        alreadyHandledDevices.contains(mep.deviceId())) {
                    continue;
                }
                boolean created = deviceService.getDevice(mep.deviceId())
                        .as(CfmMepProgrammable.class)
                        .createMaRemoteMepOnDevice(mdName, maName, newMep.mepId());
                alreadyHandledDevices.add(mep.deviceId());
                log.info("Created RMep entry on {} on device {}",
                        mdName.mdName() + "/" + maName.maName(), mep.deviceId());
            }

            return !alreadyExisted;
        } else {
            return deviceResult;
        }
    }

    @Override
    public void transmitLoopback(MdId mdName, MaIdShort maName,
                                 MepId mepId, MepLbCreate lbCreate) throws CfmConfigException {
        MepKeyId key = new MepKeyId(mdName, maName, mepId);
        Mep mep = mepStore.getMep(key)
                .orElseThrow(() -> new CfmConfigException("Mep " + mdName + "/" + maName + "/"
                + mepId + " not found when calling Transmit Loopback"));

        log.debug("Transmitting Loopback on MEP {} on Device {}",
                key, mep.deviceId());
        deviceService.getDevice(mep.deviceId())
                .as(CfmMepProgrammable.class)
                .transmitLoopback(mdName, maName, mepId, lbCreate);
    }

    @Override
    public void abortLoopback(MdId mdName, MaIdShort maName, MepId mepId)
            throws CfmConfigException {

        MepKeyId key = new MepKeyId(mdName, maName, mepId);
        Mep mep = mepStore.getMep(key)
                .orElseThrow(() -> new CfmConfigException("Mep " + mdName + "/" + maName + "/"
                        + mepId + " not found when calling Aborting Loopback"));

        log.debug("Aborting Loopback on MEP {} on Device {}",
                key, mep.deviceId());
        deviceService.getDevice(mep.deviceId())
                .as(CfmMepProgrammable.class)
                .abortLoopback(mdName, maName, mepId);
    }

    @Override
    public void transmitLinktrace(MdId mdName, MaIdShort maName, MepId mepId,
                                  MepLtCreate ltCreate) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    private class InternalMdListener implements MdListener {
        @Override
        public boolean isRelevant(MdEvent event) {
            return event.type().equals(MdEvent.Type.MD_REMOVED) ||
                    event.type().equals(MdEvent.Type.MA_REMOVED);
        }

        @Override
        public void event(MdEvent event) {
            MdId mdName = event.subject();
            switch (event.type()) {
                case MA_REMOVED:
                case MD_REMOVED:
                    log.trace("Event {} receieved from MD Service for {}", event.type(), mdName);
                    scheduleEventHandlerIfNotScheduled(event);
                    break;
                default:
                    log.warn("Unhandled Event {} received from MD Service", event.type());
                    break;
            }
        }
    }

    private class InternalDeviceListener implements DeviceListener {
        @Override
        public boolean isRelevant(DeviceEvent event) {
            return event.type().equals(DeviceEvent.Type.DEVICE_REMOVED);
        }

        @Override
        public void event(DeviceEvent event) {
            DeviceId deviceId = event.subject().id();
            switch (event.type()) {
                case DEVICE_ADDED:
                case PORT_UPDATED:
                case PORT_ADDED:
                case DEVICE_UPDATED:
                case DEVICE_REMOVED:
                case DEVICE_AVAILABILITY_CHANGED:
                    log.trace("Event {} received from Device Service", event.type());
                    scheduleEventHandlerIfNotScheduled(event);
                    break;
                default:
                    log.warn("Unhandled Event {} received from Device Service", event.type());
                    break;
            }
        }
    }

    @SuppressWarnings("rawtypes")
    private void scheduleEventHandlerIfNotScheduled(Event event) {
        synchronized (THREAD_SCHED_LOCK) {
            eventQueue.add(event);
            numOfEventsQueued++;

            if ((numOfHandlerScheduled - numOfHandlerExecution) == 0) {
                //No pending scheduled event handling threads. So start a new one.
                eventHandlerFuture = executorService
                        .schedule(eventHandler, 100, TimeUnit.MILLISECONDS);
                numOfHandlerScheduled++;
            }
            log.trace("numOfEventsQueued {}, numOfEventHandlerScheduled {}",
                    numOfEventsQueued,
                    numOfHandlerScheduled);
        }
    }

    private class InternalEventHandler implements Runnable {
        @Override
        public void run() {
            try {
                while (true) {
                    @SuppressWarnings("rawtypes")
                    Event event;
                    synchronized (THREAD_SCHED_LOCK) {
                        if (!eventQueue.isEmpty()) {
                            event = eventQueue.poll();
                            numOfEventsExecuted++;
                        } else {
                            numOfHandlerExecution++;
                            log.debug("numOfHandlerExecution {} numOfEventsExecuted {}",
                                    numOfHandlerExecution, numOfEventsExecuted);
                            break;
                        }
                    }
                    if (event.type() == DeviceEvent.Type.DEVICE_REMOVED) {
                        DeviceId deviceId = ((Device) event.subject()).id();
                        log.info("Processing device removal event for unavailable device {}",
                                deviceId);
                        processDeviceRemoved((Device) event.subject());
                    } else if (event.type() == MdEvent.Type.MD_REMOVED) {
                        MdId mdName = (MdId) event.subject();
                        log.info("Processing MD removal event for MD {}",
                                mdName);
                        processMdRemoved(mdName, ((MdEvent) event).md().get());
                    } else if (event.type() == MdEvent.Type.MA_REMOVED) {
                        MdId mdName = (MdId) event.subject();
                        MaIdShort maName = ((MdEvent) event).maId().get();
                        log.info("Processing MA removal event for MA {}",
                                mdName.mdName() + "/" + maName.maName());
                        processMaRemoved(mdName, maName, ((MdEvent) event).md().get());
                    }
                }
            } catch (Exception e) {
                log.error("CfmMepService event handler "
                        + "thread thrown an exception: {}", e);
            }
        }
    }

    /**
     * This removes a MEP from the internal list of Meps, and updates remote meps list on other Meps.
     * Note: This does not call the device's CfmMepProgrammable, because there
     * would be no point as the device has already been removed from ONOS.
     * The configuration for this MEP may still be present on the actual device, and
     * any future config would have to be careful to wipe the Mep from the device
     * before applying a Mep again
     * @param removedDevice The device that has been removed
     */
    protected void processDeviceRemoved(Device removedDevice) {
        log.warn("Remove Mep(s) associated with Device: " + removedDevice.id());
        Collection<Mep> mepListForDevice = mepStore.getMepsByDeviceId(removedDevice.id());


        for (Mep mep:mepStore.getAllMeps()) {
            for (Mep mepForDevice:mepListForDevice) {
                if (mep.mdId().equals(mepForDevice.mdId()) && mep.maId().equals(mepForDevice.maId())) {
                    Device mepDevice = deviceService.getDevice(mep.deviceId());
                    log.info("Removing Remote Mep {} from MA{} on device {}",
                            mepForDevice.mepId(),
                            mep.mdId().mdName() + "/" + mep.maId().maName(),
                            mepDevice.id());
                    try {
                        mepDevice.as(CfmMepProgrammable.class)
                                .deleteMaRemoteMepOnDevice(mep.mdId(), mep.maId(), mepForDevice.mepId());
                    } catch (CfmConfigException e) {
                        log.error("Error when removing Remote Mep {} from MA {}. Continuing.",
                                mep.mdId().mdName() + "/" + mep.maId().maName(),
                                mepForDevice.mepId());
                    }
                }
            }
        }

        for (Iterator<Mep> iter = mepListForDevice.iterator(); iter.hasNext();) {
            mepStore.deleteMep(new MepKeyId(iter.next()));
        }
    }

    protected void processMaRemoved(MdId mdId, MaIdShort maId, MaintenanceDomain oldMd) {
        Set<DeviceId> deviceIdsRemoved = new HashSet<>();

        for (Iterator<Mep> iter = mepStore.getMepsByMdMa(mdId, maId).iterator(); iter.hasNext();) {
            Mep mepForMdMa = iter.next();
            DeviceId mepDeviceId = mepForMdMa.deviceId();
            try {
                deviceService.getDevice(mepDeviceId).as(CfmMepProgrammable.class)
                        .deleteMep(mdId, maId, mepForMdMa.mepId(), Optional.of(oldMd));
                deviceIdsRemoved.add(mepDeviceId);
            } catch (CfmConfigException e) {
                log.warn("Could not delete MEP {} from Device {}", mepForMdMa.mepId(), mepDeviceId, e);
            }
            iter.remove();

            log.info("Removed MEP {} from Device {} because MA {} was deleted",
                    mepForMdMa.mepId(), mepDeviceId, mdId.mdName() + "/" + maId.maName());
        }

        deviceIdsRemoved.forEach(deviceId -> {
            try {
                deviceService.getDevice(deviceId).as(CfmMepProgrammable.class)
                        .deleteMaOnDevice(mdId, maId, Optional.of(oldMd));
            } catch (CfmConfigException e) {
                log.warn("Could not delete MA {} from Device {}",
                        mdId.mdName() + "/" + maId.maName(), deviceId, e);
            }
        });
    }

    protected void processMdRemoved(MdId mdId, MaintenanceDomain oldMd) {
        Set<DeviceId> deviceIdsRemoved = new HashSet<>();
        for (Iterator<Mep> iter = mepStore.getMepsByMd(mdId).iterator(); iter.hasNext();) {
            Mep mep = iter.next();
            DeviceId mepDeviceId = mep.deviceId();
            try {
                deviceService.getDevice(mepDeviceId).as(CfmMepProgrammable.class)
                        .deleteMep(mdId, mep.maId(), mep.mepId(), Optional.of(oldMd));
                deviceIdsRemoved.add(mepDeviceId);
            } catch (CfmConfigException e) {
                log.warn("Could not delete MEP {} from Device {}", mep.mepId(), mepDeviceId, e);
            }
            iter.remove();
            log.info("Removed MEP {} from Device {} because MD {} was deleted",
                    mep.mepId(), mepDeviceId, mdId.mdName());
        }

        deviceIdsRemoved.forEach(deviceId -> {
            try {
                deviceService.getDevice(deviceId).as(CfmMepProgrammable.class)
                        .deleteMdOnDevice(mdId, Optional.of(oldMd));
            } catch (CfmConfigException e) {
                log.warn("Could not delete MD {} from Device {}",
                        mdId.mdName(), deviceId, e);
            }
        });
    }

    private class InternalStoreDelegate implements MepStoreDelegate {
        @Override
        public void notify(CfmMepEvent event) {
            log.debug("New Mep event: {}", event);
            eventDispatcher.post(event);
        }
    }
}
