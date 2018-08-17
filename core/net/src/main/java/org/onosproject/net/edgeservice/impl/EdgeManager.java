/*
 * Copyright 2015-present Open Networking Foundation
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


package org.onosproject.net.edgeservice.impl;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.onosproject.event.AbstractListenerManager;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Link.Type;
import org.onosproject.net.device.DeviceEvent;
import org.onosproject.net.device.DeviceListener;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.edge.EdgePortEvent;
import org.onosproject.net.edge.EdgePortListener;
import org.onosproject.net.edge.EdgePortService;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.link.LinkEvent;
import org.onosproject.net.link.LinkListener;
import org.onosproject.net.link.LinkService;
import org.onosproject.net.packet.DefaultOutboundPacket;
import org.onosproject.net.packet.OutboundPacket;
import org.onosproject.net.packet.PacketService;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.onosproject.net.device.DeviceEvent.Type.DEVICE_ADDED;
import static org.onosproject.net.device.DeviceEvent.Type.DEVICE_AVAILABILITY_CHANGED;
import static org.onosproject.net.device.DeviceEvent.Type.DEVICE_REMOVED;
import static org.onosproject.net.device.DeviceEvent.Type.PORT_STATS_UPDATED;
import static org.onosproject.net.device.DeviceEvent.Type.PORT_UPDATED;
import static org.onosproject.net.edge.EdgePortEvent.Type.EDGE_PORT_ADDED;
import static org.onosproject.net.edge.EdgePortEvent.Type.EDGE_PORT_REMOVED;
import static org.onosproject.security.AppGuard.checkPermission;
import static org.onosproject.security.AppPermission.Type.PACKET_WRITE;
import static org.onosproject.security.AppPermission.Type.TOPOLOGY_READ;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * This is an implementation of the edge net service.
 */
@Component(immediate = true, service = EdgePortService.class)
public class EdgeManager
        extends AbstractListenerManager<EdgePortEvent, EdgePortListener>
        implements EdgePortService {

    private final Logger log = getLogger(getClass());

    // Set of edge ConnectPoints per Device.
    private final Map<DeviceId, Set<ConnectPoint>> connectionPoints = Maps.newConcurrentMap();

    private final DeviceListener deviceListener = new InnerDeviceListener();
    private final LinkListener linkListener = new InnerLinkListener();

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected PacketService packetService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected LinkService linkService;

    @Activate
    public void activate() {
        eventDispatcher.addSink(EdgePortEvent.class, listenerRegistry);
        deviceService.addListener(deviceListener);
        linkService.addListener(linkListener);
        loadAllEdgePorts();
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        deviceService.removeListener(deviceListener);
        linkService.removeListener(linkListener);
        eventDispatcher.removeSink(EdgePortEvent.class);
        log.info("Stopped");
    }

    @Override
    public boolean isEdgePoint(ConnectPoint point) {
        checkPermission(TOPOLOGY_READ);
        Set<ConnectPoint> connectPoints = connectionPoints.get(point.deviceId());
        return connectPoints != null && connectPoints.contains(point);
    }

    @Override
    public Iterable<ConnectPoint> getEdgePoints() {
        checkPermission(TOPOLOGY_READ);
        ImmutableSet.Builder<ConnectPoint> builder = ImmutableSet.builder();
        connectionPoints.forEach((k, v) -> v.forEach(builder::add));
        return builder.build();
    }

    @Override
    public Iterable<ConnectPoint> getEdgePoints(DeviceId deviceId) {
        checkPermission(TOPOLOGY_READ);
        ImmutableSet.Builder<ConnectPoint> builder = ImmutableSet.builder();
        Set<ConnectPoint> set = connectionPoints.get(deviceId);
        if (set != null) {
            set.forEach(builder::add);
        }
        return builder.build();
    }

    @Override
    public void emitPacket(ByteBuffer data, Optional<TrafficTreatment> treatment) {
        checkPermission(PACKET_WRITE);
        TrafficTreatment.Builder builder = treatment.map(DefaultTrafficTreatment::builder)
                .orElse(DefaultTrafficTreatment.builder());
        getEdgePoints().forEach(p -> packetService.emit(packet(builder, p, data)));
    }

    @Override
    public void emitPacket(DeviceId deviceId, ByteBuffer data,
                           Optional<TrafficTreatment> treatment) {
        TrafficTreatment.Builder builder = treatment.map(DefaultTrafficTreatment::builder)
                .orElse(DefaultTrafficTreatment.builder());
        getEdgePoints(deviceId).forEach(p -> packetService.emit(packet(builder, p, data)));
    }

    private OutboundPacket packet(TrafficTreatment.Builder builder, ConnectPoint point, ByteBuffer data) {
        builder.setOutput(point.port());
        return new DefaultOutboundPacket(point.deviceId(), builder.build(), data);
    }

    private class InnerLinkListener implements LinkListener {
        @Override
        public void event(LinkEvent event) {
            processLinkEvent(event);
        }
    }

    private class InnerDeviceListener implements DeviceListener {
        @Override
        public void event(DeviceEvent event) {
            if (event.type() == PORT_STATS_UPDATED) {
                return;
            }
            processDeviceEvent(event);
        }
    }

    // Initial loading of the edge port cache.
    private void loadAllEdgePorts() {
        deviceService.getAvailableDevices().forEach(d -> deviceService.getPorts(d.id())
                .forEach(p -> addEdgePort(new ConnectPoint(d.id(), p.number()))));
    }

    // Processes a link event by adding or removing its end-points in our cache.
    private void processLinkEvent(LinkEvent event) {
        // negative Link event can result in increase of edge ports
        boolean addEdgePort = event.type() == LinkEvent.Type.LINK_REMOVED;

        // but if the Link is an Edge type, it will be the opposite
        if (event.subject().type() == Type.EDGE) {
            addEdgePort = !addEdgePort;
        }

        if (addEdgePort) {
            addEdgePort(event.subject().src());
            addEdgePort(event.subject().dst());
        } else {
            removeEdgePort(event.subject().src());
            removeEdgePort(event.subject().dst());
        }
    }

    // Processes a device event by adding or removing its end-points in our cache.
    private void processDeviceEvent(DeviceEvent event) {
        //FIXME handle the case where a device is suspended, this may or may not come up
        DeviceEvent.Type type = event.type();
        DeviceId id = event.subject().id();

        if (type == DEVICE_ADDED ||
                type == DEVICE_AVAILABILITY_CHANGED && deviceService.isAvailable(id)) {
            // When device is added or becomes available, add all its ports
            deviceService.getPorts(event.subject().id())
                    .forEach(p -> addEdgePort(new ConnectPoint(id, p.number())));
        } else if (type == DEVICE_REMOVED ||
                type == DEVICE_AVAILABILITY_CHANGED && !deviceService.isAvailable(id)) {
            // When device is removed or becomes unavailable, remove all its ports.
            // Note: cannot rely on Device subsystem, ports may be gone.
            Optional.ofNullable(connectionPoints.remove(id))
                .orElse(ImmutableSet.of())
                .forEach(point -> post(new EdgePortEvent(EDGE_PORT_REMOVED, point)));

        } else if (type == DeviceEvent.Type.PORT_ADDED ||
                type == PORT_UPDATED && event.port().isEnabled()) {
            addEdgePort(new ConnectPoint(id, event.port().number()));
        } else if (type == DeviceEvent.Type.PORT_REMOVED ||
                type == PORT_UPDATED && !event.port().isEnabled()) {
            removeEdgePort(new ConnectPoint(id, event.port().number()));
        }
    }

    private boolean isEdgePort(ConnectPoint point) {
        // Logical ports are not counted as edge ports nor are infrastructure
        // ports. Ports that have only edge links are considered edge ports.
        return !point.port().isLogical() &&
                deviceService.getPort(point) != null &&
                linkService.getLinks(point).stream()
                        .allMatch(link -> link.type() == Type.EDGE);
    }

    // Adds the specified connection point to the edge points if needed.
    private void addEdgePort(ConnectPoint point) {
        if (isEdgePort(point)) {
            Set<ConnectPoint> set = connectionPoints.computeIfAbsent(point.deviceId(),
                                                                     (k) -> Sets.newConcurrentHashSet());
            if (set.add(point)) {
                post(new EdgePortEvent(EDGE_PORT_ADDED, point));
            }
        }
    }

    // Removes the specified connection point from the edge points.
    private void removeEdgePort(ConnectPoint point) {
        // trying to remove edge ports, so we shouldn't check if it's EdgePoint
        if (!point.port().isLogical()) {
            Set<ConnectPoint> set = connectionPoints.get(point.deviceId());
            if (set == null) {
                return;
            }
            if (set.remove(point)) {
                post(new EdgePortEvent(EDGE_PORT_REMOVED, point));
            }
            if (set.isEmpty()) {
                connectionPoints.computeIfPresent(point.deviceId(), (k, v) -> {
                    if (v.isEmpty()) {
                        return null;
                    } else {
                        return v;
                    }
                });
            }
        }
    }
}
