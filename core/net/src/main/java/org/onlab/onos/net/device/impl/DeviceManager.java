/*
 * Copyright 2014 Open Networking Laboratory
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
package org.onlab.onos.net.device.impl;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onlab.onos.net.device.DeviceEvent.Type.DEVICE_MASTERSHIP_CHANGED;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.List;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.onos.cluster.ClusterService;
import org.onlab.onos.cluster.NodeId;
import org.onlab.onos.cluster.RoleInfo;
import org.onlab.onos.event.AbstractListenerRegistry;
import org.onlab.onos.event.EventDeliveryService;
import org.onlab.onos.mastership.MastershipEvent;
import org.onlab.onos.mastership.MastershipListener;
import org.onlab.onos.mastership.MastershipService;
import org.onlab.onos.mastership.MastershipTerm;
import org.onlab.onos.mastership.MastershipTermService;
import org.onlab.onos.net.Device;
import org.onlab.onos.net.DeviceId;
import org.onlab.onos.net.MastershipRole;
import org.onlab.onos.net.Port;
import org.onlab.onos.net.PortNumber;
import org.onlab.onos.net.device.DefaultDeviceDescription;
import org.onlab.onos.net.device.DeviceAdminService;
import org.onlab.onos.net.device.DeviceClockProviderService;
import org.onlab.onos.net.device.DeviceDescription;
import org.onlab.onos.net.device.DeviceEvent;
import org.onlab.onos.net.device.DeviceListener;
import org.onlab.onos.net.device.DeviceProvider;
import org.onlab.onos.net.device.DeviceProviderRegistry;
import org.onlab.onos.net.device.DeviceProviderService;
import org.onlab.onos.net.device.DeviceService;
import org.onlab.onos.net.device.DeviceStore;
import org.onlab.onos.net.device.DeviceStoreDelegate;
import org.onlab.onos.net.device.PortDescription;
import org.onlab.onos.net.provider.AbstractProviderRegistry;
import org.onlab.onos.net.provider.AbstractProviderService;
import org.slf4j.Logger;

import com.google.common.collect.HashMultimap;

/**
 * Provides implementation of the device SB &amp; NB APIs.
 */
@Component(immediate = true)
@Service
public class DeviceManager
        extends AbstractProviderRegistry<DeviceProvider, DeviceProviderService>
        implements DeviceService, DeviceAdminService, DeviceProviderRegistry {

    private static final String DEVICE_ID_NULL = "Device ID cannot be null";
    private static final String PORT_NUMBER_NULL = "Port number cannot be null";
    private static final String DEVICE_DESCRIPTION_NULL = "Device description cannot be null";
    private static final String PORT_DESCRIPTION_NULL = "Port description cannot be null";
    private static final String ROLE_NULL = "Role cannot be null";

    private final Logger log = getLogger(getClass());

    protected final AbstractListenerRegistry<DeviceEvent, DeviceListener> listenerRegistry =
            new AbstractListenerRegistry<>();

    private final DeviceStoreDelegate delegate = new InternalStoreDelegate();

    private final MastershipListener mastershipListener = new InternalMastershipListener();

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceStore store;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected EventDeliveryService eventDispatcher;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ClusterService clusterService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected MastershipService mastershipService;

    protected MastershipTermService termService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceClockProviderService deviceClockProviderService;

    @Activate
    public void activate() {
        store.setDelegate(delegate);
        eventDispatcher.addSink(DeviceEvent.class, listenerRegistry);
        mastershipService.addListener(mastershipListener);
        termService = mastershipService.requestTermService();
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        store.unsetDelegate(delegate);
        mastershipService.removeListener(mastershipListener);
        eventDispatcher.removeSink(DeviceEvent.class);
        log.info("Stopped");
    }

    @Override
    public int getDeviceCount() {
        return store.getDeviceCount();
    }

    @Override
    public Iterable<Device> getDevices() {
        return store.getDevices();
    }

    @Override
    public Device getDevice(DeviceId deviceId) {
        checkNotNull(deviceId, DEVICE_ID_NULL);
        return store.getDevice(deviceId);
    }

    @Override
    public MastershipRole getRole(DeviceId deviceId) {
        checkNotNull(deviceId, DEVICE_ID_NULL);
        return mastershipService.getLocalRole(deviceId);
    }

    @Override
    public List<Port> getPorts(DeviceId deviceId) {
        checkNotNull(deviceId, DEVICE_ID_NULL);
        return store.getPorts(deviceId);
    }

    @Override
    public Port getPort(DeviceId deviceId, PortNumber portNumber) {
        checkNotNull(deviceId, DEVICE_ID_NULL);
        checkNotNull(portNumber, PORT_NUMBER_NULL);
        return store.getPort(deviceId, portNumber);
    }

    @Override
    public boolean isAvailable(DeviceId deviceId) {
        checkNotNull(deviceId, DEVICE_ID_NULL);
        return store.isAvailable(deviceId);
    }

    // Applies the specified role to the device; ignores NONE
    private void applyRole(DeviceId deviceId, MastershipRole newRole) {
        if (newRole.equals(MastershipRole.NONE)) {
            return;
        }

        Device device = store.getDevice(deviceId);
        // FIXME: Device might not be there yet. (eventual consistent)
        if (device == null) {
            return;
        }

        DeviceProvider provider = getProvider(device.providerId());
        if (provider != null) {
            provider.roleChanged(device, newRole);
            // only trigger event when request was sent to provider
            // TODO: consider removing this from Device event type?
            post(new DeviceEvent(DEVICE_MASTERSHIP_CHANGED, device));

            if (newRole.equals(MastershipRole.MASTER)) {
                provider.triggerProbe(device);
            }
        }
    }

    // Check a device for control channel connectivity.
    private boolean isReachable(Device device) {
        // FIXME: Device might not be there yet. (eventual consistent)
        if (device == null) {
            return false;
        }
        DeviceProvider provider = getProvider(device.providerId());
        return provider.isReachable(device);
    }

    @Override
    public void removeDevice(DeviceId deviceId) {
        checkNotNull(deviceId, DEVICE_ID_NULL);
        // XXX is this intended to apply to the full global topology?
        // if so, we probably don't want the fact that we aren't
        // MASTER to get in the way, as it would do now.
        // FIXME: forward or broadcast and let the Master handler the event.
        DeviceEvent event = store.removeDevice(deviceId);
        if (event != null) {
            log.info("Device {} administratively removed", deviceId);
            post(event);
        }
    }

    @Override
    public void addListener(DeviceListener listener) {
        listenerRegistry.addListener(listener);
    }

    @Override
    public void removeListener(DeviceListener listener) {
        listenerRegistry.removeListener(listener);
    }

    @Override
    protected DeviceProviderService createProviderService(
            DeviceProvider provider) {
        return new InternalDeviceProviderService(provider);
    }

    // Personalized device provider service issued to the supplied provider.
    private class InternalDeviceProviderService
            extends AbstractProviderService<DeviceProvider>
            implements DeviceProviderService {

        InternalDeviceProviderService(DeviceProvider provider) {
            super(provider);
        }

        @Override
        public void deviceConnected(DeviceId deviceId,
                                    DeviceDescription deviceDescription) {
            checkNotNull(deviceId, DEVICE_ID_NULL);
            checkNotNull(deviceDescription, DEVICE_DESCRIPTION_NULL);
            checkValidity();

            log.info("Device {} connected", deviceId);
            // check my Role
            MastershipRole role = mastershipService.requestRoleFor(deviceId);
            if (role != MastershipRole.MASTER) {
                // TODO: Do we need to explicitly tell the Provider that
                // this instance is no longer the MASTER? probably not
//                Device device = getDevice(deviceId);
//                if (device != null) {
//                    // FIXME roleChanged should take DeviceId instead of Device
//                    provider().roleChanged(device, role);
//                }
                return;
            }
            MastershipTerm term = termService.getMastershipTerm(deviceId);

            final NodeId myNodeId = clusterService.getLocalNode().id();
            if (!myNodeId.equals(term.master())) {
                // lost mastership after requestRole told this instance was MASTER.
                log.info("Role of this node is STANDBY for {}", deviceId);
                // TODO: Do we need to explicitly tell the Provider that
                // this instance is no longer the MASTER?
                //applyRole(deviceId, MastershipRole.STANDBY);
                return;
            }
            log.info("Role of this node is MASTER for {}", deviceId);

            // tell clock provider if this instance is the master
            deviceClockProviderService.setMastershipTerm(deviceId, term);

            DeviceEvent event = store.createOrUpdateDevice(provider().id(),
                                                           deviceId, deviceDescription);

            // If there was a change of any kind, tell the provider
            // that this instance is the master.
            // Note: event can be null, if mastership was lost between
            // roleRequest and store update calls.
            if (event != null) {
                // TODO: Check switch reconnected case. Is it assured that
                //       event will never be null?
                //       Could there be a situation MastershipService told this
                //       instance is the new Master, but
                //       event returned from the store is null?

                // FIXME: 1st argument should be deviceId, to allow setting
                //        certain roles even if the store returned null.
                log.info("event: {} {}", event.type(), event);
                provider().roleChanged(event.subject(), role);
                post(event);
            } else {
                log.info("No event to publish");
            }
        }

        @Override
        public void deviceDisconnected(DeviceId deviceId) {
            checkNotNull(deviceId, DEVICE_ID_NULL);
            checkValidity();

            log.info("Device {} disconnected from this node", deviceId);

            DeviceEvent event = null;
            try {
                event = store.markOffline(deviceId);
            } catch (IllegalStateException e) {
                log.warn("Failed to mark {} offline", deviceId);
                // only the MASTER should be marking off-line in normal cases,
                // but if I was the last STANDBY connection, etc. and no one else
                // was there to mark the device offline, this instance may need to
                // temporarily request for Master Role and mark offline.

                //there are times when this node will correctly have mastership, BUT
                //that isn't reflected in the ClockManager before the device disconnects.
                //we want to let go of the device anyways, so make sure this happens.

                // FIXME: Store semantics leaking out as IllegalStateException.
                //  Consider revising store API to handle this scenario.

                MastershipRole role = mastershipService.requestRoleFor(deviceId);
                MastershipTerm term = termService.getMastershipTerm(deviceId);
                final NodeId myNodeId = clusterService.getLocalNode().id();
                // TODO: Move this type of check inside device clock manager, etc.
                if (myNodeId.equals(term.master())) {
                    log.info("Retry marking {} offline", deviceId);
                    deviceClockProviderService.setMastershipTerm(deviceId, term);
                    event = store.markOffline(deviceId);
                } else {
                    log.info("Failed again marking {} offline. {}", deviceId, role);
                }
            } finally {
                //relinquish master role and ability to be backup.
                mastershipService.relinquishMastership(deviceId);

                if (event != null) {
                    log.info("Device {} disconnected from cluster", deviceId);
                    post(event);
                }
            }
        }

        @Override
        public void updatePorts(DeviceId deviceId,
                                List<PortDescription> portDescriptions) {
            checkNotNull(deviceId, DEVICE_ID_NULL);
            checkNotNull(portDescriptions,
                         "Port descriptions list cannot be null");
            checkValidity();

            if (!deviceClockProviderService.isTimestampAvailable(deviceId)) {
                // Never been a master for this device
                // any update will be ignored.
                log.trace("Ignoring {} port updates on standby node. {}", deviceId, portDescriptions);
                return;
            }

            List<DeviceEvent> events = store.updatePorts(this.provider().id(),
                                                         deviceId, portDescriptions);
            for (DeviceEvent event : events) {
                post(event);
            }
        }

        @Override
        public void portStatusChanged(DeviceId deviceId,
                                      PortDescription portDescription) {
            checkNotNull(deviceId, DEVICE_ID_NULL);
            checkNotNull(portDescription, PORT_DESCRIPTION_NULL);
            checkValidity();

            if (!deviceClockProviderService.isTimestampAvailable(deviceId)) {
                // Never been a master for this device
                // any update will be ignored.
                log.trace("Ignoring {} port update on standby node. {}", deviceId, portDescription);
                return;
            }

            final DeviceEvent event = store.updatePortStatus(this.provider().id(),
                                                             deviceId, portDescription);
            if (event != null) {
                log.info("Device {} port {} status changed", deviceId, event
                        .port().number());
                post(event);
            }
        }

        @Override
        public void receivedRoleReply(
                DeviceId deviceId, MastershipRole requested, MastershipRole response) {
            // Several things can happen here:
            // 1. request and response match
            // 2. request and response don't match
            // 3. MastershipRole and requested match (and 1 or 2 are true)
            // 4. MastershipRole and requested don't match (and 1 or 2 are true)
            //
            // 2, 4, and 3 with case 2 are failure modes.

            // FIXME: implement response to this notification

            log.info("got reply to a role request for {}: asked for {}, and got {}",
                    deviceId, requested, response);

            if (requested == null && response == null) {
                // something was off with DeviceProvider, maybe check channel too?
                log.warn("Failed to assert role [{}] onto Device {}", requested, deviceId);
                mastershipService.relinquishMastership(deviceId);
                return;
            }

            if (requested.equals(response)) {
                if (requested.equals(mastershipService.getLocalRole(deviceId))) {

                    return;
                } else {
                    return;
                    // FIXME roleManager got the device to comply, but doesn't agree with
                    // the store; use the store's view, then try to reassert.
                }
            } else {
                // we didn't get back what we asked for. Reelect someone else.
                log.warn("Failed to assert role [{}] onto Device {}", response, deviceId);
                if (response == MastershipRole.MASTER) {
                    mastershipService.relinquishMastership(deviceId);
                    // TODO: Shouldn't we be triggering event?
                    //final Device device = getDevice(deviceId);
                    //post(new DeviceEvent(DEVICE_MASTERSHIP_CHANGED, device));
                }
            }

        }
    }

    // Posts the specified event to the local event dispatcher.
    private void post(DeviceEvent event) {
        if (event != null && eventDispatcher != null) {
            eventDispatcher.post(event);
        }
    }

    // Intercepts mastership events
    private class InternalMastershipListener implements MastershipListener {

        // random cache size
        private final int cacheSize = 5;
        // temporarily stores term number + events to check for duplicates. A hack.
        private HashMultimap<Integer, RoleInfo> eventCache =
                HashMultimap.create();

        @Override
        public void event(MastershipEvent event) {
            final DeviceId did = event.subject();
            final NodeId myNodeId = clusterService.getLocalNode().id();

            if (myNodeId.equals(event.roleInfo().master())) {
                MastershipTerm term = termService.getMastershipTerm(did);

                // TODO duplicate suppression should probably occur in the MastershipManager
                // itself, so listeners that can't deal with duplicates don't have to
                // so this check themselves.
//                if (checkDuplicate(event.roleInfo(), term.termNumber())) {
//                    return;
//                }

                if (!myNodeId.equals(term.master())) {
                    // something went wrong in consistency, let go
                    log.warn("Mastership has changed after this event."
                                     + "Term Service suggests {} for {}", term, did);
                    // FIXME: Is it possible to let go of MASTER role
                    //        but remain on STANDBY list?
                    mastershipService.relinquishMastership(did);
                    applyRole(did, MastershipRole.STANDBY);
                    return;
                }

                // only set the new term if I am the master
                deviceClockProviderService.setMastershipTerm(did, term);

                // if the device is null here, we are the first master to claim the
                // device. No worries, the DeviceManager will create one soon.
                Device device = getDevice(did);
                if ((device != null) && !isAvailable(did)) {
                    if (!isReachable(device)) {
                        log.warn("Device {} has disconnected after this event", did);
                        mastershipService.relinquishMastership(did);
                        return;
                    }
                    //flag the device as online. Is there a better way to do this?
                    DeviceEvent devEvent =
                            store.createOrUpdateDevice(device.providerId(), did,
                                                       new DefaultDeviceDescription(
                                                               did.uri(), device.type(), device.manufacturer(),
                                                               device.hwVersion(), device.swVersion(),
                                                               device.serialNumber(), device.chassisId()));
                    post(devEvent);
                }
                applyRole(did, MastershipRole.MASTER);
            } else if (event.roleInfo().backups().contains(myNodeId)) {
                if (!isReachable(getDevice(did))) {
                    log.warn("Device {} has disconnected after this event", did);
                    mastershipService.relinquishMastership(did);
                    return;
                }
                applyRole(did, MastershipRole.STANDBY);
            } else {
                // Event suggests that this Node has no connection to this Device
                // confirm.
                final Device device = getDevice(did);
                if (!isReachable(device)) {
                    // not connection to device, as expected
                    return;
                }
                // connection seems to exist
                log.info("Detected mastership info mismatch, requesting Role");
                mastershipService.requestRoleFor(did);
                final MastershipTerm term = termService.getMastershipTerm(did);
                if (myNodeId.equals(term.master())) {
                    // became MASTER
                    // TODO: consider slicing out method for applying MASTER role
                    deviceClockProviderService.setMastershipTerm(did, term);

                    //flag the device as online. Is there a better way to do this?
                    DeviceEvent devEvent =
                            store.createOrUpdateDevice(device.providerId(), did,
                                                       new DefaultDeviceDescription(
                                                               did.uri(), device.type(), device.manufacturer(),
                                                               device.hwVersion(), device.swVersion(),
                                                               device.serialNumber(), device.chassisId()));
                    applyRole(did, MastershipRole.MASTER);
                    post(devEvent);
                } else {
                    applyRole(did, MastershipRole.STANDBY);
                }
            }
        }

        // checks for duplicate event, returning true if one is found.
        private boolean checkDuplicate(RoleInfo roleInfo, int term) {
            // turning off duplicate check
            return false;
//            synchronized (eventCache) {
//                if (eventCache.get(term).contains(roleInfo)) {
//                    log.info("duplicate event detected; ignoring");
//                    return true;
//                } else {
//                    eventCache.put(term, roleInfo);
//                    // purge by-term oldest entries to keep the cache size under limit
//                    if (eventCache.size() > cacheSize) {
//                        eventCache.removeAll(term - cacheSize);
//                    }
//                    return false;
//                }
//            }
        }

    }

    // Store delegate to re-post events emitted from the store.
    private class InternalStoreDelegate
            implements DeviceStoreDelegate {
        @Override
        public void notify(DeviceEvent event) {
            post(event);
        }
    }
}
