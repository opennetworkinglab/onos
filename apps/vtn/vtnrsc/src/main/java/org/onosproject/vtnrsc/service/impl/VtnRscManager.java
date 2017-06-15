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
package org.onosproject.vtnrsc.service.impl;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onlab.util.KryoNamespace;
import org.onosproject.core.CoreService;
import org.onosproject.event.AbstractListenerManager;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Host;
import org.onosproject.net.HostId;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.host.HostService;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.EventuallyConsistentMap;
import org.onosproject.store.service.LogicalClockService;
import org.onosproject.store.service.StorageService;
import org.onosproject.vtnrsc.FixedIp;
import org.onosproject.vtnrsc.FloatingIp;
import org.onosproject.vtnrsc.FlowClassifier;
import org.onosproject.vtnrsc.PortChain;
import org.onosproject.vtnrsc.PortPair;
import org.onosproject.vtnrsc.PortPairGroup;
import org.onosproject.vtnrsc.PortPairId;
import org.onosproject.vtnrsc.Router;
import org.onosproject.vtnrsc.RouterId;
import org.onosproject.vtnrsc.RouterInterface;
import org.onosproject.vtnrsc.SegmentationId;
import org.onosproject.vtnrsc.Subnet;
import org.onosproject.vtnrsc.SubnetId;
import org.onosproject.vtnrsc.TenantId;
import org.onosproject.vtnrsc.TenantRouter;
import org.onosproject.vtnrsc.VirtualPort;
import org.onosproject.vtnrsc.VirtualPortId;
import org.onosproject.vtnrsc.event.VtnRscEvent;
import org.onosproject.vtnrsc.event.VtnRscEventFeedback;
import org.onosproject.vtnrsc.event.VtnRscListener;
import org.onosproject.vtnrsc.floatingip.FloatingIpEvent;
import org.onosproject.vtnrsc.floatingip.FloatingIpListener;
import org.onosproject.vtnrsc.floatingip.FloatingIpService;
import org.onosproject.vtnrsc.flowclassifier.FlowClassifierEvent;
import org.onosproject.vtnrsc.flowclassifier.FlowClassifierListener;
import org.onosproject.vtnrsc.flowclassifier.FlowClassifierService;
import org.onosproject.vtnrsc.portchain.PortChainEvent;
import org.onosproject.vtnrsc.portchain.PortChainListener;
import org.onosproject.vtnrsc.portchain.PortChainService;
import org.onosproject.vtnrsc.portpair.PortPairEvent;
import org.onosproject.vtnrsc.portpair.PortPairListener;
import org.onosproject.vtnrsc.portpair.PortPairService;
import org.onosproject.vtnrsc.portpairgroup.PortPairGroupEvent;
import org.onosproject.vtnrsc.portpairgroup.PortPairGroupListener;
import org.onosproject.vtnrsc.portpairgroup.PortPairGroupService;
import org.onosproject.vtnrsc.router.RouterEvent;
import org.onosproject.vtnrsc.router.RouterListener;
import org.onosproject.vtnrsc.router.RouterService;
import org.onosproject.vtnrsc.routerinterface.RouterInterfaceEvent;
import org.onosproject.vtnrsc.routerinterface.RouterInterfaceListener;
import org.onosproject.vtnrsc.routerinterface.RouterInterfaceService;
import org.onosproject.vtnrsc.service.VtnRscService;
import org.onosproject.vtnrsc.subnet.SubnetService;
import org.onosproject.vtnrsc.tenantnetwork.TenantNetworkService;
import org.onosproject.vtnrsc.virtualport.VirtualPortEvent;
import org.onosproject.vtnrsc.virtualport.VirtualPortListener;
import org.onosproject.vtnrsc.virtualport.VirtualPortService;
import org.slf4j.Logger;

/**
 * Provides implementation of the VtnRsc service.
 */
@Component(immediate = true)
@Service
public class VtnRscManager extends AbstractListenerManager<VtnRscEvent, VtnRscListener>
                           implements VtnRscService {
    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;
    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected StorageService storageService;
    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected LogicalClockService clockService;

    private final Logger log = getLogger(getClass());
    private FloatingIpListener floatingIpListener = new InnerFloatingIpListener();
    private RouterListener routerListener = new InnerRouterListener();
    private RouterInterfaceListener routerInterfaceListener = new InnerRouterInterfaceListener();
    private PortPairListener portPairListener = new InnerPortPairListener();
    private PortPairGroupListener portPairGroupListener = new InnerPortPairGroupListener();
    private FlowClassifierListener flowClassifierListener = new InnerFlowClassifierListener();
    private PortChainListener portChainListener = new InnerPortChainListener();
    private VirtualPortListener virtualPortListener = new InnerVirtualPortListener();

    private EventuallyConsistentMap<TenantId, SegmentationId> l3vniTenantMap;
    private EventuallyConsistentMap<TenantRouter, SegmentationId> l3vniTenantRouterMap;
    private EventuallyConsistentMap<TenantId, Set<DeviceId>> classifierOvsMap;
    private EventuallyConsistentMap<TenantId, Set<DeviceId>> sffOvsMap;

    private static final String IFACEID = "ifaceid";
    private static final String RUNNELOPTOPOIC = "tunnel-ops-ids";
    private static final String EVENT_NOT_NULL = "event cannot be null";
    private static final String TENANTID_NOT_NULL = "tenantId cannot be null";
    private static final String DEVICEID_NOT_NULL = "deviceId cannot be null";
    private static final String VIRTUALPORTID_NOT_NULL = "virtualPortId cannot be null";
    private static final String HOST_NOT_NULL = "host cannot be null";
    private static final String L3VNITENANTMAP = "l3vniTenantMap";
    private static final String L3VNITENANTROUTERMAP = "l3vniTenantRouterMap";
    private static final String CLASSIFIEROVSMAP = "classifierOvsMap";
    private static final String SFFOVSMAP = "sffOvsMap";

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected RouterService routerService;
    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected FloatingIpService floatingIpService;
    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected RouterInterfaceService routerInterfaceService;
    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected VirtualPortService virtualPortService;
    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected HostService hostService;
    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected SubnetService subnetService;
    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected TenantNetworkService tenantNetworkService;
    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceService deviceService;
    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected PortPairService portPairService;
    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected PortPairGroupService portPairGroupService;
    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected FlowClassifierService flowClassifierService;
    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected PortChainService portChainService;

    @Activate
    public void activate() {
        eventDispatcher.addSink(VtnRscEvent.class, listenerRegistry);
        floatingIpService.addListener(floatingIpListener);
        routerService.addListener(routerListener);
        routerInterfaceService.addListener(routerInterfaceListener);
        portPairService.addListener(portPairListener);
        portPairGroupService.addListener(portPairGroupListener);
        flowClassifierService.addListener(flowClassifierListener);
        portChainService.addListener(portChainListener);
        virtualPortService.addListener(virtualPortListener);

        KryoNamespace.Builder serializer = KryoNamespace.newBuilder()
                .register(KryoNamespaces.API)
                .register(TenantId.class, SegmentationId.class,
                          TenantRouter.class, RouterId.class);
        l3vniTenantMap = storageService
                .<TenantId, SegmentationId>eventuallyConsistentMapBuilder()
                .withName(L3VNITENANTMAP).withSerializer(serializer)
                .withTimestampProvider((k, v) -> clockService.getTimestamp())
                .build();

        l3vniTenantRouterMap = storageService
                .<TenantRouter, SegmentationId>eventuallyConsistentMapBuilder()
                .withName(L3VNITENANTROUTERMAP).withSerializer(serializer)
                .withTimestampProvider((k, v) -> clockService.getTimestamp())
                .build();

        classifierOvsMap = storageService
                .<TenantId, Set<DeviceId>>eventuallyConsistentMapBuilder()
                .withName(CLASSIFIEROVSMAP).withSerializer(serializer)
                .withTimestampProvider((k, v) -> clockService.getTimestamp())
                .build();

        sffOvsMap = storageService
                .<TenantId, Set<DeviceId>>eventuallyConsistentMapBuilder()
                .withName(SFFOVSMAP).withSerializer(serializer)
                .withTimestampProvider((k, v) -> clockService.getTimestamp())
                .build();
    }

    @Deactivate
    public void deactivate() {
        eventDispatcher.removeSink(VtnRscEvent.class);
        floatingIpService.removeListener(floatingIpListener);
        routerService.removeListener(routerListener);
        routerInterfaceService.removeListener(routerInterfaceListener);
        portPairService.removeListener(portPairListener);
        portPairGroupService.removeListener(portPairGroupListener);
        flowClassifierService.removeListener(flowClassifierListener);
        portChainService.removeListener(portChainListener);
        virtualPortService.removeListener(virtualPortListener);

        l3vniTenantMap.destroy();
        l3vniTenantRouterMap.destroy();
        classifierOvsMap.destroy();
        sffOvsMap.destroy();
        log.info("Stopped");
    }

    @Override
    public SegmentationId getL3vni(TenantId tenantId) {
        checkNotNull(tenantId, "tenantId cannot be null");
        SegmentationId l3vni = l3vniTenantMap.get(tenantId);
        if (l3vni == null) {
            long segmentationId = coreService.getIdGenerator(RUNNELOPTOPOIC)
                    .getNewId();
            l3vni = SegmentationId.segmentationId(String
                    .valueOf(segmentationId));
            l3vniTenantMap.put(tenantId, l3vni);
        }
        return l3vni;
    }

    @Override
    public SegmentationId getL3vni(TenantRouter tenantRouter) {
        checkNotNull(tenantRouter, "tenantRouter cannot be null");
        SegmentationId l3vni = l3vniTenantRouterMap.get(tenantRouter);
        if (l3vni == null) {
            long segmentationId = coreService.getIdGenerator(RUNNELOPTOPOIC)
                    .getNewId();
            l3vni = SegmentationId.segmentationId(String
                    .valueOf(segmentationId));
            l3vniTenantRouterMap.put(tenantRouter, l3vni);
        }
        return l3vni;
    }

    private class InnerFloatingIpListener implements FloatingIpListener {

        @Override
        public void event(FloatingIpEvent event) {
            checkNotNull(event, EVENT_NOT_NULL);
            FloatingIp floatingIp = event.subject();
            if (FloatingIpEvent.Type.FLOATINGIP_PUT == event.type()) {
                notifyListeners(new VtnRscEvent(
                                                VtnRscEvent.Type.FLOATINGIP_PUT,
                                                new VtnRscEventFeedback(
                                                                        floatingIp)));
            }
            if (FloatingIpEvent.Type.FLOATINGIP_DELETE == event.type()) {
                notifyListeners(new VtnRscEvent(
                                                VtnRscEvent.Type.FLOATINGIP_DELETE,
                                                new VtnRscEventFeedback(
                                                                        floatingIp)));
            }
            if (FloatingIpEvent.Type.FLOATINGIP_BIND == event.type()) {
                notifyListeners(new VtnRscEvent(
                                                VtnRscEvent.Type.FLOATINGIP_BIND,
                                                new VtnRscEventFeedback(
                                                                        floatingIp)));
            }
            if (FloatingIpEvent.Type.FLOATINGIP_UNBIND == event.type()) {
                notifyListeners(new VtnRscEvent(
                                                VtnRscEvent.Type.FLOATINGIP_UNBIND,
                                                new VtnRscEventFeedback(
                                                                        floatingIp)));
            }
        }
    }

    private class InnerRouterListener implements RouterListener {

        @Override
        public void event(RouterEvent event) {
            checkNotNull(event, EVENT_NOT_NULL);
            Router router = event.subject();
            if (RouterEvent.Type.ROUTER_PUT == event.type()) {
                notifyListeners(new VtnRscEvent(VtnRscEvent.Type.ROUTER_PUT,
                                                new VtnRscEventFeedback(router)));
            }
            if (RouterEvent.Type.ROUTER_DELETE == event.type()) {
                notifyListeners(new VtnRscEvent(VtnRscEvent.Type.ROUTER_DELETE,
                                                new VtnRscEventFeedback(router)));
            }
        }
    }

    private class InnerRouterInterfaceListener
            implements RouterInterfaceListener {

        @Override
        public void event(RouterInterfaceEvent event) {
            checkNotNull(event, EVENT_NOT_NULL);
            RouterInterface routerInterface = event.subject();
            if (RouterInterfaceEvent.Type.ROUTER_INTERFACE_PUT == event.type()) {
                notifyListeners(new VtnRscEvent(
                                                VtnRscEvent.Type.ROUTER_INTERFACE_PUT,
                                                new VtnRscEventFeedback(
                                                                        routerInterface)));
            }
            if (RouterInterfaceEvent.Type.ROUTER_INTERFACE_DELETE == event
                    .type()) {
                notifyListeners(new VtnRscEvent(
                                                VtnRscEvent.Type.ROUTER_INTERFACE_DELETE,
                                                new VtnRscEventFeedback(
                                                                        routerInterface)));
            }
        }
    }

    private class InnerPortPairListener implements PortPairListener {

        @Override
        public void event(PortPairEvent event) {
            checkNotNull(event, EVENT_NOT_NULL);
            PortPair portPair = event.subject();
            if (PortPairEvent.Type.PORT_PAIR_PUT == event.type()) {
                notifyListeners(new VtnRscEvent(VtnRscEvent.Type.PORT_PAIR_PUT,
                        new VtnRscEventFeedback(portPair)));
            } else if (PortPairEvent.Type.PORT_PAIR_DELETE == event.type()) {
                notifyListeners(new VtnRscEvent(
                        VtnRscEvent.Type.PORT_PAIR_DELETE,
                        new VtnRscEventFeedback(portPair)));
            } else if (PortPairEvent.Type.PORT_PAIR_UPDATE == event.type()) {
                notifyListeners(new VtnRscEvent(
                        VtnRscEvent.Type.PORT_PAIR_UPDATE,
                        new VtnRscEventFeedback(portPair)));
            }
        }
    }

    private class InnerPortPairGroupListener implements PortPairGroupListener {

        @Override
        public void event(PortPairGroupEvent event) {
            checkNotNull(event, EVENT_NOT_NULL);
            PortPairGroup portPairGroup = event.subject();
            if (PortPairGroupEvent.Type.PORT_PAIR_GROUP_PUT == event.type()) {
                notifyListeners(new VtnRscEvent(
                        VtnRscEvent.Type.PORT_PAIR_GROUP_PUT,
                        new VtnRscEventFeedback(portPairGroup)));
            } else if (PortPairGroupEvent.Type.PORT_PAIR_GROUP_DELETE == event.type()) {
                notifyListeners(new VtnRscEvent(
                        VtnRscEvent.Type.PORT_PAIR_GROUP_DELETE,
                        new VtnRscEventFeedback(portPairGroup)));
            } else if (PortPairGroupEvent.Type.PORT_PAIR_GROUP_UPDATE == event.type()) {
                notifyListeners(new VtnRscEvent(
                        VtnRscEvent.Type.PORT_PAIR_GROUP_UPDATE,
                        new VtnRscEventFeedback(portPairGroup)));
            }
        }
    }

    private class InnerFlowClassifierListener implements FlowClassifierListener {

        @Override
        public void event(FlowClassifierEvent event) {
            checkNotNull(event, EVENT_NOT_NULL);
            FlowClassifier flowClassifier = event.subject();
            if (FlowClassifierEvent.Type.FLOW_CLASSIFIER_PUT == event.type()) {
                notifyListeners(new VtnRscEvent(
                        VtnRscEvent.Type.FLOW_CLASSIFIER_PUT,
                        new VtnRscEventFeedback(flowClassifier)));
            } else if (FlowClassifierEvent.Type.FLOW_CLASSIFIER_DELETE == event.type()) {
                notifyListeners(new VtnRscEvent(
                        VtnRscEvent.Type.FLOW_CLASSIFIER_DELETE,
                        new VtnRscEventFeedback(flowClassifier)));
            } else if (FlowClassifierEvent.Type.FLOW_CLASSIFIER_UPDATE == event.type()) {
                notifyListeners(new VtnRscEvent(
                        VtnRscEvent.Type.FLOW_CLASSIFIER_UPDATE,
                        new VtnRscEventFeedback(flowClassifier)));
            }
        }
    }

    private class InnerPortChainListener implements PortChainListener {

        @Override
        public void event(PortChainEvent event) {
            checkNotNull(event, EVENT_NOT_NULL);
            PortChain portChain = event.subject();
            if (PortChainEvent.Type.PORT_CHAIN_PUT == event.type()) {
                notifyListeners(new VtnRscEvent(
                        VtnRscEvent.Type.PORT_CHAIN_PUT,
                        new VtnRscEventFeedback(portChain)));
            } else if (PortChainEvent.Type.PORT_CHAIN_DELETE == event.type()) {
                notifyListeners(new VtnRscEvent(
                        VtnRscEvent.Type.PORT_CHAIN_DELETE,
                        new VtnRscEventFeedback(portChain)));
            } else if (PortChainEvent.Type.PORT_CHAIN_UPDATE == event.type()) {
                notifyListeners(new VtnRscEvent(
                        VtnRscEvent.Type.PORT_CHAIN_UPDATE,
                        new VtnRscEventFeedback(portChain)));
            }
        }
    }

    private class InnerVirtualPortListener implements VirtualPortListener {

        @Override
        public void event(VirtualPortEvent event) {
            checkNotNull(event, EVENT_NOT_NULL);
            VirtualPort virtualPort = event.subject();
            if (VirtualPortEvent.Type.VIRTUAL_PORT_PUT == event.type()) {
                notifyListeners(new VtnRscEvent(VtnRscEvent.Type.VIRTUAL_PORT_PUT,
                                                new VtnRscEventFeedback(virtualPort)));
            } else if (VirtualPortEvent.Type.VIRTUAL_PORT_DELETE == event.type()) {
                notifyListeners(new VtnRscEvent(VtnRscEvent.Type.VIRTUAL_PORT_DELETE,
                                                new VtnRscEventFeedback(virtualPort)));
            }
        }
    }

    @Override
    public Iterator<Device> getClassifierOfTenant(TenantId tenantId) {
        checkNotNull(tenantId, TENANTID_NOT_NULL);
        Set<DeviceId> deviceIdSet = classifierOvsMap.get(tenantId);
        Set<Device> deviceSet = new HashSet<>();
        if (deviceIdSet != null) {
            for (DeviceId deviceId : deviceIdSet) {
                deviceSet.add(deviceService.getDevice(deviceId));
            }
        }
        return deviceSet.iterator();
    }

    @Override
    public Iterator<Device> getSffOfTenant(TenantId tenantId) {
        checkNotNull(tenantId, TENANTID_NOT_NULL);
        Set<DeviceId> deviceIdSet = sffOvsMap.get(tenantId);
        Set<Device> deviceSet = new HashSet<>();
        if (deviceIdSet != null) {
            for (DeviceId deviceId : deviceIdSet) {
                deviceSet.add(deviceService.getDevice(deviceId));
            }
        }
        return deviceSet.iterator();
    }

    @Override
    public MacAddress getGatewayMac(HostId hostId) {
        checkNotNull(hostId, "hostId cannot be null");
        Host host = hostService.getHost(hostId);
        String ifaceId = host.annotations().value(IFACEID);
        VirtualPortId hPortId = VirtualPortId.portId(ifaceId);
        VirtualPort hPort = virtualPortService.getPort(hPortId);
        SubnetId subnetId = hPort.fixedIps().iterator().next().subnetId();
        Subnet subnet = subnetService.getSubnet(subnetId);
        IpAddress gatewayIp = subnet.gatewayIp();
        Iterable<VirtualPort> virtualPorts = virtualPortService.getPorts();
        MacAddress macAddress = null;
        for (VirtualPort port : virtualPorts) {
            Set<FixedIp> fixedIpSet = port.fixedIps();
            for (FixedIp fixedIp : fixedIpSet) {
                if (fixedIp.ip().equals(gatewayIp)) {
                    macAddress = port.macAddress();
                }
            }
        }
        return macAddress;
    }

    @Override
    public boolean isServiceFunction(VirtualPortId portId) {
        return portPairService.exists(PortPairId.of(portId.portId()));
    }

    @Override
    public DeviceId getSfToSffMaping(VirtualPortId portId) {
        checkNotNull(portId, "portId cannot be null");
        VirtualPort vmPort = virtualPortService.getPort(portId);
        Set<Host> hostSet = hostService.getHostsByMac(vmPort.macAddress());
        for (Host host : hostSet) {
            if (host.annotations().value(IFACEID).equals(vmPort.portId().portId())) {
                return host.location().deviceId();
            }
        }
        return null;
    }

    @Override
    public void addDeviceIdOfOvsMap(VirtualPortId virtualPortId,
                                    TenantId tenantId, DeviceId deviceId) {
        checkNotNull(virtualPortId, VIRTUALPORTID_NOT_NULL);
        checkNotNull(tenantId, TENANTID_NOT_NULL);
        checkNotNull(deviceId, DEVICEID_NOT_NULL);
        if (isServiceFunction(virtualPortId)) {
            addDeviceIdToSpecificMap(tenantId, deviceId, sffOvsMap);
        } else {
            addDeviceIdToSpecificMap(tenantId, deviceId, classifierOvsMap);
        }
    }

    @Override
    public void removeDeviceIdOfOvsMap(Host host, TenantId tenantId, DeviceId deviceId) {
        checkNotNull(host, HOST_NOT_NULL);
        checkNotNull(tenantId, TENANTID_NOT_NULL);
        checkNotNull(deviceId, DEVICEID_NOT_NULL);
        if (isLastSFHostOfTenant(host, deviceId, tenantId)) {
            removeDeviceIdToSpecificMap(tenantId, deviceId, sffOvsMap);
        }
        if (isLastClassifierHostOfTenant(host, deviceId, tenantId)) {
            removeDeviceIdToSpecificMap(tenantId, deviceId, classifierOvsMap);
        }
    }

    /**
     * Checks whether the last Service Function host of a specific tenant in
     * this device.
     *
     * @param host the host on device
     * @param deviceId the device identifier
     * @param tenantId the tenant identifier
     * @return true or false
     */
    private boolean isLastSFHostOfTenant(Host host, DeviceId deviceId,
                                         TenantId tenantId) {
        Set<Host> hostSet = hostService.getConnectedHosts(deviceId);
        if (hostSet != null) {
            for (Host h : hostSet) {
                String ifaceId = h.annotations().value(IFACEID);
                if (ifaceId != null) {
                    VirtualPortId hPortId = VirtualPortId.portId(ifaceId);
                    if (virtualPortService.getPort(hPortId).tenantId().tenantId()
                            .equals(tenantId.tenantId())
                            && isServiceFunction(hPortId)) {
                        if (!h.equals(host)) {
                            return false;
                        }
                    }
                }
            }
        }
        return true;
    }

    /**
     * Checks whether the last Classifier host of a specific tenant in this
     * device.
     *
     * @param host the host on device
     * @param deviceId the device identifier
     * @param tenantId the tenant identifier
     * @return true or false
     */
    private boolean isLastClassifierHostOfTenant(Host host, DeviceId deviceId,
                                                 TenantId tenantId) {
        Set<Host> hostSet = hostService.getConnectedHosts(deviceId);
        if (hostSet != null) {
            for (Host h : hostSet) {
                String ifaceId = h.annotations().value(IFACEID);
                if (ifaceId != null) {
                    VirtualPortId hPortId = VirtualPortId.portId(ifaceId);
                    if (virtualPortService.getPort(hPortId).tenantId().tenantId()
                            .equals(tenantId.tenantId())
                            && !isServiceFunction(hPortId)) {
                        if (!h.equals(host)) {
                            return false;
                        }
                    }
                }
            }
        }
        return true;
    }

    /**
     * Adds specify Device identifier to OvsMap.
     *
     * @param tenantId the tenant identifier
     * @param deviceId the device identifier
     * @param ovsMap the instance of map to store device identifier
     */
    private void addDeviceIdToSpecificMap(TenantId tenantId,
                                     DeviceId deviceId,
                                     EventuallyConsistentMap<TenantId, Set<DeviceId>> ovsMap) {
        if (ovsMap.containsKey(tenantId)) {
            Set<DeviceId> deviceIdSet = ovsMap.get(tenantId);
            deviceIdSet.add(deviceId);
            ovsMap.put(tenantId, deviceIdSet);
        } else {
            Set<DeviceId> deviceIdSet = new HashSet<>();
            deviceIdSet.add(deviceId);
            ovsMap.put(tenantId, deviceIdSet);
        }
    }

    /**
     * Removes specify Device identifier from OvsMap.
     *
     * @param tenantId the tenant identifier
     * @param deviceId the device identifier
     * @param ovsMap the instance of map to store device identifier
     */
    private void removeDeviceIdToSpecificMap(TenantId tenantId,
                                        DeviceId deviceId,
                                        EventuallyConsistentMap<TenantId, Set<DeviceId>> ovsMap) {
        Set<DeviceId> deviceIdSet = ovsMap.get(tenantId);
        if (deviceIdSet != null && deviceIdSet.size() > 1) {
            deviceIdSet.remove(deviceId);
            ovsMap.put(tenantId, deviceIdSet);
        } else {
            ovsMap.remove(tenantId);
        }
    }

    /**
     * Notifies specify event to all listeners.
     *
     * @param event VtnRsc event
     */
    private void notifyListeners(VtnRscEvent event) {
        checkNotNull(event, EVENT_NOT_NULL);
        post(event);
    }
}
