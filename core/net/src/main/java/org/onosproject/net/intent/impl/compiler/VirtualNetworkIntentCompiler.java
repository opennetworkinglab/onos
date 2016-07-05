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

package org.onosproject.net.intent.impl.compiler;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onlab.osgi.DefaultServiceDirectory;
import org.onlab.osgi.ServiceDirectory;
import org.onosproject.core.ApplicationId;
import org.onosproject.incubator.net.tunnel.TunnelId;
import org.onosproject.incubator.net.virtual.NetworkId;
import org.onosproject.incubator.net.virtual.VirtualNetworkIntent;
import org.onosproject.incubator.net.virtual.VirtualNetworkService;
import org.onosproject.incubator.net.virtual.VirtualNetworkStore;
import org.onosproject.incubator.net.virtual.VirtualPort;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.EncapsulationType;
import org.onosproject.net.Link;
import org.onosproject.net.Path;
import org.onosproject.net.intent.Constraint;
import org.onosproject.net.intent.Intent;
import org.onosproject.net.intent.IntentService;
import org.onosproject.net.intent.Key;
import org.onosproject.net.intent.PointToPointIntent;
import org.onosproject.net.intent.constraint.EncapsulationConstraint;
import org.onosproject.net.intent.impl.IntentCompilationException;
import org.onosproject.net.topology.TopologyService;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * An intent compiler for {@link org.onosproject.incubator.net.virtual.VirtualNetworkIntent}.
 */
@Component(immediate = true)
public class VirtualNetworkIntentCompiler
        extends ConnectivityIntentCompiler<VirtualNetworkIntent> {

    private final Logger log = getLogger(getClass());

    private static final String NETWORK_ID = "networkId=";
    protected static final String KEY_FORMAT = "{" + NETWORK_ID + "%s, src=%s, dst=%s}";

    protected ServiceDirectory serviceDirectory = new DefaultServiceDirectory();

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected VirtualNetworkService manager;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected IntentService intentService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected VirtualNetworkStore store;


    @Activate
    public void activate() {
        intentManager.registerCompiler(VirtualNetworkIntent.class, this);
    }

    @Deactivate
    public void deactivate() {
        intentManager.unregisterCompiler(VirtualNetworkIntent.class);
    }

    @Override
    public List<Intent> compile(VirtualNetworkIntent intent, List<Intent> installable) {

        log.debug("Compiling intent: " + intent);
        List<Intent> intents = new ArrayList<>();
        Optional<Path> path = getPaths(intent).stream()
                .findFirst();
        if (path != null && path.isPresent()) {
            path.get().links().forEach(link -> {
                Intent physicalIntent = createPtPtIntent(intent, link);
                intents.add(physicalIntent);

                // store the virtual intent to physical intent tunnelId mapping
                store.addTunnelId(intent, TunnelId.valueOf(physicalIntent.key().toString()));
            });
        } else {
            throw new IntentCompilationException("Unable to find a path for intent " + intent);
        }

        return intents;
    }

    /**
     * Returns the paths for the virtual network intent.
     *
     * @param intent virtual network intent
     * @return set of paths
     */
    private Set<Path> getPaths(VirtualNetworkIntent intent) {

        TopologyService topologyService = manager.get(intent.networkId(), TopologyService.class);
        if (topologyService == null) {
            throw new IntentCompilationException("topologyService is null");
        }
        return topologyService.getPaths(topologyService.currentTopology(),
                                        intent.ingressPoint().deviceId(), intent.egressPoint().deviceId());
    }

    /**
     * Encodes the key using the network identifier, application identifer, source and destination
     * connect points.
     *
     * @param networkId     virtual network identifier
     * @param applicationId application identifier
     * @param src           source connect point
     * @param dst           destination connect point
     * @return encoded key
     */
    private static Key encodeKey(NetworkId networkId, ApplicationId applicationId, ConnectPoint src, ConnectPoint dst) {
        String key = String.format(KEY_FORMAT, networkId, src, dst);
        return Key.of(key, applicationId);
    }

    /**
     * Creates a point-to-point intent from the virtual network intent and virtual link.
     *
     * @param intent virtual network intent
     * @param link   virtual link
     * @return point to point intent
     */
    private Intent createPtPtIntent(VirtualNetworkIntent intent, Link link) {
        ConnectPoint ingressPoint = mapVirtualToPhysicalPort(intent.networkId(), link.src());
        ConnectPoint egressPoint = mapVirtualToPhysicalPort(intent.networkId(), link.dst());
        Key intentKey = encodeKey(intent.networkId(), intent.appId(), ingressPoint, egressPoint);

        List<Constraint> constraints = new ArrayList<>();
        constraints.add(new EncapsulationConstraint(EncapsulationType.VLAN));

        // TODO Currently there can only be one intent between the ingress and egress across
        // all virtual networks. We may want to support multiple intents between the same src/dst pairs.
        PointToPointIntent physicalIntent = PointToPointIntent.builder()
                .key(intentKey)
                .appId(intent.appId())
                .ingressPoint(ingressPoint)
                .egressPoint(egressPoint)
                .constraints(constraints)
                .build();
        log.debug("Submitting physical intent: " + physicalIntent);
        intentService.submit(physicalIntent);

        return physicalIntent;
    }

    /**
     * Maps the virtual connect point to a physical connect point.
     *
     * @param networkId virtual network identifier
     * @param virtualCp virtual connect point
     * @return physical connect point
     */
    private ConnectPoint mapVirtualToPhysicalPort(NetworkId networkId, ConnectPoint virtualCp) {
        Set<VirtualPort> ports = manager.getVirtualPorts(networkId, virtualCp.deviceId());
        for (VirtualPort port : ports) {
            if (port.element().id().equals(virtualCp.elementId()) &&
                    port.number().equals(virtualCp.port())) {
                return new ConnectPoint(port.realizedBy().element().id(), port.realizedBy().number());
            }
        }
        return null;
    }
}

