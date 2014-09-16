package org.onlab.onos.net.trivial.host.impl;

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
import org.onlab.onos.net.provider.AbstractProviderRegistry;
import org.onlab.onos.net.provider.AbstractProviderService;
import org.onlab.packet.IPAddress;
import org.onlab.packet.MACAddress;
import org.onlab.packet.VLANID;
import org.slf4j.Logger;

import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Provides basic implementation of the host SB &amp; NB APIs.
 */
@Component(immediate = true)
@Service
public class SimpleHostManager
        extends AbstractProviderRegistry<HostProvider, HostProviderService>
        implements HostService, HostAdminService, HostProviderRegistry {

    public static final String HOST_ID_NULL = "Host ID cannot be null";
    private final Logger log = getLogger(getClass());

    private final AbstractListenerRegistry<HostEvent, HostListener>
            listenerRegistry = new AbstractListenerRegistry<>();

    private final SimpleHostStore store = new SimpleHostStore();

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
    public Set<Host> getHostsByVlan(VLANID vlanId) {
        return store.getHosts(vlanId);
    }

    @Override
    public Set<Host> getHostsByMac(MACAddress mac) {
        checkNotNull(mac, "MAC address cannot be null");
        return store.getHosts(mac);
    }

    @Override
    public Set<Host> getHostsByIp(IPAddress ip) {
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
