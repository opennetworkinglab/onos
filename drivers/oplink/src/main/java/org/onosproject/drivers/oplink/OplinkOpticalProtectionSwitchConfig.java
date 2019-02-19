/*
 * Copyright 2016 Open Networking Foundation
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
package org.onosproject.drivers.oplink;

import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.FilteredConnectPoint;
import org.onosproject.net.Link;
import org.onosproject.net.LinkKey;
import org.onosproject.net.PortNumber;
import org.onosproject.net.behaviour.protection.ProtectedTransportEndpointDescription;
import org.onosproject.net.behaviour.protection.ProtectedTransportEndpointState;
import org.onosproject.net.behaviour.protection.ProtectionConfigBehaviour;
import org.onosproject.net.behaviour.protection.TransportEndpointDescription;
import org.onosproject.net.behaviour.protection.TransportEndpointId;
import org.onosproject.net.behaviour.protection.TransportEndpointState;
import org.onosproject.net.config.NetworkConfigService;
import org.onosproject.net.config.basics.BasicLinkConfig;
import org.onosproject.net.driver.AbstractHandlerBehaviour;
import org.onosproject.net.link.LinkService;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import static org.onosproject.drivers.oplink.OplinkNetconfUtility.*;
import static org.onosproject.net.LinkKey.linkKey;
import static org.onosproject.net.optical.OpticalAnnotations.INPUT_PORT_STATUS;
import static org.onosproject.net.optical.OpticalAnnotations.STATUS_IN_SERVICE;
import static org.onosproject.net.optical.OpticalAnnotations.STATUS_OUT_SERVICE;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Implementations of the protection behaviours for Oplink Optical Protection Switch (OPS).
 *             _____________________
 *            |                     |     4
 *   CLIENT RX|          ---50%-----|----------- PRIMARY TX
 * -----------|----------           |
 *      1     |          ---50%-----|----------- SECONDARY TX
 *            |                     |     6
 *            |    OPLINK OPS BOX   |
 *            |     ___________     |     3
 *   CLIENT TX|    |           |----|----------- PRIMARY RX
 * -----------|----|   SWITCH  |    |
 *      2     |    |___________|----|----------- SECONDARY RX
 *            |_____________________|     5
 *
 *   - Oplink OPS has 6 uni-directional physical ports:
 *      - port 2 is connected to the client side port.
 *      - port 3 is primary port for network side.
 *      - port 5 is secondary port for network side.
 *      - port 1 directly connects to port 4 and 5 with 50/50 split light.
 *   - Traffic protection
 *      - Traffic(Optical light) from client port is broadcasted (50/50 split) to
 *        both primary and secondary ports all the time.
 *      - In fault free condition, traffic from primary port is bridged to client port and
 *        in the case of primary port fails (LOS), traffic is bridged from secondary port to client port.
 *      - User initiated switch (to primary or secondary) is also supported.
 */
public class OplinkOpticalProtectionSwitchConfig extends AbstractHandlerBehaviour
        implements ProtectionConfigBehaviour {

    // key nodes
    private static final String KEY_CONFIG = "config";
    private static final String KEY_OPSCONFIG = "ops-config";
    private static final String KEY_STATE = "state";
    private static final String KEY_OPSSTATE = "ops-state";
    private static final String KEY_NAME_PRIMARY = "primary";
    private static final String KEY_NAME_SECONDARY = "secondary";
    private static final String KEY_OPT_FORCE = "force";
    private static final String KEY_OPT_MANUAL = "manual";
    private static final String KEY_OPT_AUTO = "auto-revertive";

    // operation format: [OPT]-[NAME], eg. force-primary
    private static final String FMT_OPT = "%s-%s";

    // define virtual port number
    private static final PortNumber PORT_VIRTUAL = PortNumber.portNumber(0);
    private static final PortNumber PORT_PRIMARY = PortNumber.portNumber(3, "primary_port");
    private static final PortNumber PORT_SECONDARY = PortNumber.portNumber(5, "secondary_port");
    // log
    private static final Logger log = getLogger(OplinkOpticalProtectionSwitchConfig.class);

    @Override
    public CompletableFuture<ConnectPoint> createProtectionEndpoint(
            ProtectedTransportEndpointDescription configuration) {
        // Add a virtual link between two virtual ports of the device and peer.
        addLink(getPeerId());
        // Only support one group in the device.
        return CompletableFuture.completedFuture(new ConnectPoint(data().deviceId(), PORT_VIRTUAL));
    }

    @Override
    public CompletableFuture<ConnectPoint> updateProtectionEndpoint(ConnectPoint identifier,
            ProtectedTransportEndpointDescription configuration) {
        // The function of updating protection virtual Port is not supported by the device.
        return CompletableFuture.completedFuture(identifier);
    }

    @Override
    public CompletableFuture<Boolean> deleteProtectionEndpoint(ConnectPoint identifier) {
        if (identifier.port().equals(PORT_VIRTUAL)) {
            // Remove the virtual link from peer to the virtual port.
            removeLink(getPeerId());
            return CompletableFuture.completedFuture(true);
        }
        return CompletableFuture.completedFuture(false);
    }

    @Override
    public CompletableFuture<Map<ConnectPoint, ProtectedTransportEndpointDescription>>
        getProtectionEndpointConfigs() {
        // There are two underlying transport entity endpoints in the device.
        Map<ConnectPoint, ProtectedTransportEndpointDescription> map = new HashMap<>();
        map.put(new ConnectPoint(data().deviceId(), PORT_VIRTUAL), buildProtectedTransportEndpointDescription());
        return CompletableFuture.completedFuture(map);
    }

    @Override
    public CompletableFuture<Map<ConnectPoint, ProtectedTransportEndpointState>> getProtectionEndpointStates() {
        Map<ConnectPoint, ProtectedTransportEndpointState> map = new HashMap<>();
        map.put(new ConnectPoint(data().deviceId(), PORT_VIRTUAL), buildProtectedTransportEndpointState());
        return CompletableFuture.completedFuture(map);
    }

    @Override
    public CompletableFuture<Void> switchToForce(ConnectPoint identifier, int index) {
        return getProtectionEndpointConfig(identifier)
                .thenApply(m -> m.paths().get(index))
                .thenApply(m -> switchDevice(formatOperation(m.output().connectPoint().port(), KEY_OPT_FORCE)))
                .thenApply(m -> null);
    }

    @Override
    public CompletableFuture<Void> switchToManual(ConnectPoint identifier, int index) {
        return getProtectionEndpointConfig(identifier)
                .thenApply(m -> m.paths().get(index))
                .thenApply(m -> switchDevice(formatOperation(m.output().connectPoint().port(), KEY_OPT_MANUAL)))
                .thenApply(m -> null);
    }

    @Override
    public CompletableFuture<Void> switchToAutomatic(ConnectPoint identifier) {
        switchDevice(KEY_OPT_AUTO);
        return CompletableFuture.completedFuture(null);
    }

    private ProtectedTransportEndpointState buildProtectedTransportEndpointState() {
        // First, get active port from device.
        PortNumber activePort = acquireActivePort();
        // Build all endpoint state with port working attribute.
        List<TransportEndpointState> states = new ArrayList<>();
        states.add(buildTransportEndpointState(data().deviceId(), PORT_PRIMARY, activePort));
        states.add(buildTransportEndpointState(data().deviceId(), PORT_SECONDARY, activePort));
        return ProtectedTransportEndpointState.builder()
                .withPathStates(states)
                .withDescription(buildProtectedTransportEndpointDescription())
                .withActivePathIndex(getActiveIndex(states, activePort))
                .build();
    }

    private ProtectedTransportEndpointDescription buildProtectedTransportEndpointDescription() {
        List<TransportEndpointDescription> descs = new ArrayList<>();
        descs.add(buildTransportEndpointDescription(data().deviceId(), PORT_PRIMARY));
        descs.add(buildTransportEndpointDescription(data().deviceId(), PORT_SECONDARY));
        return ProtectedTransportEndpointDescription.of(descs, getPeerId(), FINGERPRINT);
    }

    private TransportEndpointDescription buildTransportEndpointDescription(DeviceId id, PortNumber port) {
        return TransportEndpointDescription.builder()
                .withOutput(new FilteredConnectPoint(new ConnectPoint(id, port)))
                .build();
    }

    private TransportEndpointState buildTransportEndpointState(
            DeviceId id, PortNumber port, PortNumber activePort) {
        String status = port.equals(activePort) ? STATUS_IN_SERVICE : STATUS_OUT_SERVICE;
        Map<String, String> attributes = new HashMap<>();
        attributes.put(INPUT_PORT_STATUS, status);
        return TransportEndpointState.builder()
                .withId(TransportEndpointId.of(port.name()))
                .withDescription(buildTransportEndpointDescription(id, port))
                .addAttributes(attributes)
                .build();
    }

    private int getActiveIndex(List<TransportEndpointState> pathStates, PortNumber activePort) {
        int activeIndex = 0;
        for (TransportEndpointState state : pathStates) {
            if (activePort.equals(state.description().output().connectPoint().port())) {
                return activeIndex;
            }
            ++activeIndex;
        }
        return ProtectedTransportEndpointState.ACTIVE_UNKNOWN;
    }

    private PortNumber acquireActivePort() {
        String filter = new StringBuilder(xmlOpen(KEY_OPENOPTICALDEV_XMLNS))
                .append(xmlOpen(KEY_STATE))
                .append(xmlEmpty(KEY_OPSSTATE))
                .append(xmlClose(KEY_STATE))
                .append(xmlClose(KEY_OPENOPTICALDEV))
                .toString();
        String reply = netconfGet(handler(), filter);
        log.debug("Service state replying, {}", reply);
        return reply.contains(KEY_NAME_PRIMARY) ? PORT_PRIMARY : PORT_SECONDARY;
    }

    private String formatOperation(PortNumber port, String operation) {
        String key = port.name().contains(KEY_NAME_PRIMARY) ? KEY_NAME_PRIMARY : KEY_NAME_SECONDARY;
        return String.format(FMT_OPT, operation, key);
    }

    private boolean switchDevice(String operation) {
        log.debug("Switch to {} for Device {}", operation, data().deviceId());
        String cfg = new StringBuilder(xmlOpen(KEY_OPENOPTICALDEV_XMLNS))
                .append(xmlOpen(KEY_CONFIG))
                .append(xml(KEY_OPSCONFIG, operation))
                .append(xmlClose(KEY_CONFIG))
                .append(xmlClose(KEY_OPENOPTICALDEV))
                .toString();
        return netconfEditConfig(handler(), CFG_MODE_MERGE, cfg);
    }

    private void addLink(DeviceId peerId) {
        if (peerId == null) {
            log.warn("PeerID is null for device {}", data().deviceId());
            return;
        }
        LinkKey link = linkKey(new ConnectPoint(peerId, PORT_VIRTUAL),
                               new ConnectPoint(data().deviceId(), PORT_VIRTUAL));
        handler().get(NetworkConfigService.class).addConfig(link, BasicLinkConfig.class)
                .type(Link.Type.VIRTUAL)
                .apply();
    }

    private void removeLink(DeviceId peerId) {
        if (peerId == null) {
            log.warn("PeerID is null for device {}", data().deviceId());
            return;
        }
        LinkKey link = linkKey(new ConnectPoint(peerId, PORT_VIRTUAL),
                               new ConnectPoint(data().deviceId(), PORT_VIRTUAL));
        handler().get(NetworkConfigService.class).removeConfig(link, BasicLinkConfig.class);
    }

    private DeviceId getPeerId() {
        ConnectPoint dstCp = new ConnectPoint(data().deviceId(), PORT_VIRTUAL);
        Set<Link> links = handler().get(LinkService.class).getIngressLinks(dstCp);
        for (Link l : links) {
            if (l.type() == Link.Type.VIRTUAL) {
                // This device is the destination and peer is the source.
                return l.src().deviceId();
            }
        }
        // None of link found, return itself.
        return data().deviceId();
    }
}
