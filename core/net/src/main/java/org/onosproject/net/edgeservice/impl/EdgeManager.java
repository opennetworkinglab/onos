/*
 * Copyright 2015-present Open Networking Laboratory
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
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onosproject.event.AbstractListenerManager;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Link.Type;
import org.onosproject.net.device.DeviceEvent;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.edge.EdgePortEvent;
import org.onosproject.net.edge.EdgePortListener;
import org.onosproject.net.edge.EdgePortService;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.link.LinkEvent;
import org.onosproject.net.packet.DefaultOutboundPacket;
import org.onosproject.net.packet.OutboundPacket;
import org.onosproject.net.packet.PacketService;
import org.onosproject.net.topology.Topology;
import org.onosproject.net.topology.TopologyEvent;
import org.onosproject.net.topology.TopologyListener;
import org.onosproject.net.topology.TopologyService;
import org.slf4j.Logger;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import static org.onosproject.net.device.DeviceEvent.Type.*;
import static org.onosproject.net.edge.EdgePortEvent.Type.EDGE_PORT_ADDED;
import static org.onosproject.net.edge.EdgePortEvent.Type.EDGE_PORT_REMOVED;
import static org.slf4j.LoggerFactory.getLogger;
import static org.onosproject.security.AppGuard.checkPermission;
import static org.onosproject.security.AppPermission.Type.*;

/**
 * This is an implementation of the edge net service.
 */
@Component(immediate = true)
@Service
public class EdgeManager
        extends AbstractListenerManager<EdgePortEvent, EdgePortListener>
        implements EdgePortService {

    private final Logger log = getLogger(getClass());

    private Topology topology;

    /**
     * Set of edge ConnectPoints per Device.
     */
    private final Map<DeviceId, Set<ConnectPoint>> connectionPoints = Maps.newConcurrentMap();

    private final TopologyListener topologyListener = new InnerTopologyListener();

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected PacketService packetService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected TopologyService topologyService;

    @Activate
    public void activate() {
        eventDispatcher.addSink(EdgePortEvent.class, listenerRegistry);
        topologyService.addListener(topologyListener);
        loadAllEdgePorts();
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        topologyService.removeListener(topologyListener);
        eventDispatcher.removeSink(EdgePortEvent.class);
        log.info("Stopped");
    }

    @Override
    public boolean isEdgePoint(ConnectPoint point) {
        checkPermission(TOPOLOGY_READ);
        return !topologyService.isInfrastructure(topologyService.currentTopology(), point);
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

    private class InnerTopologyListener implements TopologyListener {

        @Override
        public void event(TopologyEvent event) {
            log.trace("Processing TopologyEvent {} caused by {}",
                      event.subject(), event.reasons());
            topology = event.subject();
            event.reasons().forEach(reason -> {
                if (reason instanceof DeviceEvent) {
                    processDeviceEvent((DeviceEvent) reason);
                } else if (reason instanceof LinkEvent) {
                    processLinkEvent((LinkEvent) reason);
                }
            });
        }
    }

    // Initial loading of the edge port cache.
    private void loadAllEdgePorts() {
        topology = topologyService.currentTopology();
        deviceService.getAvailableDevices().forEach(d -> deviceService.getPorts(d.id())
                .forEach(p -> addEdgePort(new ConnectPoint(d.id(), p.number()))));
    }

    // Processes a link event by adding or removing its end-points in our cache.
    private void processLinkEvent(LinkEvent event) {
        // negative Link event can result in increase of edge ports
        boolean addEdgePort = event.type() == LinkEvent.Type.LINK_REMOVED;

        // but if the Link is an Edge type,
        // it will be the opposite
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

        // FIXME there's still chance that Topology and Device Service
        // view is out-of-sync
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
        return !topologyService.isInfrastructure(topology, point) &&
               !point.port().isLogical();
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
