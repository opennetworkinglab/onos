/*
 * Copyright 2014-present Open Networking Laboratory
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
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
import org.onlab.util.Tools;
import org.onosproject.cfg.ComponentConfigService;
import org.onosproject.incubator.net.intf.InterfaceService;
import org.onosproject.net.edge.EdgePortService;
import org.onosproject.net.provider.AbstractListenerProviderRegistry;
import org.onosproject.net.config.NetworkConfigEvent;
import org.onosproject.net.config.NetworkConfigListener;
import org.onosproject.net.config.NetworkConfigService;
import org.onosproject.net.config.basics.BasicHostConfig;
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
import org.onosproject.net.packet.PacketService;
import org.onosproject.net.provider.AbstractProviderService;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;

import java.util.Dictionary;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static org.onosproject.security.AppGuard.checkPermission;
import static org.slf4j.LoggerFactory.getLogger;
import static org.onosproject.security.AppPermission.Type.*;

/**
 * Provides basic implementation of the host SB &amp; NB APIs.
 */
@Component(immediate = true)
@Service
public class HostManager
        extends AbstractListenerProviderRegistry<HostEvent, HostListener, HostProvider, HostProviderService>
        implements HostService, HostAdminService, HostProviderRegistry {

    private final Logger log = getLogger(getClass());

    public static final String HOST_ID_NULL = "Host ID cannot be null";

    private final NetworkConfigListener networkConfigListener = new InternalNetworkConfigListener();

    private HostStoreDelegate delegate = new InternalStoreDelegate();

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected HostStore store;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected PacketService packetService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected NetworkConfigService networkConfigService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected InterfaceService interfaceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected EdgePortService edgePortService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ComponentConfigService cfgService;

    @Property(name = "allowDuplicateIps", boolValue = true,
            label = "Enable removal of duplicate ip address")

    private boolean allowDuplicateIps = true;

    @Property(name = "monitorHosts", boolValue = false,
            label = "Enable/Disable monitoring of hosts")
    private boolean monitorHosts = false;

    @Property(name = "probeRate", longValue = 30000,
            label = "Set the probe Rate in milli seconds")
    private long probeRate = 30000;

    private HostMonitor monitor;


    @Activate
    public void activate(ComponentContext context) {
        store.setDelegate(delegate);
        eventDispatcher.addSink(HostEvent.class, listenerRegistry);
        cfgService.registerProperties(getClass());
        networkConfigService.addListener(networkConfigListener);
        monitor = new HostMonitor(packetService, this, interfaceService, edgePortService);
        monitor.setProbeRate(probeRate);
        monitor.start();
        modified(context);
        cfgService.registerProperties(getClass());
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        store.unsetDelegate(delegate);
        eventDispatcher.removeSink(HostEvent.class);
        networkConfigService.removeListener(networkConfigListener);
        cfgService.unregisterProperties(getClass(), false);
        monitor.shutdown();
        log.info("Stopped");
    }

    @Modified
     public void modified(ComponentContext context) {
        boolean oldValue = monitorHosts;
        readComponentConfiguration(context);
        if (probeRate > 0) {
            monitor.setProbeRate(probeRate);
        } else {
            log.warn("probeRate cannot be lessthan 0");
        }

        if (oldValue != monitorHosts) {
            if (monitorHosts) {
                startMonitoring();
            } else {
                stopMonitoring();
            }
        }
    }

    /**
     * Extracts properties from the component configuration context.
     *
     * @param context the component context
     */
    private void readComponentConfiguration(ComponentContext context) {
        Dictionary<?, ?> properties = context.getProperties();
        Boolean flag;

        flag = Tools.isPropertyEnabled(properties, "monitorHosts");
        if (flag == null) {
            log.info("monitorHosts is not enabled " +
                             "using current value of {}", monitorHosts);
        } else {
            monitorHosts = flag;
            log.info("Configured. monitorHosts {}",
            monitorHosts ? "enabled" : "disabled");
        }

        Long longValue = Tools.getLongProperty(properties, "probeRate");
        if (longValue == null || longValue == 0) {
            log.info("probeRate is not set sing default value of {}", probeRate);
        } else {
            probeRate = longValue;
            log.info("Configured. probeRate {}", probeRate);
        }

        flag = Tools.isPropertyEnabled(properties, "allowDuplicateIps");
        if (flag == null) {
            log.info("Removal of duplicate ip address is not configured");
        } else {
            allowDuplicateIps = flag;
            log.info("Removal of duplicate ip address is {}",
                     allowDuplicateIps ? "disabled" : "enabled");
        }

    }

    /**
     * Starts monitoring the hosts by IP Address.
     *
     */
    private void startMonitoring() {
        store.getHosts().forEach(host -> {
                    host.ipAddresses().forEach(ip -> {
                           monitor.addMonitoringFor(ip);
            });
        });
    }

    /**
     * Stops monitoring the hosts by IP Address.
     *
     */
    private void stopMonitoring() {
        store.getHosts().forEach(host -> {
                    host.ipAddresses().forEach(ip -> {
                           monitor.stopMonitoring(ip);
            });
        });
    }

    @Override
    protected HostProviderService createProviderService(HostProvider provider) {
        monitor.registerHostProvider(provider);
        return new InternalHostProviderService(provider);
    }

    @Override
    public int getHostCount() {
        checkPermission(HOST_READ);
        return store.getHostCount();
    }

    @Override
    public Iterable<Host> getHosts() {
        checkPermission(HOST_READ);
        return store.getHosts();
    }

    @Override
    public Host getHost(HostId hostId) {
        checkPermission(HOST_READ);
        checkNotNull(hostId, HOST_ID_NULL);
        return store.getHost(hostId);
    }

    @Override
    public Set<Host> getHostsByVlan(VlanId vlanId) {
        checkPermission(HOST_READ);
        return store.getHosts(vlanId);
    }

    @Override
    public Set<Host> getHostsByMac(MacAddress mac) {
        checkPermission(HOST_READ);
        checkNotNull(mac, "MAC address cannot be null");
        return store.getHosts(mac);
    }

    @Override
    public Set<Host> getHostsByIp(IpAddress ip) {
        checkPermission(HOST_READ);
        checkNotNull(ip, "IP address cannot be null");
        return store.getHosts(ip);
    }

    @Override
    public Set<Host> getConnectedHosts(ConnectPoint connectPoint) {
        checkPermission(HOST_READ);
        checkNotNull(connectPoint, "Connection point cannot be null");
        return store.getConnectedHosts(connectPoint);
    }

    @Override
    public Set<Host> getConnectedHosts(DeviceId deviceId) {
        checkPermission(HOST_READ);
        checkNotNull(deviceId, "Device ID cannot be null");
        return store.getConnectedHosts(deviceId);
    }

    @Override
    public void startMonitoringIp(IpAddress ip) {
        checkPermission(HOST_EVENT);
        monitor.addMonitoringFor(ip);
    }

    @Override
    public void stopMonitoringIp(IpAddress ip) {
        checkPermission(HOST_EVENT);
        monitor.stopMonitoring(ip);
    }

    @Override
    public void requestMac(IpAddress ip) {
        // FIXME!!!! Auto-generated method stub
    }

    @Override
    public void removeHost(HostId hostId) {
        checkNotNull(hostId, HOST_ID_NULL);
        store.removeHost(hostId);
    }

    // Personalized host provider service issued to the supplied provider.
    private class InternalHostProviderService
            extends AbstractProviderService<HostProvider>
            implements HostProviderService {
        InternalHostProviderService(HostProvider provider) {
            super(provider);
        }

        @Override
        public void hostDetected(HostId hostId, HostDescription hostDescription, boolean replaceIps) {
            checkNotNull(hostId, HOST_ID_NULL);
            checkValidity();
            hostDescription = validateHost(hostDescription, hostId);

            if (!allowDuplicateIps) {
                removeDuplicates(hostId, hostDescription);
            }
            store.createOrUpdateHost(provider().id(), hostId,
                                     hostDescription, replaceIps);

            if (monitorHosts) {
                hostDescription.ipAddress().forEach(ip -> {
                    monitor.addMonitoringFor(ip);
                });
            }
        }

        // When a new IP is detected, remove that IP on other hosts if it exists
        public void removeDuplicates(HostId hostId, HostDescription desc) {
            desc.ipAddress().forEach(ip -> {
                Set<Host> allHosts = store.getHosts(ip);
                allHosts.forEach(eachHost -> {
                    if (!(eachHost.id().equals(hostId))) {
                        log.info("Duplicate ip {} found on host {} and {}", ip,
                                 hostId.toString(), eachHost.id().toString());
                        store.removeIp(eachHost.id(), ip);
                    }
                });
            });
        }




        // returns a HostDescription made from the union of the BasicHostConfig
        // annotations if it exists
        private HostDescription validateHost(HostDescription hostDescription, HostId hostId) {
            BasicHostConfig cfg = networkConfigService.getConfig(hostId, BasicHostConfig.class);
            checkState(cfg == null || cfg.isAllowed(), "Host {} is not allowed", hostId);

            return BasicHostOperator.combine(cfg, hostDescription);
        }

        @Override
        public void hostVanished(HostId hostId) {
            checkNotNull(hostId, HOST_ID_NULL);
            checkValidity();
            Host host = store.getHost(hostId);
            if (monitorHosts) {
                host.ipAddresses().forEach(ip -> {
                    monitor.stopMonitoring(ip);
                });
            }
            store.removeHost(hostId);
        }

        @Override
        public void removeIpFromHost(HostId hostId, IpAddress ipAddress) {
            checkNotNull(hostId, HOST_ID_NULL);
            checkValidity();
            store.removeIp(hostId, ipAddress);
        }
    }

    // Store delegate to re-post events emitted from the store.
    private class InternalStoreDelegate implements HostStoreDelegate {
        @Override
        public void notify(HostEvent event) {
            post(event);
        }
    }

    // listens for NetworkConfigEvents of type BasicHostConfig and removes
    // links that the config does not allow
    private class InternalNetworkConfigListener implements NetworkConfigListener {
        @Override
        public void event(NetworkConfigEvent event) {
            if ((event.type() == NetworkConfigEvent.Type.CONFIG_ADDED ||
                    event.type() == NetworkConfigEvent.Type.CONFIG_UPDATED) &&
                    event.configClass().equals(BasicHostConfig.class)) {
                log.debug("Detected host network config event {}", event.type());
                kickOutBadHost(((HostId) event.subject()));
            }
        }
    }

    // checks if the specified host is allowed by the BasicHostConfig
    // and if not, removes it
    private void kickOutBadHost(HostId hostId) {
        BasicHostConfig cfg = networkConfigService.getConfig(hostId, BasicHostConfig.class);
        if (cfg != null && !cfg.isAllowed()) {
            Host badHost = getHost(hostId);
            if (badHost != null) {
                removeHost(hostId);
            } else {
                log.info("Failed removal: Host {} does not exist", hostId);
            }
        }
    }
}
