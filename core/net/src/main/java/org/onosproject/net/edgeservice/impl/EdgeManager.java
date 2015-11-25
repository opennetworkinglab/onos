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
import org.onosproject.net.topology.Topology;
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

    private final Map<DeviceId, Set<ConnectPoint>> connectionPoints = Maps.newConcurrentMap();

    private final LinkListener linkListener = new InnerLinkListener();

    private final DeviceListener deviceListener = new InnerDeviceListener();

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected PacketService packetService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected TopologyService topologyService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
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
        eventDispatcher.removeSink(EdgePortEvent.class);
        deviceService.removeListener(deviceListener);
        linkService.removeListener(linkListener);
        log.info("Stopped");
    }

    @Override
    public boolean isEdgePoint(ConnectPoint point) {
        return !topologyService.isInfrastructure(topologyService.currentTopology(), point);
    }

    @Override
    public Iterable<ConnectPoint> getEdgePoints() {
        ImmutableSet.Builder<ConnectPoint> builder = ImmutableSet.builder();
        connectionPoints.forEach((k, v) -> v.forEach(builder::add));
        return builder.build();
    }

    @Override
    public Iterable<ConnectPoint> getEdgePoints(DeviceId deviceId) {
        ImmutableSet.Builder<ConnectPoint> builder = ImmutableSet.builder();
        Set<ConnectPoint> set = connectionPoints.get(deviceId);
        if (set != null) {
            set.forEach(builder::add);
        }
        return builder.build();
    }

    @Override
    public void emitPacket(ByteBuffer data, Optional<TrafficTreatment> treatment) {
        TrafficTreatment.Builder builder = treatment.isPresent() ?
                DefaultTrafficTreatment.builder(treatment.get()) :
                DefaultTrafficTreatment.builder();
        getEdgePoints().forEach(p -> packetService.emit(packet(builder, p, data)));
    }

    @Override
    public void emitPacket(DeviceId deviceId, ByteBuffer data,
                           Optional<TrafficTreatment> treatment) {
        TrafficTreatment.Builder builder = treatment.isPresent() ?
                DefaultTrafficTreatment.builder(treatment.get()) :
                DefaultTrafficTreatment.builder();
        getEdgePoints(deviceId).forEach(p -> packetService.emit(packet(builder, p, data)));
    }

    private OutboundPacket packet(TrafficTreatment.Builder builder, ConnectPoint point, ByteBuffer data) {
        builder.setOutput(point.port());
        return new DefaultOutboundPacket(point.deviceId(), builder.build(), data);
    }

    private class InnerLinkListener implements LinkListener {

        @Override
        public void event(LinkEvent event) {
            topology = topologyService.currentTopology();
            processLinkEvent(event);
        }
    }

    private class InnerDeviceListener implements DeviceListener {

        @Override
        public void event(DeviceEvent event) {
            topology = topologyService.currentTopology();
            processDeviceEvent(event);
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
        if (event.type() == LinkEvent.Type.LINK_ADDED) {
            removeEdgePort(event.subject().src());
            removeEdgePort(event.subject().dst());
        } else if (event.type() == LinkEvent.Type.LINK_REMOVED) {
            addEdgePort(event.subject().src());
            addEdgePort(event.subject().dst());
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
            // When device is removed or becomes unavailable, remove all its ports
            deviceService.getPorts(event.subject().id())
                    .forEach(p -> removeEdgePort(new ConnectPoint(id, p.number())));
            connectionPoints.remove(id);

        } else if (type == DeviceEvent.Type.PORT_ADDED ||
                type == PORT_UPDATED && event.port().isEnabled()) {
            addEdgePort(new ConnectPoint(id, event.port().number()));
        } else if (type == DeviceEvent.Type.PORT_REMOVED ||
                type == PORT_UPDATED && !event.port().isEnabled()) {
            removeEdgePort(new ConnectPoint(id, event.port().number()));
        }
    }

    // Adds the specified connection point to the edge points if needed.
    private void addEdgePort(ConnectPoint point) {
        if (!topologyService.isInfrastructure(topology, point) && !point.port().isLogical()) {
            Set<ConnectPoint> set = connectionPoints.get(point.deviceId());
            if (set == null) {
                set = Sets.newConcurrentHashSet();
                connectionPoints.put(point.deviceId(), set);
            }
            if (set.add(point)) {
                post(new EdgePortEvent(EDGE_PORT_ADDED, point));
            }
        }
    }

    // Removes the specified connection point from the edge points.
    private void removeEdgePort(ConnectPoint point) {
        if (!point.port().isLogical()) {
            Set<ConnectPoint> set = connectionPoints.get(point.deviceId());
            if (set == null) {
                return;
            }
            if (set.remove(point)) {
                post(new EdgePortEvent(EDGE_PORT_REMOVED, point));
            }
            if (set.isEmpty()) {
                connectionPoints.remove(point.deviceId());
            }
        }
    }
}
