/*
 * Copyright 2015 Open Networking Laboratory
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
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Host;
import org.onosproject.net.HostId;
import org.onosproject.net.host.HostEvent;
import org.onosproject.net.host.HostListener;
import org.onosproject.net.host.HostService;
import org.onosproject.net.device.DeviceService;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.EventuallyConsistentMap;
import org.onosproject.store.service.LogicalClockService;
import org.onosproject.store.service.StorageService;
import org.onosproject.vtnrsc.FixedIp;
import org.onosproject.vtnrsc.FloatingIp;
import org.onosproject.vtnrsc.Router;
import org.onosproject.vtnrsc.RouterInterface;
import org.onosproject.vtnrsc.SegmentationId;
import org.onosproject.vtnrsc.Subnet;
import org.onosproject.vtnrsc.SubnetId;
import org.onosproject.vtnrsc.TenantId;
import org.onosproject.vtnrsc.VirtualPort;
import org.onosproject.vtnrsc.VirtualPortId;
import org.onosproject.vtnrsc.event.VtnRscEvent;
import org.onosproject.vtnrsc.event.VtnRscEventFeedback;
import org.onosproject.vtnrsc.event.VtnRscListener;
import org.onosproject.vtnrsc.floatingip.FloatingIpEvent;
import org.onosproject.vtnrsc.floatingip.FloatingIpListener;
import org.onosproject.vtnrsc.floatingip.FloatingIpService;
import org.onosproject.vtnrsc.router.RouterEvent;
import org.onosproject.vtnrsc.router.RouterListener;
import org.onosproject.vtnrsc.router.RouterService;
import org.onosproject.vtnrsc.routerinterface.RouterInterfaceEvent;
import org.onosproject.vtnrsc.routerinterface.RouterInterfaceListener;
import org.onosproject.vtnrsc.routerinterface.RouterInterfaceService;
import org.onosproject.vtnrsc.service.VtnRscService;
import org.onosproject.vtnrsc.subnet.SubnetService;
import org.onosproject.vtnrsc.tenantnetwork.TenantNetworkService;
import org.onosproject.vtnrsc.virtualport.VirtualPortService;
import org.slf4j.Logger;

import com.google.common.collect.Sets;

/**
 * Provides implementation of the VtnRsc service.
 */
@Component(immediate = true)
@Service
public class VtnRscManager implements VtnRscService {
    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;
    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected StorageService storageService;
    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected LogicalClockService clockService;

    private final Logger log = getLogger(getClass());
    private final Set<VtnRscListener> listeners = Sets.newCopyOnWriteArraySet();
    private HostListener hostListener = new InnerHostListener();
    private FloatingIpListener floatingIpListener = new InnerFloatingIpListener();
    private RouterListener routerListener = new InnerRouterListener();
    private RouterInterfaceListener routerInterfaceListener = new InnerRouterInterfaceListener();

    private EventuallyConsistentMap<TenantId, SegmentationId> l3vniMap;
    private EventuallyConsistentMap<TenantId, Set<DeviceId>> classifierOvsMap;
    private EventuallyConsistentMap<TenantId, Set<DeviceId>> sffOvsMap;

    private static final String IFACEID = "ifaceid";
    private static final String RUNNELOPTOPOIC = "tunnel-ops-ids";
    private static final String LISTENER_NOT_NULL = "listener cannot be null";
    private static final String EVENT_NOT_NULL = "event cannot be null";
    private static final String TENANTID_NOT_NULL = "tenantId cannot be null";
    private static final String DEVICEID_NOT_NULL = "deviceId cannot be null";
    private static final String OVSMAP_NOT_NULL = "ovsMap cannot be null";
    private static final String L3VNIMAP = "l3vniMap";
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

    @Activate
    public void activate() {
        hostService.addListener(hostListener);
        floatingIpService.addListener(floatingIpListener);
        routerService.addListener(routerListener);
        routerInterfaceService.addListener(routerInterfaceListener);

        KryoNamespace.Builder serializer = KryoNamespace.newBuilder()
                .register(KryoNamespaces.API)
                .register(TenantId.class, DeviceId.class);
        l3vniMap = storageService
                .<TenantId, SegmentationId>eventuallyConsistentMapBuilder()
                .withName(L3VNIMAP).withSerializer(serializer)
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
        hostService.removeListener(hostListener);
        floatingIpService.removeListener(floatingIpListener);
        routerService.removeListener(routerListener);
        routerInterfaceService.removeListener(routerInterfaceListener);
        l3vniMap.destroy();
        classifierOvsMap.destroy();
        sffOvsMap.destroy();
        listeners.clear();
        log.info("Stopped");
    }

    @Override
    public void addListener(VtnRscListener listener) {
        checkNotNull(listener, LISTENER_NOT_NULL);
        listeners.add(listener);
    }

    @Override
    public void removeListener(VtnRscListener listener) {
        checkNotNull(listener, LISTENER_NOT_NULL);
        listeners.add(listener);
    }

    @Override
    public SegmentationId getL3vni(TenantId tenantId) {
        checkNotNull(tenantId, "tenantId cannot be null");
        SegmentationId l3vni = l3vniMap.get(tenantId);
        if (l3vni == null) {
            long segmentationId = coreService.getIdGenerator(RUNNELOPTOPOIC)
                    .getNewId();
            l3vni = SegmentationId.segmentationId(String
                    .valueOf(segmentationId));
            l3vniMap.put(tenantId, l3vni);
        }
        return l3vni;
    }

    private class InnerHostListener implements HostListener {

        @Override
        public void event(HostEvent event) {
            checkNotNull(event, EVENT_NOT_NULL);
            Host host = event.subject();
            String ifaceId = host.annotations().value(IFACEID);
            VirtualPortId hPortId = VirtualPortId.portId(ifaceId);
            TenantId tenantId = virtualPortService.getPort(hPortId).tenantId();
            DeviceId deviceId = host.location().deviceId();
            if (HostEvent.Type.HOST_ADDED == event.type()) {
                if (isServiceFunction(hPortId)) {
                    addDeviceIdOfOvsMap(tenantId, deviceId, sffOvsMap);
                } else {
                    addDeviceIdOfOvsMap(tenantId, deviceId, classifierOvsMap);
                }
            } else if (HostEvent.Type.HOST_REMOVED == event.type()) {
                if (isLastSFHostOfTenant(host, deviceId, tenantId)) {
                    removeDeviceIdOfOvsMap(tenantId, deviceId, sffOvsMap);
                }
                if (isLastClassifierHostOfTenant(host, deviceId, tenantId)) {
                    removeDeviceIdOfOvsMap(tenantId, deviceId, classifierOvsMap);
                }
            }
        }
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
    public Iterator<Device> getSFFOfTenant(TenantId tenantId) {
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
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public DeviceId getSFToSFFMaping(VirtualPortId portId) {
        checkNotNull(portId, "portId cannot be null");
        VirtualPort vmPort = virtualPortService.getPort(portId);
        Set<Host> hostSet = hostService.getHostsByMac(vmPort.macAddress());
        for (Host host : hostSet) {
            if (host.annotations().value(IFACEID).equals(vmPort.portId())) {
                return host.location().deviceId();
            }
        }
        return null;
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
        checkNotNull(host, "host cannot be null");
        checkNotNull(deviceId, DEVICEID_NOT_NULL);
        checkNotNull(tenantId, TENANTID_NOT_NULL);
        Set<Host> hostSet = hostService.getConnectedHosts(deviceId);
        for (Host h : hostSet) {
            String ifaceId = h.annotations().value(IFACEID);
            VirtualPortId hPortId = VirtualPortId.portId(ifaceId);
            if (virtualPortService.getPort(hPortId).tenantId() != tenantId) {
                hostSet.remove(h);
            } else {
                if (!isServiceFunction(hPortId)) {
                    hostSet.remove(h);
                }
            }
        }
        if (hostSet.size() == 1 && hostSet.contains(host)) {
            return true;
        }
        return false;
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
        checkNotNull(host, "host cannot be null");
        checkNotNull(deviceId, DEVICEID_NOT_NULL);
        checkNotNull(tenantId, TENANTID_NOT_NULL);
        Set<Host> hostSet = hostService.getConnectedHosts(deviceId);
        for (Host h : hostSet) {
            String ifaceId = h.annotations().value(IFACEID);
            VirtualPortId hPortId = VirtualPortId.portId(ifaceId);
            if (virtualPortService.getPort(hPortId).tenantId() != tenantId) {
                hostSet.remove(h);
            } else {
                if (isServiceFunction(hPortId)) {
                    hostSet.remove(h);
                }
            }
        }
        if (hostSet.size() == 1 && hostSet.contains(host)) {
            return true;
        }
        return false;
    }

    /**
     * Adds specify Device identifier to OvsMap.
     *
     * @param tenantId the tenant identifier
     * @param deviceId the device identifier
     * @param ovsMap the instance of map to store device identifier
     */
    private void addDeviceIdOfOvsMap(TenantId tenantId,
                                     DeviceId deviceId,
                                     EventuallyConsistentMap<TenantId, Set<DeviceId>> ovsMap) {
        checkNotNull(tenantId, TENANTID_NOT_NULL);
        checkNotNull(deviceId, DEVICEID_NOT_NULL);
        checkNotNull(ovsMap, OVSMAP_NOT_NULL);
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
    private void removeDeviceIdOfOvsMap(TenantId tenantId,
                                        DeviceId deviceId,
                                        EventuallyConsistentMap<TenantId, Set<DeviceId>> ovsMap) {
        checkNotNull(tenantId, TENANTID_NOT_NULL);
        checkNotNull(deviceId, DEVICEID_NOT_NULL);
        checkNotNull(ovsMap, OVSMAP_NOT_NULL);
        Set<DeviceId> deviceIdSet = ovsMap.get(tenantId);
        if (deviceIdSet.size() > 1) {
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
        listeners.forEach(listener -> listener.event(event));
    }
}
