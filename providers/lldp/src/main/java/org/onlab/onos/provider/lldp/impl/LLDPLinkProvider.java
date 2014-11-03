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
package org.onlab.onos.provider.lldp.impl;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onlab.onos.mastership.MastershipService;
import org.onlab.onos.net.ConnectPoint;
import org.onlab.onos.net.Device;
import org.onlab.onos.net.DeviceId;
import org.onlab.onos.net.Port;
import org.onlab.onos.net.device.DeviceEvent;
import org.onlab.onos.net.device.DeviceListener;
import org.onlab.onos.net.device.DeviceService;
import org.onlab.onos.net.link.LinkProvider;
import org.onlab.onos.net.link.LinkProviderRegistry;
import org.onlab.onos.net.link.LinkProviderService;
import org.onlab.onos.net.packet.PacketContext;
import org.onlab.onos.net.packet.PacketProcessor;
import org.onlab.onos.net.packet.PacketService;
import org.onlab.onos.net.provider.AbstractProvider;
import org.onlab.onos.net.provider.ProviderId;
import org.slf4j.Logger;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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

    private final boolean useBDDP = true;


    private final InternalLinkProvider listener = new InternalLinkProvider();

    protected final Map<DeviceId, LinkDiscovery> discoverers = new ConcurrentHashMap<>();

    /**
     * Creates an OpenFlow link provider.
     */
    public LLDPLinkProvider() {
        super(new ProviderId("lldp", "org.onlab.onos.provider.lldp"));
    }

    @Activate
    public void activate() {
        providerService = providerRegistry.register(this);
        deviceService.addListener(listener);
        packetSevice.addProcessor(listener, 0);
        LinkDiscovery ld;
        for (Device device : deviceService.getDevices()) {
            ld = new LinkDiscovery(device, packetSevice, masterService,
                              providerService, useBDDP);
            discoverers.put(device.id(), ld);
            for (Port p : deviceService.getPorts(device.id())) {
                if (!p.number().isLogical()) {
                    ld.addPort(p);
                }
            }
        }

        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        for (LinkDiscovery ld : discoverers.values()) {
            ld.stop();
        }
        providerRegistry.unregister(this);
        deviceService.removeListener(listener);
        packetSevice.removeProcessor(listener);
        providerService = null;

        log.info("Stopped");
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
                    ld = discoverers.get(deviceId);
                    if (ld == null) {
                        log.debug("Device added ({}) {}", event.type(), deviceId);
                        discoverers.put(deviceId,
                               new LinkDiscovery(device, packetSevice, masterService,
                                      providerService, useBDDP));
                    } else {
                        if (ld.isStopped()) {
                            log.debug("Device restarted ({}) {}", event.type(), deviceId);
                            ld.start();
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
                    // TODO: Don't we need to removePort from ld?
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
                case DEVICE_MASTERSHIP_CHANGED:
                    if (!discoverers.containsKey(deviceId)) {
                        // TODO: ideally, should never reach here
                        log.debug("Device mastership changed ({}) {}", event.type(), deviceId);
                        discoverers.put(deviceId,
                               new LinkDiscovery(device, packetSevice, masterService,
                                      providerService, useBDDP));
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

}
