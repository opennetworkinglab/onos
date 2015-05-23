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
package org.onosproject.net.host.impl;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
import org.onosproject.core.Permission;
import org.onosproject.event.EventDeliveryService;
import org.onosproject.event.ListenerRegistry;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Host;
import org.onosproject.net.HostId;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.host.HostAdminService;
import org.onosproject.net.host.HostDescription;
import org.onosproject.net.host.HostEvent;
import org.onosproject.net.host.HostListener;
import org.onosproject.net.host.HostProvider;
import org.onosproject.net.host.HostProviderRegistry;
import org.onosproject.net.host.HostProviderService;
import org.onosproject.net.host.HostService;
import org.onosproject.net.host.HostStore;
import org.onosproject.net.host.HostStoreDelegate;
import org.onosproject.net.host.PortAddresses;
import org.onosproject.net.packet.PacketService;
import org.onosproject.net.provider.AbstractProviderRegistry;
import org.onosproject.net.provider.AbstractProviderService;
import org.slf4j.Logger;

import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.slf4j.LoggerFactory.getLogger;
import static org.onosproject.security.AppGuard.checkPermission;


/**
 * Provides basic implementation of the host SB &amp; NB APIs.
 */
@Component(immediate = true)
@Service
public class HostManager
        extends AbstractProviderRegistry<HostProvider, HostProviderService>
        implements HostService, HostAdminService, HostProviderRegistry {

    public static final String HOST_ID_NULL = "Host ID cannot be null";
    private final Logger log = getLogger(getClass());

    private final ListenerRegistry<HostEvent, HostListener>
            listenerRegistry = new ListenerRegistry<>();

    private HostStoreDelegate delegate = new InternalStoreDelegate();

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected HostStore store;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected EventDeliveryService eventDispatcher;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected PacketService packetService;

    private HostMonitor monitor;

    @Activate
    public void activate() {
        log.info("Started");
        store.setDelegate(delegate);
        eventDispatcher.addSink(HostEvent.class, listenerRegistry);

        monitor = new HostMonitor(deviceService,  packetService, this);
        monitor.start();
    }

    @Deactivate
    public void deactivate() {
        store.unsetDelegate(delegate);
        eventDispatcher.removeSink(HostEvent.class);
        log.info("Stopped");
    }

    @Override
    protected HostProviderService createProviderService(HostProvider provider) {
        monitor.registerHostProvider(provider);

        return new InternalHostProviderService(provider);
    }

    @Override
    public int getHostCount() {
        checkPermission(Permission.HOST_READ);

        return store.getHostCount();
    }

    @Override
    public Iterable<Host> getHosts() {
        checkPermission(Permission.HOST_READ);

        return store.getHosts();
    }

    @Override
    public Host getHost(HostId hostId) {
        checkPermission(Permission.HOST_READ);

        checkNotNull(hostId, HOST_ID_NULL);
        return store.getHost(hostId);
    }

    @Override
    public Set<Host> getHostsByVlan(VlanId vlanId) {
        checkPermission(Permission.HOST_READ);

        return store.getHosts(vlanId);
    }

    @Override
    public Set<Host> getHostsByMac(MacAddress mac) {
        checkPermission(Permission.HOST_READ);

        checkNotNull(mac, "MAC address cannot be null");
        return store.getHosts(mac);
    }

    @Override
    public Set<Host> getHostsByIp(IpAddress ip) {
        checkPermission(Permission.HOST_READ);

        checkNotNull(ip, "IP address cannot be null");
        return store.getHosts(ip);
    }

    @Override
    public Set<Host> getConnectedHosts(ConnectPoint connectPoint) {
        checkPermission(Permission.HOST_READ);

        checkNotNull(connectPoint, "Connection point cannot be null");
        return store.getConnectedHosts(connectPoint);
    }

    @Override
    public Set<Host> getConnectedHosts(DeviceId deviceId) {
        checkPermission(Permission.HOST_READ);

        checkNotNull(deviceId, "Device ID cannot be null");
        return store.getConnectedHosts(deviceId);
    }

    @Override
    public void startMonitoringIp(IpAddress ip) {
        checkPermission(Permission.HOST_EVENT);

        monitor.addMonitoringFor(ip);
    }

    @Override
    public void stopMonitoringIp(IpAddress ip) {
        checkPermission(Permission.HOST_EVENT);

        monitor.stopMonitoring(ip);
    }

    @Override
    public void requestMac(IpAddress ip) {
        // TODO Auto-generated method stub
    }

    @Override
    public void addListener(HostListener listener) {
        checkPermission(Permission.HOST_EVENT);

        listenerRegistry.addListener(listener);
    }

    @Override
    public void removeListener(HostListener listener) {
        checkPermission(Permission.HOST_EVENT);

        listenerRegistry.removeListener(listener);
    }

    @Override
    public void removeHost(HostId hostId) {
        checkNotNull(hostId, HOST_ID_NULL);
        HostEvent event = store.removeHost(hostId);
        if (event != null) {
            post(event);
        }
    }

    @Override
    public void bindAddressesToPort(PortAddresses addresses) {
        store.updateAddressBindings(addresses);
    }

    @Override
    public void unbindAddressesFromPort(PortAddresses portAddresses) {
        store.removeAddressBindings(portAddresses);
    }

    @Override
    public void clearAddresses(ConnectPoint connectPoint) {
        store.clearAddressBindings(connectPoint);
    }

    @Override
    public Set<PortAddresses> getAddressBindings() {
        checkPermission(Permission.HOST_READ);

        return store.getAddressBindings();
    }

    @Override
    public Set<PortAddresses> getAddressBindingsForPort(ConnectPoint connectPoint) {
        checkPermission(Permission.HOST_READ);

        return store.getAddressBindingsForPort(connectPoint);
    }

    // Personalized host provider service issued to the supplied provider.
    private class InternalHostProviderService
            extends AbstractProviderService<HostProvider>
            implements HostProviderService {

        InternalHostProviderService(HostProvider provider) {
            super(provider);
        }

        @Override
        public void hostDetected(HostId hostId, HostDescription hostDescription) {
            checkNotNull(hostId, HOST_ID_NULL);
            checkValidity();
            HostEvent event = store.createOrUpdateHost(provider().id(), hostId,
                                                       hostDescription);
            if (event != null) {
                post(event);
            }
        }

        @Override
        public void hostVanished(HostId hostId) {
            checkNotNull(hostId, HOST_ID_NULL);
            checkValidity();
            HostEvent event = store.removeHost(hostId);
            if (event != null) {
                post(event);
            }
        }
    }

    // Posts the specified event to the local event dispatcher.
    private void post(HostEvent event) {
        if (event != null) {
            eventDispatcher.post(event);
        }
    }

    // Store delegate to re-post events emitted from the store.
    private class InternalStoreDelegate implements HostStoreDelegate {
        @Override
        public void notify(HostEvent event) {
            post(event);
        }
    }
}
