/*
 * Copyright 2016-present Open Networking Laboratory
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
package org.onosproject.vpls;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
import org.onosproject.app.ApplicationService;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.incubator.net.intf.InterfaceEvent;
import org.onosproject.incubator.net.intf.InterfaceListener;
import org.onosproject.incubator.net.intf.InterfaceService;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.Host;
import org.onosproject.net.host.HostEvent;
import org.onosproject.net.host.HostListener;
import org.onosproject.net.host.HostService;
import org.onosproject.net.intent.IntentService;
import org.onosproject.routing.IntentSynchronizationService;
import org.slf4j.Logger;

import java.util.Map;
import java.util.Set;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Application to create L2 broadcast overlay networks using VLAN.
 */
@Component(immediate = true)
public class Vpls {
    protected static final String VPLS_APP = "org.onosproject.vpls";

    private final Logger log = getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ApplicationService applicationService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected HostService hostService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected IntentService intentService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected InterfaceService interfaceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected IntentSynchronizationService intentSynchronizer;

    private final HostListener hostListener = new InternalHostListener();

    private final InternalInterfaceListener interfaceListener
            = new InternalInterfaceListener();

    private IntentInstaller intentInstaller;

    private ApplicationId appId;


    @Activate
    public void activate() {
        appId = coreService.registerApplication(VPLS_APP);

        intentInstaller = new IntentInstaller(appId,
                                              intentService,
                                              intentSynchronizer);

        applicationService.registerDeactivateHook(appId, () -> {
            intentSynchronizer.removeIntentsByAppId(appId);
        });

        hostService.addListener(hostListener);
        interfaceService.addListener(interfaceListener);

        setupConnectivity();

        log.debug("Activated");
    }

    @Deactivate
    public void deactivate() {
        intentSynchronizer.removeIntentsByAppId(appId);
        log.debug("Deactivated");
    }

    protected void setupConnectivity() {
        /*
         * Parse Configuration and get Connect Point by VlanId.
         */
        SetMultimap<VlanId, ConnectPoint> confCPointsByVlan = getConfigCPoints();

        /*
         * Check that configured Connect Points have hosts attached and
         * associate their Mac Address to the Connect Points configured.
         */
        SetMultimap<VlanId, Pair<ConnectPoint, MacAddress>> confHostPresentCPoint =
                pairAvailableHosts(confCPointsByVlan);

        /*
         * Create and submit intents between the Connect Points.
         * Intents for broadcast between all the configured Connect Points.
         * Intents for unicast between all the configured Connect Points with
         * hosts attached.
         */
        intentInstaller.installIntents(confHostPresentCPoint);

    }

    /**
     * Computes the list of configured interfaces with a VLAN Id.
     *
     * @return the interfaces grouped by vlan id
     */
    protected SetMultimap<VlanId, ConnectPoint> getConfigCPoints() {
        log.debug("Checking interface configuration");

        SetMultimap<VlanId, ConnectPoint> confCPointsByVlan =
                HashMultimap.create();

        interfaceService.getInterfaces()
                .stream()
                .filter(intf -> intf.ipAddressesList().isEmpty())
                .forEach(intf -> confCPointsByVlan.put(intf.vlan(), intf.connectPoint()));
        return confCPointsByVlan;
    }

    /**
     * Checks if for any ConnectPoint configured there's an host presents
     * and in case it associates them together.
     *
     * @param confCPointsByVlan the configured ConnectPoints grouped by VLAN Id
     * @return the configured ConnectPoints with eventual hosts associated.
     */
    protected SetMultimap<VlanId, Pair<ConnectPoint, MacAddress>> pairAvailableHosts(
            SetMultimap<VlanId, ConnectPoint> confCPointsByVlan) {
        log.debug("Binding connected hosts MAC addresses");

        SetMultimap<VlanId, Pair<ConnectPoint, MacAddress>> confHostPresentCPoint =
                HashMultimap.create();

        confCPointsByVlan.entries()
                .forEach(e -> bindMacAddr(e, confHostPresentCPoint));

        return confHostPresentCPoint;
    }

    // Bind VLAN Id with hosts and connect points
    private void bindMacAddr(Map.Entry<VlanId, ConnectPoint> e,
                             SetMultimap<VlanId, Pair<ConnectPoint,
                                     MacAddress>> confHostPresentCPoint) {
        VlanId vlanId = e.getKey();
        ConnectPoint cp = e.getValue();
        Set<Host> connectedHosts = hostService.getConnectedHosts(cp);
        connectedHosts.forEach(host -> {
            if (host.vlan().equals(vlanId)) {
                confHostPresentCPoint.put(vlanId, Pair.of(cp, host.mac()));
            } else {
                confHostPresentCPoint.put(vlanId, Pair.of(cp, null));
            }
        });
        if (connectedHosts.isEmpty()) {
            confHostPresentCPoint.put(vlanId, Pair.of(cp, null));
        }
    }

    /**
     * Listener for host events.
     */
    class InternalHostListener implements HostListener {
        @Override
        public void event(HostEvent event) {
            log.debug("Received HostEvent {}", event);
            switch (event.type()) {
                case HOST_ADDED:
                case HOST_UPDATED:
                case HOST_REMOVED:
                    setupConnectivity();
                    break;
                default:
                    break;
            }
        }
    }

    /**
     * Listener for interface configuration events.
     */
    private class InternalInterfaceListener implements InterfaceListener {
        @Override
        public void event(InterfaceEvent event) {
            log.debug("Received InterfaceConfigEvent {}", event);
            switch (event.type()) {
                case INTERFACE_ADDED:
                case INTERFACE_UPDATED:
                case INTERFACE_REMOVED:
                    setupConnectivity();
                    break;
                default:
                    break;
            }
        }
    }
}
