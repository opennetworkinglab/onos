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
import org.onosproject.event.Event;
import org.onosproject.event.EventDeliveryService;
import org.onosproject.event.ListenerRegistry;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.onosproject.net.edge.EdgePortEvent.Type.EDGE_PORT_ADDED;
import static org.onosproject.net.edge.EdgePortEvent.Type.EDGE_PORT_REMOVED;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * This is an implementation of the edge net service.
 */
@Component(immediate = true)
@Service
public class EdgeManager implements EdgePortService {

    private final ListenerRegistry<EdgePortEvent, EdgePortListener>
            listenerRegistry = new ListenerRegistry<>();

    private final Logger log = getLogger(getClass());

    private Topology topology;

    private final TopologyListener topologyListener = new InnerTopologyListener();

    private final Map<DeviceId, Set<ConnectPoint>> connectionPoints = Maps.newConcurrentMap();

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected EventDeliveryService eventDispatcher;

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
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        eventDispatcher.removeSink(EdgePortEvent.class);
        topologyService.removeListener(topologyListener);
        log.info("Stopped");
    }

    public boolean isEdgePoint(ConnectPoint point) {
        return !topologyService.isInfrastructure(topologyService.currentTopology(), point);
    }

    public Iterable<ConnectPoint> getEdgePoints() {
        //TODO if this is called before any notifications need to populate structure
        ImmutableSet.Builder<ConnectPoint> builder = ImmutableSet.builder();
        connectionPoints.forEach((k, v) -> v.forEach(builder::add));
        return builder.build();
    }

    public Iterable<ConnectPoint> getEdgePoints(DeviceId deviceId) {
        //TODO if this is called before any notifications need to populate structure
        ImmutableSet.Builder<ConnectPoint> builder = ImmutableSet.builder();
        Set<ConnectPoint> set = connectionPoints.get(deviceId);
        if (set != null) {
            set.forEach(builder::add);
        }
        return builder.build();
    }

    public void emitPacket(ByteBuffer data, Optional<TrafficTreatment> treatment) {
        TrafficTreatment.Builder builder = treatment.isPresent() ?
                DefaultTrafficTreatment.builder(treatment.get()) :
                DefaultTrafficTreatment.builder();
        getEdgePoints().forEach(p -> packetService.emit(packet(builder, p, data)));
    }

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

    public void addListener(EdgePortListener listener) {
        listenerRegistry.addListener(listener);
    }

    public void removeListener(EdgePortListener listener) {
        listenerRegistry.removeListener(listener);
    }


    private class InnerTopologyListener implements TopologyListener {
        @Override
        public void event(TopologyEvent event) {
            topology = event.subject();
            List<Event> triggers = event.reasons();
            if (triggers != null) {
                triggers.forEach(reason -> {
                    if (reason instanceof DeviceEvent) {
                        //TODO spuriously catches events not handled in the handler method
                        processDeviceEvent((DeviceEvent) reason);
                    } else if (reason instanceof LinkEvent) {
                        processLinkEvent((LinkEvent) reason);
                    } else {
                        System.out.println(reason.toString());
                    }
                });
            } else {
                loadAllEdgePorts();
            }
        }
    }


    private void loadAllEdgePorts() {
        deviceService.getDevices().forEach(d -> deviceService.getPorts(d.id())
                .forEach(p -> addEdgePort(new ConnectPoint(d.id(), p.number()))));
    }

    private void processLinkEvent(LinkEvent event) {
        if (event.type() == LinkEvent.Type.LINK_ADDED) {
            removeEdgePort(event.subject().src());
            removeEdgePort(event.subject().dst());
        } else if (event.type() == LinkEvent.Type.LINK_REMOVED) {
            addEdgePort(event.subject().src());
            addEdgePort(event.subject().dst());
        }

    }

    private void processDeviceEvent(DeviceEvent event) {

        if (event.type() == DeviceEvent.Type.PORT_ADDED) {
            addEdgePort(new ConnectPoint(event.subject().id(), event.port().number()));
        } else if (event.type() == DeviceEvent.Type.PORT_REMOVED) {
            removeEdgePort(new ConnectPoint(event.subject().id(), event.port().number()));
        }
    }

    private void addEdgePort(ConnectPoint point) {
        //TODO case of link removed and one of the end ports removed in same topo cycle
        //TODO pt2. resulting behavior will be adding a non-existent edge to the set
        if (!topologyService.isInfrastructure(topology, point)) {
            Set<ConnectPoint> set = connectionPoints.get(point.deviceId());
            if (set == null) {
                set = Sets.newConcurrentHashSet();
                connectionPoints.put(point.deviceId(), set);
            }
            if (set.add(point)) {
                eventDispatcher.post(new EdgePortEvent(EDGE_PORT_ADDED, point));
            }
        }

    }

    private void removeEdgePort(ConnectPoint point) {
        //TODO need to check that points still exist IE when a link and port are removed
        //TODO pt2 and both events are captures in the same topo update
        if (!topologyService.isInfrastructure(topology, point)) {
            Set<ConnectPoint> set = connectionPoints.get(point.deviceId());
            if (set == null) {
                return;
            }
            if (set.remove(point)) {
                eventDispatcher.post(new EdgePortEvent(EDGE_PORT_REMOVED, point));
            }
            if (set.isEmpty()) {
                connectionPoints.remove(point.deviceId());
            }
        }

    }
}
