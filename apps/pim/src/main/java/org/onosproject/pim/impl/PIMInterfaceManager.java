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
package org.onosproject.pim.impl;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.util.SafeRecurringTask;
import org.onosproject.incubator.net.intf.Interface;
import org.onosproject.incubator.net.intf.InterfaceEvent;
import org.onosproject.incubator.net.intf.InterfaceListener;
import org.onosproject.incubator.net.intf.InterfaceService;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.config.ConfigFactory;
import org.onosproject.net.config.NetworkConfigEvent;
import org.onosproject.net.config.NetworkConfigListener;
import org.onosproject.net.config.NetworkConfigRegistry;
import org.onosproject.net.config.basics.SubjectFactories;
import org.onosproject.net.packet.PacketService;
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
@Component(immediate = true)
@Service
public class PIMInterfaceManager implements PIMInterfaceService {

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

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected PacketService packetService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected NetworkConfigRegistry networkConfig;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected InterfaceService interfaceService;

    // Store PIM Interfaces in a map key'd by ConnectPoint
    private final Map<ConnectPoint, PIMInterface> pimInterfaces = Maps.newConcurrentMap();

    private final InternalNetworkConfigListener configListener =
            new InternalNetworkConfigListener();
    private final InternalInterfaceListener interfaceListener =
            new InternalInterfaceListener();

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

        // Schedule the periodic hello sender.
        scheduledExecutorService.scheduleAtFixedRate(
                SafeRecurringTask.wrap(
                        () -> pimInterfaces.values().forEach(PIMInterface::sendHello)),
                initialHelloDelay, pimHelloPeriod, TimeUnit.MILLISECONDS);

        // Schedule task to periodically time out expired neighbors
        scheduledExecutorService.scheduleAtFixedRate(
                SafeRecurringTask.wrap(
                        () -> pimInterfaces.values().forEach(PIMInterface::checkNeighborTimeouts)),
                0, timeoutTaskPeriod, TimeUnit.MILLISECONDS);

        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        interfaceService.removeListener(interfaceListener);
        networkConfig.removeListener(configListener);
        networkConfig.unregisterConfigFactory(pimConfigFactory);

        // Shutdown the periodic hello task.
        scheduledExecutorService.shutdown();

        log.info("Stopped");
    }

    /**
     * Return the PIMInterface that corresponds to the given ConnectPoint.
     *
     * @param cp The ConnectPoint we want to get the PIMInterface for
     * @return The PIMInterface if it exists, NULL if it does not exist.
     */
    @Override
    public PIMInterface getPIMInterface(ConnectPoint cp) {
        PIMInterface pi = pimInterfaces.getOrDefault(cp, null);
        if (pi == null) {
            log.warn("We have been asked for an Interface we don't have: " + cp.toString());
        }
        return pi;
    }

    @Override
    public Set<PIMInterface> getPimInterfaces() {
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

    private PIMInterface buildPimInterface(PimInterfaceConfig config, Interface intf) {
        PIMInterface.Builder builder = PIMInterface.builder()
                .withPacketService(packetService)
                .withInterface(intf);

        config.getHelloInterval().ifPresent(builder::withHelloInterval);
        config.getHoldTime().ifPresent(builder::withHoldTime);
        config.getPriority().ifPresent(builder::withPriority);
        config.getPropagationDelay().ifPresent(builder::withPropagationDelay);
        config.getOverrideInterval().ifPresent(builder::withOverrideInterval);

        return builder.build();
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
}
