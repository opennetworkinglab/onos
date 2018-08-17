/*
 * Copyright 2016-present Open Networking Foundation
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
package org.onosproject.pim.impl;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import org.onlab.util.SafeRecurringTask;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.Host;
import org.onosproject.net.config.ConfigFactory;
import org.onosproject.net.config.NetworkConfigEvent;
import org.onosproject.net.config.NetworkConfigListener;
import org.onosproject.net.config.NetworkConfigRegistry;
import org.onosproject.net.config.basics.SubjectFactories;
import org.onosproject.net.host.HostService;
import org.onosproject.net.intf.Interface;
import org.onosproject.net.intf.InterfaceEvent;
import org.onosproject.net.intf.InterfaceListener;
import org.onosproject.net.intf.InterfaceService;
import org.onosproject.net.mcast.McastEvent;
import org.onosproject.net.mcast.McastListener;
import org.onosproject.net.mcast.McastRoute;
import org.onosproject.net.mcast.MulticastRouteService;
import org.onosproject.net.packet.PacketService;
import org.onosproject.routeservice.Route;
import org.onosproject.routeservice.RouteService;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Manages PIMInterfaces.
 *
 * TODO: Do we need to add a ServiceListener?
 */
@Component(immediate = true, service = PimInterfaceService.class)
public class PimInterfaceManager implements PimInterfaceService {

    private final Logger log = getLogger(getClass());

    private static final Class<PimInterfaceConfig> PIM_INTERFACE_CONFIG_CLASS = PimInterfaceConfig.class;
    private static final String PIM_INTERFACE_CONFIG_KEY = "pimInterface";

    public static final int DEFAULT_HELLO_INTERVAL = 30; // seconds

    private static final int DEFAULT_TASK_PERIOD_MS = 250;

    // Create a Scheduled Executor service for recurring tasks
    private final ScheduledExecutorService scheduledExecutorService =
            Executors.newScheduledThreadPool(1);

    private final long initialHelloDelay = 1000;

    private final long pimHelloPeriod = DEFAULT_TASK_PERIOD_MS;

    private final int timeoutTaskPeriod = DEFAULT_TASK_PERIOD_MS;

    private final int joinTaskPeriod = 10000;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected PacketService packetService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected NetworkConfigRegistry networkConfig;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected InterfaceService interfaceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected HostService hostService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected MulticastRouteService multicastRouteService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected RouteService unicastRouteService;

    // Store PIM Interfaces in a map key'd by ConnectPoint
    private final Map<ConnectPoint, PimInterface> pimInterfaces = Maps.newConcurrentMap();

    private final Map<McastRoute, PimInterface> routes = Maps.newConcurrentMap();

    private final InternalNetworkConfigListener configListener =
            new InternalNetworkConfigListener();
    private final InternalInterfaceListener interfaceListener =
            new InternalInterfaceListener();
    private final InternalMulticastListener multicastListener =
            new InternalMulticastListener();

    private final ConfigFactory<ConnectPoint, PimInterfaceConfig> pimConfigFactory
            = new ConfigFactory<ConnectPoint, PimInterfaceConfig>(
            SubjectFactories.CONNECT_POINT_SUBJECT_FACTORY, PIM_INTERFACE_CONFIG_CLASS,
            PIM_INTERFACE_CONFIG_KEY) {

        @Override
        public PimInterfaceConfig createConfig() {
            return new PimInterfaceConfig();
        }
    };

    @Activate
    public void activate() {
        networkConfig.registerConfigFactory(pimConfigFactory);

        // Create PIM Interfaces for each of the existing configured interfaces.
        Set<ConnectPoint> subjects = networkConfig.getSubjects(
                ConnectPoint.class, PIM_INTERFACE_CONFIG_CLASS);
        for (ConnectPoint cp : subjects) {
            PimInterfaceConfig config = networkConfig.getConfig(cp, PIM_INTERFACE_CONFIG_CLASS);
            updateInterface(config);
        }

        networkConfig.addListener(configListener);
        interfaceService.addListener(interfaceListener);
        multicastRouteService.addListener(multicastListener);

        multicastRouteService.getRoutes().forEach(this::addRoute);

        // Schedule the periodic hello sender.
        scheduledExecutorService.scheduleAtFixedRate(
                SafeRecurringTask.wrap(
                        () -> pimInterfaces.values().forEach(PimInterface::sendHello)),
                initialHelloDelay, pimHelloPeriod, TimeUnit.MILLISECONDS);

        // Schedule task to periodically time out expired neighbors
        scheduledExecutorService.scheduleAtFixedRate(
                SafeRecurringTask.wrap(
                        () -> pimInterfaces.values().forEach(PimInterface::checkNeighborTimeouts)),
                0, timeoutTaskPeriod, TimeUnit.MILLISECONDS);

        scheduledExecutorService.scheduleAtFixedRate(
                SafeRecurringTask.wrap(
                        () -> pimInterfaces.values().forEach(PimInterface::sendJoins)),
                0, joinTaskPeriod, TimeUnit.MILLISECONDS);

        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        interfaceService.removeListener(interfaceListener);
        networkConfig.removeListener(configListener);
        multicastRouteService.removeListener(multicastListener);
        networkConfig.unregisterConfigFactory(pimConfigFactory);

        // Shutdown the periodic hello task.
        scheduledExecutorService.shutdown();

        log.info("Stopped");
    }

    @Override
    public PimInterface getPimInterface(ConnectPoint cp) {
        PimInterface pi = pimInterfaces.get(cp);
        if (pi == null && log.isTraceEnabled()) {
            log.trace("We have been asked for an Interface we don't have: {}", cp);
        }
        return pi;
    }

    @Override
    public Set<PimInterface> getPimInterfaces() {
        return ImmutableSet.copyOf(pimInterfaces.values());
    }

    private void updateInterface(PimInterfaceConfig config) {
        ConnectPoint cp = config.subject();

        if (!config.isEnabled()) {
            removeInterface(cp);
            return;
        }

        String intfName = config.getInterfaceName();
        Interface intf = interfaceService.getInterfaceByName(cp, intfName);

        if (intf == null) {
            log.debug("Interface configuration missing: {}", config.getInterfaceName());
            return;
        }


        log.debug("Updating Interface for " + intf.connectPoint().toString());
        pimInterfaces.computeIfAbsent(cp, k -> buildPimInterface(config, intf));
    }

    private void removeInterface(ConnectPoint cp) {
        pimInterfaces.remove(cp);
    }

    private PimInterface buildPimInterface(PimInterfaceConfig config, Interface intf) {
        PimInterface.Builder builder = PimInterface.builder()
                .withPacketService(packetService)
                .withInterface(intf);

        config.getHelloInterval().ifPresent(builder::withHelloInterval);
        config.getHoldTime().ifPresent(builder::withHoldTime);
        config.getPriority().ifPresent(builder::withPriority);
        config.getPropagationDelay().ifPresent(builder::withPropagationDelay);
        config.getOverrideInterval().ifPresent(builder::withOverrideInterval);

        return builder.build();
    }

    private void addRoute(McastRoute route) {
        PimInterface pimInterface = getSourceInterface(route);

        if (pimInterface == null) {
            return;
        }

        multicastRouteService.addSource(route, pimInterface.getInterface().connectPoint());

        routes.put(route, pimInterface);
    }

    private void removeRoute(McastRoute route) {
        PimInterface pimInterface = routes.remove(route);

        if (pimInterface == null) {
            return;
        }

        pimInterface.removeRoute(route);
    }

    private PimInterface getSourceInterface(McastRoute route) {
        Route unicastRoute = unicastRouteService.longestPrefixMatch(route.source());

        if (unicastRoute == null) {
            log.warn("No route to source {}", route.source());
            return null;
        }

        Interface intf = interfaceService.getMatchingInterface(unicastRoute.nextHop());

        if (intf == null) {
            log.warn("No interface with route to next hop {}", unicastRoute.nextHop());
            return null;
        }

        PimInterface pimInterface = pimInterfaces.get(intf.connectPoint());

        if (pimInterface == null) {
            log.warn("PIM is not enabled on interface {}", intf);
            return null;
        }

        Set<Host> hosts = hostService.getHostsByIp(unicastRoute.nextHop());
        Host host = null;
        for (Host h : hosts) {
            if (h.vlan().equals(intf.vlan())) {
                host = h;
            }
        }
        if (host == null) {
            log.warn("Next hop host entry not found: {}", unicastRoute.nextHop());
            return null;
        }

        pimInterface.addRoute(route, unicastRoute.nextHop(), host.mac());

        return pimInterface;
    }

    /**
     * Listener for network config events.
     */
    private class InternalNetworkConfigListener implements NetworkConfigListener {

        @Override
        public void event(NetworkConfigEvent event) {
            if (event.configClass() != PIM_INTERFACE_CONFIG_CLASS) {
                return;
            }

            switch (event.type()) {
            case CONFIG_REGISTERED:
            case CONFIG_UNREGISTERED:
                break;
            case CONFIG_ADDED:
            case CONFIG_UPDATED:
                ConnectPoint cp = (ConnectPoint) event.subject();
                PimInterfaceConfig config = networkConfig.getConfig(
                        cp, PIM_INTERFACE_CONFIG_CLASS);

                updateInterface(config);
                break;
            case CONFIG_REMOVED:
                removeInterface((ConnectPoint) event.subject());
                break;
            default:
                break;
            }
        }
    }

    /**
     * Listener for interface events.
     */
    private class InternalInterfaceListener implements InterfaceListener {

        @Override
        public void event(InterfaceEvent event) {
            switch (event.type()) {
            case INTERFACE_ADDED:
                PimInterfaceConfig config = networkConfig.getConfig(
                        event.subject().connectPoint(), PIM_INTERFACE_CONFIG_CLASS);

                if (config != null) {
                    updateInterface(config);
                }
                break;
            case INTERFACE_UPDATED:
                break;
            case INTERFACE_REMOVED:
                removeInterface(event.subject().connectPoint());
                break;
            default:
                break;

            }
        }
    }

    /**
     * Listener for multicast route events.
     */
    private class InternalMulticastListener implements McastListener {
        @Override
        public void event(McastEvent event) {
            switch (event.type()) {
            case ROUTE_ADDED:
                addRoute(event.subject().route());
                break;
            case ROUTE_REMOVED:
                removeRoute(event.subject().route());
                break;
            case SOURCE_ADDED:
            case SINK_ADDED:
            case SINK_REMOVED:
            default:
                break;
            }
        }
    }
}
