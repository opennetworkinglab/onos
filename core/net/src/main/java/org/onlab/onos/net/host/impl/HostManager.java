package org.onlab.onos.net.host.impl;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.Set;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.onos.event.AbstractListenerRegistry;
import org.onlab.onos.event.EventDeliveryService;
import org.onlab.onos.net.ConnectPoint;
import org.onlab.onos.net.DeviceId;
import org.onlab.onos.net.Host;
import org.onlab.onos.net.HostId;
import org.onlab.onos.net.host.HostAdminService;
import org.onlab.onos.net.host.HostDescription;
import org.onlab.onos.net.host.HostEvent;
import org.onlab.onos.net.host.HostListener;
import org.onlab.onos.net.host.HostProvider;
import org.onlab.onos.net.host.HostProviderRegistry;
import org.onlab.onos.net.host.HostProviderService;
import org.onlab.onos.net.host.HostService;
import org.onlab.onos.net.host.HostStore;
import org.onlab.onos.net.host.PortAddresses;
import org.onlab.onos.net.provider.AbstractProviderRegistry;
import org.onlab.onos.net.provider.AbstractProviderService;
import org.onlab.packet.IpAddress;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
import org.slf4j.Logger;

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

    private final AbstractListenerRegistry<HostEvent, HostListener>
            listenerRegistry = new AbstractListenerRegistry<>();

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected HostStore store;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected EventDeliveryService eventDispatcher;


    @Activate
    public void activate() {
        eventDispatcher.addSink(HostEvent.class, listenerRegistry);
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        eventDispatcher.removeSink(HostEvent.class);
        log.info("Stopped");
    }

    @Override
    protected HostProviderService createProviderService(HostProvider provider) {
        return new InternalHostProviderService(provider);
    }

    @Override
    public int getHostCount() {
        return store.getHostCount();
    }

    @Override
    public Iterable<Host> getHosts() {
        return store.getHosts();
    }

    @Override
    public Host getHost(HostId hostId) {
        checkNotNull(hostId, HOST_ID_NULL);
        return store.getHost(hostId);
    }

    @Override
    public Set<Host> getHostsByVlan(VlanId vlanId) {
        return store.getHosts(vlanId);
    }

    @Override
    public Set<Host> getHostsByMac(MacAddress mac) {
        checkNotNull(mac, "MAC address cannot be null");
        return store.getHosts(mac);
    }

    @Override
    public Set<Host> getHostsByIp(IpPrefix ip) {
        checkNotNull(ip, "IP address cannot be null");
        return store.getHosts(ip);
    }

    @Override
    public Set<Host> getConnectedHosts(ConnectPoint connectPoint) {
        checkNotNull(connectPoint, "Connection point cannot be null");
        return store.getConnectedHosts(connectPoint);
    }

    @Override
    public Set<Host> getConnectedHosts(DeviceId deviceId) {
        checkNotNull(deviceId, "Device ID cannot be null");
        return store.getConnectedHosts(deviceId);
    }

    @Override
    public void startMonitoringIp(IpAddress ip) {
        // TODO pass through to HostMonitor
    }

    @Override
    public void stopMonitoringIp(IpAddress ip) {
        // TODO pass through to HostMonitor
    }

    @Override
    public void requestMac(IpAddress ip) {
        // TODO Auto-generated method stub
    }

    @Override
    public void addListener(HostListener listener) {
        listenerRegistry.addListener(listener);
    }

    @Override
    public void removeListener(HostListener listener) {
        listenerRegistry.removeListener(listener);
    }

    @Override
    public void removeHost(HostId hostId) {
        checkNotNull(hostId, HOST_ID_NULL);
        HostEvent event = store.removeHost(hostId);
        if (event != null) {
            log.info("Host {} administratively removed", hostId);
            post(event);
        }
    }

    @Override
    public void bindAddressesToPort(IpAddress ip, MacAddress mac,
            ConnectPoint connectPoint) {
        // TODO Auto-generated method stub

    }

    @Override
    public void unbindAddressesFromPort(ConnectPoint connectPoint) {
        // TODO Auto-generated method stub

    }

    @Override
    public Set<PortAddresses> getAddressBindings() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public PortAddresses getAddressBindingsForPort(ConnectPoint connectPoint) {
        // TODO Auto-generated method stub
        return null;
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
                log.debug("Host {} detected", hostId);
                post(event);
            }
        }

        @Override
        public void hostVanished(HostId hostId) {
            checkNotNull(hostId, HOST_ID_NULL);
            checkValidity();
            HostEvent event = store.removeHost(hostId);
            if (event != null) {
                log.debug("Host {} vanished", hostId);
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

}
