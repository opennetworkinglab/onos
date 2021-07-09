/*
 * Copyright 2018-present Open Networking Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onosproject.provider.general.device.impl;

import com.google.common.annotations.Beta;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.Striped;
import gnmi.Gnmi.Notification;
import gnmi.Gnmi.Path;
import gnmi.Gnmi.PathElem;
import gnmi.Gnmi.SubscribeRequest;
import gnmi.Gnmi.Subscription;
import gnmi.Gnmi.SubscriptionList;
import gnmi.Gnmi.SubscriptionMode;
import gnmi.Gnmi.Update;
import org.onosproject.gnmi.api.GnmiController;
import org.onosproject.gnmi.api.GnmiEvent;
import org.onosproject.gnmi.api.GnmiEventListener;
import org.onosproject.gnmi.api.GnmiUpdate;
import org.onosproject.gnmi.api.GnmiUtils;
import org.onosproject.mastership.MastershipEvent;
import org.onosproject.mastership.MastershipListener;
import org.onosproject.mastership.MastershipService;
import org.onosproject.net.DefaultAnnotations;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Port;
import org.onosproject.net.PortNumber;
import org.onosproject.net.device.DefaultPortDescription;
import org.onosproject.net.device.DeviceEvent;
import org.onosproject.net.device.DeviceListener;
import org.onosproject.net.device.DeviceProviderService;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.device.PortDescription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.locks.Lock;
import java.util.stream.Collectors;

import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;
import static org.onlab.util.Tools.groupedThreads;

/**
 * Entity that manages gNMI subscription for devices using OpenConfig models and
 * that reports relevant events to the core.
 */
@Beta
class GnmiDeviceStateSubscriber {

    private static final String LAST_CHANGE = "last-change";

    private static Logger log = LoggerFactory.getLogger(GnmiDeviceStateSubscriber.class);

    private final GnmiController gnmiController;
    private final DeviceService deviceService;
    private final DeviceProviderService providerService;
    private final MastershipService mastershipService;

    private final InternalGnmiEventListener gnmiEventListener = new InternalGnmiEventListener();
    private final InternalDeviceListener deviceEventListener = new InternalDeviceListener();
    private final InternalMastershipListener mastershipListener = new InternalMastershipListener();
    private final Map<DeviceId, Set<PortNumber>> deviceSubscribed = Maps.newHashMap();

    private final Striped<Lock> deviceLocks = Striped.lock(30);

    private ExecutorService eventExecutor;

    GnmiDeviceStateSubscriber(GnmiController gnmiController, DeviceService deviceService,
                              MastershipService mastershipService,
                              DeviceProviderService providerService) {
        this.gnmiController = gnmiController;
        this.deviceService = deviceService;
        this.mastershipService = mastershipService;
        this.providerService = providerService;
    }

    public void activate() {
        eventExecutor = newSingleThreadScheduledExecutor(groupedThreads(
                "onos/gnmi", "events-%d", log));
        deviceService.addListener(deviceEventListener);
        mastershipService.addListener(mastershipListener);
        gnmiController.addListener(gnmiEventListener);
        // Subscribe to existing devices.
        deviceService.getDevices().forEach(d -> checkSubscription(d.id()));
    }

    public void deactivate() {
        deviceSubscribed.keySet().forEach(this::unsubscribeIfNeeded);
        deviceService.removeListener(deviceEventListener);
        mastershipService.removeListener(mastershipListener);
        gnmiController.removeListener(gnmiEventListener);
        eventExecutor.shutdownNow();
        eventExecutor = null;
    }

    private void checkSubscription(DeviceId deviceId) {
        if (gnmiController.get(deviceId) == null) {
            // Ignore devices for which a gNMI client does not exist.
            return;
        }
        deviceLocks.get(deviceId).lock();
        try {
            if (shouldHaveSubscription(deviceId)) {
                subscribeIfNeeded(deviceId);
            } else {
                unsubscribeIfNeeded(deviceId);
            }
        } finally {
            deviceLocks.get(deviceId).unlock();
        }
    }

    private boolean shouldHaveSubscription(DeviceId deviceId) {
        return deviceService.getDevice(deviceId) != null
                && deviceService.isAvailable(deviceId)
                && mastershipService.isLocalMaster(deviceId)
                && !deviceService.getPorts(deviceId).isEmpty();
    }

    private Path interfaceStatePath(String interfaceName) {
        return Path.newBuilder()
                .addElem(PathElem.newBuilder().setName("interfaces").build())
                .addElem(PathElem.newBuilder()
                                 .setName("interface").putKey("name", interfaceName).build())
                .addElem(PathElem.newBuilder().setName("state").build())
                .build();
    }

    private void unsubscribeIfNeeded(DeviceId deviceId) {
        gnmiController.get(deviceId).unsubscribe();
        if (deviceSubscribed.remove(deviceId) != null) {
            log.info("Cancelled gNMI subscription for {}", deviceId);
        }
    }

    private void subscribeIfNeeded(DeviceId deviceId) {

        Set<PortNumber> ports = deviceService.getPorts(deviceId).stream()
                .map(Port::number)
                .collect(Collectors.toSet());

        if (Objects.equals(ports, deviceSubscribed.get(deviceId))) {
            // Already subscribed for the same ports.
            return;
        }

        // Subscribe for the new set of ports.
        deviceSubscribed.put(deviceId, ports);

        // Send subscription request.
        final SubscriptionList subscriptionList = SubscriptionList.newBuilder()
                .setMode(SubscriptionList.Mode.STREAM)
                .setUpdatesOnly(true)
                .addAllSubscription(ports.stream().map(
                        port -> Subscription.newBuilder()
                                .setPath(interfaceStatePath(port.name()))
                                .setMode(SubscriptionMode.ON_CHANGE)
                                .build()).collect(Collectors.toList()))
                .build();
        gnmiController.get(deviceId).subscribe(
                SubscribeRequest.newBuilder()
                        .setSubscribe(subscriptionList)
                        .build());

        log.info("Started gNMI subscription for {} ports on {}", ports.size(), deviceId);
    }

    private void handleGnmiUpdate(GnmiUpdate eventSubject) {
        Notification notification = eventSubject.update();
        if (notification == null) {
            log.warn("Cannot handle gNMI event without update data, abort");
            log.debug("gNMI update:\n{}", eventSubject);
            return;
        }

        long lastChange = 0;
        Update statusUpdate = null;
        Path path;
        PathElem lastElem;
        // The assumption is that the notification contains all the updates:
        // last-change, oper-status, counters, and so on. Otherwise, we need
        // to put in place the aggregation logic in ONOS
        for (Update update : notification.getUpdateList()) {
            path = update.getPath();
            lastElem = path.getElem(path.getElemCount() - 1);

            // Use last element to identify which state updated
            if ("oper-status".equals(lastElem.getName())) {
                statusUpdate = update;
            } else if ("last-change".equals(lastElem.getName())) {
                lastChange = update.getVal().getUintVal();
            } else if (log.isDebugEnabled()) {
                log.debug("Unrecognized update {}", GnmiUtils.pathToString(path));
            }
        }

        // Last-change could be not supported by the device
        // Cannot proceed without the status update.
        if (statusUpdate != null) {
            handleOperStatusUpdate(eventSubject.deviceId(), statusUpdate, lastChange);
        }
    }

    private void handleOperStatusUpdate(DeviceId deviceId, Update update, long timestamp) {
        Path path = update.getPath();
        // first element should be "interface"
        String interfaceName = path.getElem(1).getKeyOrDefault("name", null);
        if (interfaceName == null) {
            log.error("No interface present in gNMI update, abort");
            log.debug("gNMI update:\n{}", update);
            return;
        }

        List<Port> portsFromDevice = deviceService.getPorts(deviceId);
        portsFromDevice.forEach(port -> {
            if (!port.number().name().equals(interfaceName)) {
                return;
            }

            DefaultAnnotations portAnnotations = DefaultAnnotations.builder()
                    .putAll(port.annotations())
                    .set(LAST_CHANGE, String.valueOf(timestamp))
                    .build();

            // Port/Interface name is identical in OpenConfig model, but not in ONOS
            // This might cause some problem if we use one name to different port
            PortDescription portDescription = DefaultPortDescription.builder()
                    .portSpeed(port.portSpeed())
                    .withPortNumber(port.number())
                    .isEnabled(update.getVal().getStringVal().equals("UP"))
                    .type(port.type())
                    .annotations(portAnnotations)
                    .build();
            providerService.portStatusChanged(deviceId, portDescription);
        });
    }

    class InternalGnmiEventListener implements GnmiEventListener {

        @Override
        public void event(GnmiEvent event) {
            eventExecutor.execute(() -> {
                if (!deviceSubscribed.containsKey(event.subject().deviceId())) {
                    log.warn("Received gNMI event from {}, but we did'nt expect to " +
                                    "be subscribed to it! Discarding event...",
                            event.subject().deviceId());
                    return;
                }

                log.debug("Received gNMI event {}", event.toString());
                if (event.type() == GnmiEvent.Type.UPDATE) {
                    handleGnmiUpdate((GnmiUpdate) event.subject());
                } else {
                    log.debug("Unsupported gNMI event type: {}", event.type());
                }
            });
        }
    }

    class InternalMastershipListener implements MastershipListener {

        @Override
        public void event(MastershipEvent event) {
            eventExecutor.execute(() -> checkSubscription(event.subject()));
        }
    }

    class InternalDeviceListener implements DeviceListener {

        @Override
        public void event(DeviceEvent event) {
            eventExecutor.execute(() -> {
                switch (event.type()) {
                    case DEVICE_ADDED:
                    case DEVICE_AVAILABILITY_CHANGED:
                    case DEVICE_UPDATED:
                    case DEVICE_REMOVED:
                    case PORT_ADDED:
                    case PORT_REMOVED:
                        checkSubscription(event.subject().id());
                        break;
                    default:
                        break;
                }
            });
        }
    }
}
