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
package org.onosproject.provider.lldp.impl;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onosproject.mastership.MastershipEvent;
import org.onosproject.mastership.MastershipListener;
import org.onosproject.mastership.MastershipService;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Port;
import org.onosproject.net.device.DeviceEvent;
import org.onosproject.net.device.DeviceListener;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.link.LinkProvider;
import org.onosproject.net.link.LinkProviderRegistry;
import org.onosproject.net.link.LinkProviderService;
import org.onosproject.net.packet.PacketContext;
import org.onosproject.net.packet.PacketProcessor;
import org.onosproject.net.packet.PacketService;
import org.onosproject.net.provider.AbstractProvider;
import org.onosproject.net.provider.ProviderId;
import org.slf4j.Logger;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;
import static org.onlab.util.Tools.namedThreads;
import static org.slf4j.LoggerFactory.getLogger;


/**
 * Provider which uses an OpenFlow controller to detect network
 * infrastructure links.
 */
@Component(immediate = true)
public class LLDPLinkProvider extends AbstractProvider implements LinkProvider {

    private final Logger log = getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected LinkProviderRegistry providerRegistry;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected PacketService packetSevice;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected MastershipService masterService;

    private LinkProviderService providerService;

    private ScheduledExecutorService executor;

    private final boolean useBDDP = true;

    private static final long INIT_DELAY = 5;
    private static final long DELAY = 5;

    private final InternalLinkProvider listener = new InternalLinkProvider();

    private final InternalRoleListener roleListener = new InternalRoleListener();

    protected final Map<DeviceId, LinkDiscovery> discoverers = new ConcurrentHashMap<>();

    /**
     * Creates an OpenFlow link provider.
     */
    public LLDPLinkProvider() {
        super(new ProviderId("lldp", "org.onosproject.provider.lldp"));
    }

    @Activate
    public void activate() {
        providerService = providerRegistry.register(this);
        deviceService.addListener(listener);
        packetSevice.addProcessor(listener, 0);
        masterService.addListener(roleListener);

        LinkDiscovery ld;
        for (Device device : deviceService.getAvailableDevices()) {
            ld = new LinkDiscovery(device, packetSevice, masterService,
                              providerService, useBDDP);
            discoverers.put(device.id(), ld);
            for (Port p : deviceService.getPorts(device.id())) {
                if (!p.number().isLogical()) {
                    ld.addPort(p);
                }
            }
        }

        executor = newSingleThreadScheduledExecutor(namedThreads("device-sync-%d"));
        executor.scheduleAtFixedRate(new SyncDeviceInfoTask(), INIT_DELAY,
                DELAY, TimeUnit.SECONDS);

        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        executor.shutdownNow();
        for (LinkDiscovery ld : discoverers.values()) {
            ld.stop();
        }
        providerRegistry.unregister(this);
        deviceService.removeListener(listener);
        packetSevice.removeProcessor(listener);
        masterService.removeListener(roleListener);
        providerService = null;

        log.info("Stopped");
    }

    private class InternalRoleListener implements MastershipListener {

        @Override
        public void event(MastershipEvent event) {

            if (MastershipEvent.Type.BACKUPS_CHANGED.equals(event.type())) {
                // only need new master events
                return;
            }

            DeviceId deviceId = event.subject();
            Device device = deviceService.getDevice(deviceId);
            if (device == null) {
                log.warn("Device {} doesn't exist, or isn't there yet", deviceId);
                return;
            }
            synchronized (discoverers) {
                if (!discoverers.containsKey(deviceId)) {
                    // ideally, should never reach here
                    log.debug("Device mastership changed ({}) {}",
                            event.type(), deviceId);
                    discoverers.put(deviceId, new LinkDiscovery(device,
                            packetSevice, masterService, providerService,
                            useBDDP));
                }
            }
        }

    }

    private class InternalLinkProvider implements PacketProcessor, DeviceListener {

        @Override
        public void event(DeviceEvent event) {
            LinkDiscovery ld = null;
            Device device = event.subject();
            Port port = event.port();
            if (device == null) {
                log.error("Device is null.");
                return;
            }
            log.trace("{} {} {}", event.type(), event.subject(), event);
            final DeviceId deviceId = device.id();
            switch (event.type()) {
                case DEVICE_ADDED:
                case DEVICE_UPDATED:
                synchronized (discoverers) {
                    ld = discoverers.get(deviceId);
                    if (ld == null) {
                        log.debug("Device added ({}) {}", event.type(),
                                    deviceId);
                        discoverers.put(deviceId, new LinkDiscovery(device,
                                packetSevice, masterService, providerService,
                                useBDDP));
                    } else {
                        if (ld.isStopped()) {
                            log.debug("Device restarted ({}) {}", event.type(),
                                    deviceId);
                            ld.start();
                        }
                        }
                    }
                    break;
                case PORT_ADDED:
                case PORT_UPDATED:
                    if (port.isEnabled()) {
                        ld = discoverers.get(deviceId);
                        if (ld == null) {
                            return;
                        }
                        if (!port.number().isLogical()) {
                            log.debug("Port added {}", port);
                            ld.addPort(port);
                        }
                    } else {
                        log.debug("Port down {}", port);
                        ConnectPoint point = new ConnectPoint(deviceId,
                                                              port.number());
                        providerService.linksVanished(point);
                    }
                    break;
                case PORT_REMOVED:
                    log.debug("Port removed {}", port);
                    ConnectPoint point = new ConnectPoint(deviceId,
                                                          port.number());
                    providerService.linksVanished(point);

                    break;
                case DEVICE_REMOVED:
                case DEVICE_SUSPENDED:
                    log.debug("Device removed {}", deviceId);
                    ld = discoverers.get(deviceId);
                    if (ld == null) {
                        return;
                    }
                    ld.stop();
                    providerService.linksVanished(deviceId);
                    break;
                case DEVICE_AVAILABILITY_CHANGED:
                    ld = discoverers.get(deviceId);
                    if (ld == null) {
                        return;
                    }
                    if (deviceService.isAvailable(deviceId)) {
                        log.debug("Device up {}", deviceId);
                        ld.start();
                    } else {
                        providerService.linksVanished(deviceId);
                        log.debug("Device down {}", deviceId);
                        ld.stop();
                    }
                    break;
                default:
                    log.debug("Unknown event {}", event);
            }
        }

        @Override
        public void process(PacketContext context) {
            if (context == null) {
                return;
            }
            LinkDiscovery ld = discoverers.get(
                    context.inPacket().receivedFrom().deviceId());
            if (ld == null) {
                return;
            }

            if (ld.handleLLDP(context)) {
                context.block();
            }
        }
    }

    private final class SyncDeviceInfoTask implements Runnable {

        @Override
        public void run() {
            if (Thread.currentThread().isInterrupted()) {
                log.info("Interrupted, quitting");
                return;
            }
            // check what deviceService sees, to see if we are missing anything
            try {
                LinkDiscovery ld = null;
                for (Device dev : deviceService.getDevices()) {
                    DeviceId did = dev.id();
                    synchronized (discoverers) {
                        if (!discoverers.containsKey(did)) {
                            ld = new LinkDiscovery(dev, packetSevice,
                                    masterService, providerService, useBDDP);
                            discoverers.put(did, ld);
                            for (Port p : deviceService.getPorts(did)) {
                                if (!p.number().isLogical()) {
                                    ld.addPort(p);
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                // catch all Exception to avoid Scheduled task being suppressed.
                log.error("Exception thrown during synchronization process", e);
            }
        }
    }

}
