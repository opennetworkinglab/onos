/*
 * Copyright 2014-present Open Networking Foundation
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

import com.google.common.collect.Sets;
import org.onlab.packet.Ip6Address;
import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
import org.onlab.util.Tools;
import org.onosproject.cfg.ComponentConfigService;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Host;
import org.onosproject.net.HostId;
import org.onosproject.net.HostLocation;
import org.onosproject.net.config.Config;
import org.onosproject.net.config.NetworkConfigEvent;
import org.onosproject.net.config.NetworkConfigListener;
import org.onosproject.net.config.NetworkConfigService;
import org.onosproject.net.config.basics.BasicHostConfig;
import org.onosproject.net.config.basics.HostAnnotationConfig;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.edge.EdgePortService;
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
import org.onosproject.net.intf.Interface;
import org.onosproject.net.intf.InterfaceService;
import org.onosproject.net.packet.PacketService;
import org.onosproject.net.provider.AbstractListenerProviderRegistry;
import org.onosproject.net.provider.AbstractProviderService;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;

import java.util.Dictionary;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Strings.isNullOrEmpty;
import static org.onlab.packet.IPv6.getLinkLocalAddress;
import static org.onlab.util.Tools.get;
import static org.onosproject.net.OsgiPropertyConstants.HM_ALLOW_DUPLICATE_IPS;
import static org.onosproject.net.OsgiPropertyConstants.HM_ALLOW_DUPLICATE_IPS_DEFAULT;
import static org.onosproject.net.OsgiPropertyConstants.HM_GREEDY_LEARNING_IPV6;
import static org.onosproject.net.OsgiPropertyConstants.HM_GREEDY_LEARNING_IPV6_DEFAULT;
import static org.onosproject.net.OsgiPropertyConstants.HM_MONITOR_HOSTS;
import static org.onosproject.net.OsgiPropertyConstants.HM_MONITOR_HOSTS_DEFAULT;
import static org.onosproject.net.OsgiPropertyConstants.HM_PROBE_RATE;
import static org.onosproject.net.OsgiPropertyConstants.HM_PROBE_RATE_DEFAULT;
import static org.onosproject.net.OsgiPropertyConstants.HM_HOST_MOVED_THRESHOLD_IN_MILLIS;
import static org.onosproject.net.OsgiPropertyConstants.HM_HOST_MOVED_THRESHOLD_IN_MILLIS_DEFAULT;
import static org.onosproject.net.OsgiPropertyConstants.HM_HOST_MOVE_COUNTER;
import static org.onosproject.net.OsgiPropertyConstants.HM_HOST_MOVE_COUNTER_DEFAULT;
import static org.onosproject.net.OsgiPropertyConstants.HM_HOST_MOVE_TRACKER_ENABLE;
import static org.onosproject.net.OsgiPropertyConstants.HM_HOST_MOVE_TRACKER_ENABLE_DEFAULT;
import static org.onosproject.net.OsgiPropertyConstants.HM_OFFENDING_HOST_EXPIRY_IN_MINS;
import static org.onosproject.net.OsgiPropertyConstants.HM_OFFENDING_HOST_EXPIRY_IN_MINS_DEFAULT;
import static org.onosproject.net.OsgiPropertyConstants.HM_OFFENDING_HOST_THREADS_POOL_SIZE;
import static org.onosproject.net.OsgiPropertyConstants.HM_OFFENDING_HOST_THREADS_POOL_SIZE_DEFAULT;

import static org.onosproject.security.AppGuard.checkPermission;
import static org.onosproject.security.AppPermission.Type.HOST_EVENT;
import static org.onosproject.security.AppPermission.Type.HOST_READ;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Provides basic implementation of the host SB &amp; NB APIs.
 */
@Component(
        immediate = true,
        service = {
            HostService.class,
            HostAdminService.class,
            HostProviderRegistry.class
        },
        property = {
                HM_ALLOW_DUPLICATE_IPS + ":Boolean=" + HM_ALLOW_DUPLICATE_IPS_DEFAULT,
                HM_MONITOR_HOSTS + ":Boolean=" + HM_MONITOR_HOSTS_DEFAULT,
                HM_PROBE_RATE + ":Integer=" + HM_PROBE_RATE_DEFAULT,
                HM_GREEDY_LEARNING_IPV6 + ":Boolean=" + HM_GREEDY_LEARNING_IPV6_DEFAULT,
                HM_HOST_MOVE_TRACKER_ENABLE + ":Boolean=" + HM_HOST_MOVE_TRACKER_ENABLE_DEFAULT,
                HM_HOST_MOVED_THRESHOLD_IN_MILLIS + ":Integer=" + HM_HOST_MOVED_THRESHOLD_IN_MILLIS_DEFAULT,
                HM_HOST_MOVE_COUNTER + ":Integer=" + HM_HOST_MOVE_COUNTER_DEFAULT,
                HM_OFFENDING_HOST_EXPIRY_IN_MINS + ":Long=" + HM_OFFENDING_HOST_EXPIRY_IN_MINS_DEFAULT,
                HM_OFFENDING_HOST_THREADS_POOL_SIZE + ":Integer=" + HM_OFFENDING_HOST_THREADS_POOL_SIZE_DEFAULT


        }
)
public class HostManager
        extends AbstractListenerProviderRegistry<HostEvent, HostListener, HostProvider, HostProviderService>
        implements HostService, HostAdminService, HostProviderRegistry {

    private final Logger log = getLogger(getClass());

    public static final String HOST_ID_NULL = "Host ID cannot be null";

    private final NetworkConfigListener networkConfigListener = new InternalNetworkConfigListener();

    private HostStoreDelegate delegate = new InternalStoreDelegate();

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected HostStore store;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected PacketService packetService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected NetworkConfigService networkConfigService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected InterfaceService interfaceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected EdgePortService edgePortService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected ComponentConfigService cfgService;

    /** Enable removal of duplicate ip address. */
    private boolean allowDuplicateIps = HM_ALLOW_DUPLICATE_IPS_DEFAULT;

    /** Enable/Disable monitoring of hosts. */
    private boolean monitorHosts = HM_MONITOR_HOSTS_DEFAULT;

    /** Set the probe Rate in milli seconds. */
    private long probeRate = HM_PROBE_RATE_DEFAULT;

    /** Enable/Disable greedy learning of IPv6 link local address. */
    private boolean greedyLearningIpv6 = HM_GREEDY_LEARNING_IPV6_DEFAULT;

    /** Enable/Disable tracking of rogue host moves. */
    private boolean hostMoveTrackerEnabled = HM_HOST_MOVE_TRACKER_ENABLE_DEFAULT;

    /** Host move threshold in milli seconds. */
    private int hostMoveThresholdInMillis = HM_HOST_MOVED_THRESHOLD_IN_MILLIS_DEFAULT;

    /** If the host move happening within given threshold then increment the host move counter. */
    private int hostMoveCounter = HM_HOST_MOVE_COUNTER_DEFAULT;

    /** Max value of the counter after which the host will not be considered as offending host. */
    private long offendingHostExpiryInMins = HM_OFFENDING_HOST_EXPIRY_IN_MINS_DEFAULT;

    /** Default pool size of offending host clear executor thread. */
    private int offendingHostClearThreadPool = HM_OFFENDING_HOST_THREADS_POOL_SIZE_DEFAULT;

    private HostMonitor monitor;
    private HostAnnotationOperator hostAnnotationOperator;
    private ScheduledExecutorService offendingHostUnblockExecutor = null;
    private Map<HostId, HostMoveTracker> hostMoveTracker = new ConcurrentHashMap<>();


    @Activate
    public void activate(ComponentContext context) {
        hostAnnotationOperator = new HostAnnotationOperator(networkConfigService);
        store.setDelegate(delegate);
        eventDispatcher.addSink(HostEvent.class, listenerRegistry);
        cfgService.registerProperties(getClass());
        networkConfigService.addListener(networkConfigListener);
        monitor = new HostMonitor(packetService, this, interfaceService, edgePortService, deviceService);
        monitor.setProbeRate(probeRate);
        monitor.start();
        cfgService.registerProperties(getClass());
        modified(context);
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        store.unsetDelegate(delegate);
        eventDispatcher.removeSink(HostEvent.class);
        networkConfigService.removeListener(networkConfigListener);
        cfgService.unregisterProperties(getClass(), false);
        monitor.shutdown();
        if (offendingHostUnblockExecutor != null) {
            offendingHostUnblockExecutor.shutdown();
        }
        log.info("Stopped");
    }

    @Modified
    public void modified(ComponentContext context) {
        boolean oldValue = monitorHosts;
        readComponentConfiguration(context);
        if (probeRate > 0) {
            monitor.setProbeRate(probeRate);
        } else {
            log.warn("ProbeRate cannot be less than 0");
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
        int newHostMoveThresholdInMillis;
        int newHostMoveCounter;
        int newOffendinghostPoolSize;
        long newOffendingHostExpiryInMins;

        flag = Tools.isPropertyEnabled(properties, HM_MONITOR_HOSTS);
        if (flag == null) {
            log.info("monitorHosts is not enabled " +
                             "using current value of {}", monitorHosts);
        } else {
            monitorHosts = flag;
            log.info("Configured. monitorHosts {}",
                     monitorHosts ? "enabled" : "disabled");
        }

        Long longValue = Tools.getLongProperty(properties, HM_PROBE_RATE);
        if (longValue == null || longValue == 0) {
            log.info("probeRate is not set sing default value of {}", probeRate);
        } else {
            probeRate = longValue;
            log.info("Configured. probeRate {}", probeRate);
        }

        flag = Tools.isPropertyEnabled(properties, HM_ALLOW_DUPLICATE_IPS);
        if (flag == null) {
            log.info("Removal of duplicate ip address is not configured");
        } else {
            allowDuplicateIps = flag;
            log.info("Removal of duplicate ip address is {}",
                     allowDuplicateIps ? "disabled" : "enabled");
        }

        flag = Tools.isPropertyEnabled(properties, HM_GREEDY_LEARNING_IPV6);
        if (flag == null) {
            log.info("greedy learning is not enabled " +
                             "using current value of {}", greedyLearningIpv6);
        } else {
            greedyLearningIpv6 = flag;
            log.info("Configured. greedyLearningIpv6 {}",
                     greedyLearningIpv6 ? "enabled" : "disabled");
        }
        flag = Tools.isPropertyEnabled(properties, HM_HOST_MOVE_TRACKER_ENABLE);
        if (flag == null) {
            log.info("Host move tracker is not configured " +
                    "using current value of {}", hostMoveTrackerEnabled);
        } else {
            hostMoveTrackerEnabled = flag;
            log.info("Configured. hostMoveTrackerEnabled {}",
                    hostMoveTrackerEnabled ? "enabled" : "disabled");

            //On enable cfg ,sets default configuration vales added , else use the default values
            properties = context.getProperties();
            try {
                String s = get(properties, HM_HOST_MOVED_THRESHOLD_IN_MILLIS);
                newHostMoveThresholdInMillis = isNullOrEmpty(s) ?
                        hostMoveThresholdInMillis : Integer.parseInt(s.trim());

                s = get(properties, HM_HOST_MOVE_COUNTER);
                newHostMoveCounter = isNullOrEmpty(s) ? hostMoveCounter : Integer.parseInt(s.trim());

                s = get(properties, HM_OFFENDING_HOST_EXPIRY_IN_MINS);
                newOffendingHostExpiryInMins = isNullOrEmpty(s) ?
                        offendingHostExpiryInMins : Integer.parseInt(s.trim());

                s = get(properties, HM_OFFENDING_HOST_THREADS_POOL_SIZE);
                newOffendinghostPoolSize = isNullOrEmpty(s) ?
                        offendingHostClearThreadPool : Integer.parseInt(s.trim());
            } catch (NumberFormatException | ClassCastException e) {
                newHostMoveThresholdInMillis = HM_HOST_MOVED_THRESHOLD_IN_MILLIS_DEFAULT;
                newHostMoveCounter = HM_HOST_MOVE_COUNTER_DEFAULT;
                newOffendingHostExpiryInMins = HM_OFFENDING_HOST_EXPIRY_IN_MINS_DEFAULT;
                newOffendinghostPoolSize = HM_OFFENDING_HOST_THREADS_POOL_SIZE_DEFAULT;
            }
            if (newHostMoveThresholdInMillis != hostMoveThresholdInMillis) {
                hostMoveThresholdInMillis = newHostMoveThresholdInMillis;
            }
            if (newHostMoveCounter != hostMoveCounter) {
                hostMoveCounter = newHostMoveCounter;
            }
            if (newOffendingHostExpiryInMins != offendingHostExpiryInMins) {
                offendingHostExpiryInMins = newOffendingHostExpiryInMins;
            }
            if (hostMoveTrackerEnabled && offendingHostUnblockExecutor == null) {
                setupThreadPool();
            } else if (newOffendinghostPoolSize != offendingHostClearThreadPool
                    && offendingHostUnblockExecutor != null) {
                offendingHostClearThreadPool = newOffendinghostPoolSize;
                offendingHostUnblockExecutor.shutdown();
                offendingHostUnblockExecutor = null;
                setupThreadPool();
            } else if (!hostMoveTrackerEnabled && offendingHostUnblockExecutor != null) {
                offendingHostUnblockExecutor.shutdown();
                offendingHostUnblockExecutor = null;
            }
            if (newOffendinghostPoolSize != offendingHostClearThreadPool) {
                offendingHostClearThreadPool = newOffendinghostPoolSize;
            }

            log.debug("modified hostMoveThresholdInMillis: {}, hostMoveCounter: {}, " +
                            "offendingHostExpiryInMins: {} ", hostMoveThresholdInMillis,
                    hostMoveCounter, offendingHostExpiryInMins);
        }
    }

    private synchronized void setupThreadPool() {
        offendingHostUnblockExecutor = Executors.newScheduledThreadPool(offendingHostClearThreadPool);
    }

    /**
     * Starts monitoring the hosts by IP Address.
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
        return getConnectedHosts(connectPoint, false);
    }

    @Override
    public Set<Host> getConnectedHosts(ConnectPoint connectPoint, boolean matchAuxLocations) {
        checkPermission(HOST_READ);
        checkNotNull(connectPoint, "Connection point cannot be null");
        return store.getConnectedHosts(connectPoint, matchAuxLocations);
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
        public void hostDetected(HostId hostId, HostDescription initialHostDescription, boolean replaceIps) {
            log.debug("Host Detected {}, {}", hostId, initialHostDescription);
            HostDescription hostDescription = initialHostDescription;
            checkNotNull(hostId, HOST_ID_NULL);
            checkValidity();

            BasicHostConfig cfg = networkConfigService.getConfig(hostId, BasicHostConfig.class);
            if (!isAllowed(cfg)) {
                log.warn("Host {} is not allowed to be added into the contol domain", hostId);
                return;
            }

            hostDescription = BasicHostOperator.combine(cfg, initialHostDescription);

            if (!allowDuplicateIps) {
                removeDuplicates(hostId, hostDescription);
            }

            HostAnnotationConfig annoConfig = networkConfigService.getConfig(hostId, HostAnnotationConfig.class);
            if (annoConfig != null) {
                hostDescription = hostAnnotationOperator.combine(hostId, hostDescription, Optional.of(annoConfig));
            }

            if (!hostMoveTrackerEnabled) {
                store.createOrUpdateHost(provider().id(), hostId,
                        hostDescription, replaceIps);
            } else if (!shouldBlock(hostId, hostDescription.locations())) {
                log.debug("Host move is allowed for host with Id: {} ", hostId);
                store.createOrUpdateHost(provider().id(), hostId,
                        hostDescription, replaceIps);
            } else {
                log.info("Host move is NOT allowed for host with Id: {} , removing from host store ", hostId);
            }

            if (monitorHosts) {
                hostDescription.ipAddress().forEach(ip -> {
                    monitor.addMonitoringFor(ip);
                });
            }

            // Greedy learning of IPv6 host. We have to disable the greedy
            // learning of configured hosts. Validate hosts each time will
            // overwrite the learnt information with the configured information.
            if (greedyLearningIpv6) {
                // Auto-generation of the IPv6 link local address
                // using the mac address
                Ip6Address targetIp6Address = Ip6Address.valueOf(
                        getLinkLocalAddress(hostId.mac().toBytes())
                );
                // If we already know this guy we don't need to do other
                if (!hostDescription.ipAddress().contains(targetIp6Address)) {
                    Host host = store.getHost(hostId);
                    // Configured host, skip it.
                    if (host != null && host.configured()) {
                        return;
                    }
                    // Host does not exist in the store or the target is not known
                    if ((host == null || !host.ipAddresses().contains(targetIp6Address))) {
                        // Use DAD to probe if interface MAC is not specified
                        MacAddress probeMac = interfaceService.getInterfacesByPort(hostDescription.location())
                                .stream().map(Interface::mac).findFirst().orElse(MacAddress.ONOS);
                        Ip6Address probeIp = !probeMac.equals(MacAddress.ONOS) ?
                                Ip6Address.valueOf(getLinkLocalAddress(probeMac.toBytes())) :
                                Ip6Address.ZERO;
                        // We send a probe using the monitoring service
                        monitor.sendProbe(
                                hostDescription.location(),
                                targetIp6Address,
                                probeIp,
                                probeMac,
                                hostId.vlanId()
                        );
                    }
                }
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

        @Override
        public void hostVanished(HostId hostId) {
            checkNotNull(hostId, HOST_ID_NULL);
            checkValidity();
            // TODO SDFAB-718 rethink HostStore APIs to allow atomic operations
            Host host = store.getHost(hostId);

            if (!allowedToChange(hostId)) {
                log.info("Request to remove {} is ignored due to provider mismatch", hostId);
                return;
            }

            if (host == null) {
                log.info("Request to remove {} is ignored due to host not present in the store", hostId);
                return;
            }

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

            if (!allowedToChange(hostId)) {
                log.info("Request to remove {} from {} is ignored due to provider mismatch",
                        ipAddress, hostId);
                return;
            }

            store.removeIp(hostId, ipAddress);
        }

        @Override
        public void addLocationToHost(HostId hostId, HostLocation location) {
            checkNotNull(hostId, HOST_ID_NULL);
            checkValidity();

            if (!allowedToChange(hostId)) {
                log.info("Request to add {} to {} is ignored due to provider mismatch",
                        location, hostId);
                return;
            }

            store.appendLocation(hostId, location);
        }

        @Override
        public void removeLocationFromHost(HostId hostId, HostLocation location) {
            checkNotNull(hostId, HOST_ID_NULL);
            checkValidity();

            if (!allowedToChange(hostId)) {
                log.info("Request to remove {} from {} is ignored due to provider mismatch",
                        location, hostId);
                return;
            }

            store.removeLocation(hostId, location);
        }

        /**
         * Providers should only be able to remove a host that is provided by itself,
         * or a host that is not configured.
         */
        private boolean allowedToChange(HostId hostId) {
            Host host = store.getHost(hostId);
            return host == null || !host.configured() || host.providerId().equals(provider().id());
        }


        /**
         * Deny host move if happening within the threshold time,
         * track moved host to identify offending hosts.
         *
         * @param hostId    host identifier
         * @param locations host locations
         */
        private boolean shouldBlock(HostId hostId, Set<HostLocation> locations) {
            Host host = store.getHost(hostId);
            // If host is not present in host store means host added for hte first time.
            if (host != null) {
                if (host.suspended()) {
                    // Checks host is marked as offending in other onos cluster instance/local instance
                    log.debug("Host id {} is moving frequently hence host moving " +
                            "processing is ignored", hostId);
                    return true;
                }
            } else {
                //host added for the first time.
                return false;
            }
            HostMoveTracker hostMove = hostMoveTracker.computeIfAbsent(hostId, id -> new HostMoveTracker(locations));
            if (Sets.difference(hostMove.getLocations(), locations).isEmpty() &&
                    Sets.difference(locations, hostMove.getLocations()).isEmpty()) {
                log.debug("Not hostmove scenario: Host id: {}, Old Host Location: {}, New host Location: {}",
                        hostId, hostMove.getLocations(), locations);
                return false; // It is not a host move scenario
            } else if (hostMove.getCounter() >= hostMoveCounter && System.currentTimeMillis() - hostMove.getTimeStamp()
                    < hostMoveThresholdInMillis) {
                //Check host move is crossed the threshold, then to mark as offending Host
                log.debug("Host id {} is identified as offending host and entry is added in cache", hostId);
                hostMove.resetHostMoveTracker(locations);
                store.suspend(hostId);
                //Set host suspended flag to false after given offendingHostExpiryInMins
                offendingHostUnblockExecutor.schedule(new UnblockOffendingHost(hostId),
                        offendingHostExpiryInMins,
                        TimeUnit.MINUTES);
                return true;
            } else if (System.currentTimeMillis() - hostMove.getTimeStamp()
                    < hostMoveThresholdInMillis) {
                //Increment the host move count as hostmove occured within the hostMoveThresholdInMillis time
                hostMove.updateHostMoveTracker(locations);
                log.debug("Updated the tracker with the host move registered for host: {}", hostId);
            } else if (System.currentTimeMillis() - hostMove.getTimeStamp()
                    > hostMoveThresholdInMillis) {
                //Hostmove is happened after hostMoveThresholdInMillis time so remove from host tracker.
                hostMove.resetHostMoveTracker(locations);
                store.unsuspend(hostId);
                log.debug("Reset the tracker with the host move registered for host: {}", hostId);
            }
            return false;
        }

        // Set host suspended flag to false after given offendingHostExpiryInMins.
        private final class UnblockOffendingHost implements Runnable {
            private HostId hostId;

            UnblockOffendingHost(HostId hostId) {
                this.hostId = hostId;
            }

            @Override
            public void run() {
                // Set the host suspended flag to false
                try {
                    store.unsuspend(hostId);
                    log.debug("Host {}: Marked host as unsuspended", hostId);
                } catch (Exception ex) {
                    log.debug("Host {}: not present in host list", hostId);
                }
            }
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
        public boolean isRelevant(NetworkConfigEvent event) {
            return (event.type() == NetworkConfigEvent.Type.CONFIG_ADDED
                    || event.type() == NetworkConfigEvent.Type.CONFIG_UPDATED)
                    && (event.configClass().equals(BasicHostConfig.class)
                    || event.configClass().equals(HostAnnotationConfig.class));
        }

        @Override
        public void event(NetworkConfigEvent event) {
            log.debug("Detected host network config event {}", event.type());
            HostEvent he = null;

            HostId hostId = (HostId) event.subject();
            BasicHostConfig cfg =
                    networkConfigService.getConfig(hostId, BasicHostConfig.class);

            if (!isAllowed(cfg)) {
                kickOutBadHost(hostId);
            } else if (event.configClass().equals(BasicHostConfig.class)) {
                Host host = getHost(hostId);
                HostDescription desc =
                        (host == null) ? null : BasicHostOperator.descriptionOf(host);
                desc = BasicHostOperator.combine(cfg, desc);
                if (desc != null) {
                    he = store.createOrUpdateHost(host.providerId(), hostId, desc, false);
                }
            } else if (event.configClass().equals(HostAnnotationConfig.class)) {
                Host host = getHost(hostId);
                HostProvider hp = (host == null) ? null : getProvider(host.providerId());
                HostDescription desc = (host == null) ? null : BasicHostOperator.descriptionOf(host);
                Optional<Config> prevConfig = event.prevConfig();
                log.debug("Host annotations: {} prevconfig {} desc {}", hostId, prevConfig, desc);
                desc = hostAnnotationOperator.combine(hostId, desc, prevConfig);
                if (desc != null && hp != null) {
                    log.debug("Host annotations update - updated host description :{}", desc.toString());
                    he = store.createOrUpdateHost(hp.id(), hostId, desc, false);
                    if (he != null && he.subject() != null) {
                        log.debug("Host annotations update - Host Event : {}", he.subject().annotations());
                    }
                }
            }

            if (he != null) {
                post(he);
            }
        }
    }

    // by default allowed, otherwise check flag
    private boolean isAllowed(BasicHostConfig cfg) {
        return (cfg == null || cfg.isAllowed());
    }

    // removes the specified host, if it exists
    private void kickOutBadHost(HostId hostId) {
        Host badHost = getHost(hostId);
        if (badHost != null) {
            removeHost(hostId);
        }
    }
}
