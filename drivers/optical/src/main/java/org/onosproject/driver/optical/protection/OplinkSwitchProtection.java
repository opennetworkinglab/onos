/*
 * Copyright 2017-present Open Networking Foundation
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

package org.onosproject.driver.optical.protection;

import org.onosproject.core.CoreService;
import org.onosproject.net.DeviceId;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.FilteredConnectPoint;
import org.onosproject.net.Link;
import org.onosproject.net.LinkKey;
import org.onosproject.net.Port;
import org.onosproject.net.PortNumber;
import org.onosproject.net.behaviour.protection.ProtectedTransportEndpointDescription;
import org.onosproject.net.behaviour.protection.ProtectedTransportEndpointState;
import org.onosproject.net.behaviour.protection.ProtectionConfigBehaviour;
import org.onosproject.net.behaviour.protection.TransportEndpointDescription;
import org.onosproject.net.behaviour.protection.TransportEndpointId;
import org.onosproject.net.behaviour.protection.TransportEndpointState;
import org.onosproject.net.config.NetworkConfigService;
import org.onosproject.net.config.basics.BasicLinkConfig;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.driver.AbstractHandlerBehaviour;
import org.onosproject.net.flow.DefaultFlowRule;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.FlowRuleService;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.link.LinkService;
import org.onosproject.net.optical.OpticalAnnotations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import static org.onosproject.net.LinkKey.linkKey;

/**
 * Implementations of the protection behaviours for Oplink Optical Protection Switch (OPS).
 *   - Oplink OPS has 3 logical bi-directional ports (6 uni-directional physical ports):
 *      - port 1 is primary port for network side
 *      - port 2 is secondary port for network side.
 *      - port 3 is connected to the client side port;
 *   - Traffic protection
 *      - Traffic(Optical light) from client port is broadcasted (50/50 split) to
 *        both primary and secondary ports all the time.
 *      - In fault free condition, traffic from primary port is bridged to client port and
 *        in the case of primary port fails (LOS), traffic is bridged from secondary port to client port.
 *      - User initiated switch (to primary or secondary) is also supported.
 */
public class OplinkSwitchProtection extends AbstractHandlerBehaviour implements ProtectionConfigBehaviour {

    private static final int VIRTUAL_PORT = 0;
    private static final int PRIMARY_PORT = 1;
    private static final int SECONDARY_PORT = 2;
    private static final int CLIENT_PORT = 3;
    private static final int FLOWRULE_PRIORITY = 88;
    private static final String PRIMARY_ID = "primary_port";
    private static final String SECONDARY_ID = "secondary_port";
    private static final String OPLINK_FINGERPRINT = "OplinkOPS";
    private static final String APP_ID = "org.onosproject.drivers.optical";

    protected final Logger log = LoggerFactory.getLogger(getClass());

    @Override
    public CompletableFuture<ConnectPoint> createProtectionEndpoint(
            ProtectedTransportEndpointDescription configuration) {
        //This OPS device only support one protection group of port 2 and port 3

        CompletableFuture result = new CompletableFuture<ConnectPoint>();

        //add flow from client port to virtual port. This set device in auto switch mode
        addFlow(PortNumber.portNumber(VIRTUAL_PORT));

        //add a virtual link between two virtual ports of this device and peer
        addLinkToPeer(configuration.peer());

        result.complete(new ConnectPoint(data().deviceId(), PortNumber.portNumber(VIRTUAL_PORT)));

        return result;
    }

    @Override
    public CompletableFuture<ConnectPoint> updateProtectionEndpoint(
            ConnectPoint identifier, ProtectedTransportEndpointDescription configuration) {

        log.warn("Update protection configuration is not supported by this device");

        CompletableFuture result = new CompletableFuture<ConnectPoint>();
        result.complete(new ConnectPoint(data().deviceId(), PortNumber.portNumber(VIRTUAL_PORT)));

        return result;
    }

    @Override
    public CompletableFuture<Boolean> deleteProtectionEndpoint(ConnectPoint identifier) {
        //OPS has only one protection group
        CompletableFuture result = new CompletableFuture<Boolean>();

        if (identifier.port().toLong() == VIRTUAL_PORT) {
            //add a link between two virtual ports of this device and peer
            removeLinkToPeer(getPeerId());
            deleteFlow();
            result.complete(true);
        } else {
            result.complete(false);
        }

        return result;
    }

    @Override
    public CompletableFuture<Map<ConnectPoint, ProtectedTransportEndpointDescription>> getProtectionEndpointConfigs() {
        ConnectPoint cp = new ConnectPoint(data().deviceId(), PortNumber.portNumber(VIRTUAL_PORT));

        Map<ConnectPoint, ProtectedTransportEndpointDescription> protectedGroups = new HashMap<>();
        CompletableFuture result = new CompletableFuture<Map<ConnectPoint, ProtectedTransportEndpointDescription>>();

        protectedGroups.put(cp, getProtectedTransportEndpointDescription());
        result.complete(protectedGroups);

        return result;
    }

    @Override
    public CompletableFuture<Map<ConnectPoint, ProtectedTransportEndpointState>> getProtectionEndpointStates() {
        ConnectPoint cp = new ConnectPoint(data().deviceId(), PortNumber.portNumber(VIRTUAL_PORT));

        Map<ConnectPoint, ProtectedTransportEndpointState> protectedGroups = new HashMap<>();
        CompletableFuture result = new CompletableFuture<Map<ConnectPoint, ProtectedTransportEndpointState>>();

        protectedGroups.put(cp, getProtectedTransportEndpointState());
        result.complete(protectedGroups);

        return result;
    }

    @Override
    public CompletableFuture<Void> switchToForce(ConnectPoint identifier, int index) {
        // TODO
        // Currently not supported for openflow device.
        CompletableFuture<Void> future = new CompletableFuture<>();
        future.completeExceptionally(new UnsupportedOperationException());
        return future;
    }

    @Override
    public CompletableFuture<Void> switchToManual(ConnectPoint identifier, int index) {
        return getProtectionEndpointConfig(identifier)
                .thenApply(m -> m.paths().get(index))
                .thenApply(m -> switchDevice(m.output().connectPoint().port()))
                .thenApply(m -> null);
    }

    @Override
    public CompletableFuture<Void> switchToAutomatic(ConnectPoint identifier) {
        switchDevice(PortNumber.portNumber(VIRTUAL_PORT));
        return CompletableFuture.completedFuture(null);
    }

    /**
     * port: PRIMARY_PORT   - manual switch to primary port.
     *       SECONDARY_PORT - manual switch to Secondary port.
     *       VIRTUAL_PORT   - automatic switch mode.
     */
    private boolean switchDevice(PortNumber port) {
        // TODO
        // If the flow operations do not go through, the controller would be in an inconsistent state.
        // Using listener can be a hack to imitate async API, to workaround the issue.
        // But can probably get tricky to do it correctly, ensuring not leaving dangling listener, etc.
        deleteFlow();
        addFlow(port);
        return true;
    }

    /*
     * return the protected endpoint description of this devices
     */
    private ProtectedTransportEndpointDescription getProtectedTransportEndpointDescription() {
        List<TransportEndpointDescription> teds = new ArrayList<>();
        FilteredConnectPoint fcpPrimary = new FilteredConnectPoint(
                new ConnectPoint(data().deviceId(), PortNumber.portNumber(PRIMARY_PORT)));
        FilteredConnectPoint fcpSecondary = new FilteredConnectPoint(
                new ConnectPoint(data().deviceId(), PortNumber.portNumber(SECONDARY_PORT)));
        TransportEndpointDescription tedPrimary = TransportEndpointDescription.builder()
                .withOutput(fcpPrimary).build();
        TransportEndpointDescription tedSecondary = TransportEndpointDescription.builder()
                .withOutput(fcpSecondary).build();

        teds.add(tedPrimary);
        teds.add(tedSecondary);
        return ProtectedTransportEndpointDescription.of(teds, getPeerId(), OPLINK_FINGERPRINT);
    }

    /*
     * get endpoint state attributes
     */
    private Map<String, String> getProtectionStateAttributes(PortNumber portNumber) {
        Map<String, String> attributes = new HashMap<>();

        //get status form port annotations, the status is update by hand shaker driver periodically
        Port port = handler().get(DeviceService.class).getPort(data().deviceId(), portNumber);
        if (port != null) {
            String portStatus = port.annotations().value(OpticalAnnotations.INPUT_PORT_STATUS);
            attributes.put(OpticalAnnotations.INPUT_PORT_STATUS, portStatus);
        }
        return attributes;
    }

    /*
     * get activer port number
     */
    private int getActivePort() {
        Port port = handler().get(DeviceService.class)
                .getPort(data().deviceId(), PortNumber.portNumber(PRIMARY_PORT));
        if (port != null) {
            if (port.annotations().value(OpticalAnnotations.INPUT_PORT_STATUS)
                    .equals(OpticalAnnotations.STATUS_IN_SERVICE)) {
                return PRIMARY_PORT;
            }
        }
        return SECONDARY_PORT;
    }

    /*
     * get active path index
     */
    private int getActiveIndex(List<TransportEndpointState> pathStates) {
        long activePort = (long) getActivePort();
        int activeIndex = 0;
        for (TransportEndpointState state : pathStates) {
            if (state.description().output().connectPoint().port().toLong() == activePort) {
                return activeIndex;
            }
            ++activeIndex;
        }
        return ProtectedTransportEndpointState.ACTIVE_UNKNOWN;
    }

    /*
     * get protected endpoint state
     */
    private ProtectedTransportEndpointState getProtectedTransportEndpointState() {
        List<TransportEndpointState> tess = new ArrayList<>();
        PortNumber portPrimary = PortNumber.portNumber(PRIMARY_PORT);
        PortNumber portSecondary = PortNumber.portNumber(SECONDARY_PORT);
        FilteredConnectPoint fcpPrimary = new FilteredConnectPoint(
                new ConnectPoint(data().deviceId(), portPrimary));
        FilteredConnectPoint fcpSecondary = new FilteredConnectPoint(
                new ConnectPoint(data().deviceId(), portSecondary));
        TransportEndpointDescription tedPrimary = TransportEndpointDescription.builder()
                .withOutput(fcpPrimary).build();
        TransportEndpointDescription tedSecondary = TransportEndpointDescription.builder()
                .withOutput(fcpSecondary).build();

        TransportEndpointState tesPrimary = TransportEndpointState.builder()
                .withDescription(tedPrimary)
                .withId(TransportEndpointId.of(PRIMARY_ID))
                .addAttributes(getProtectionStateAttributes(portPrimary))
                .build();
        TransportEndpointState tesSecondary = TransportEndpointState.builder()
                .withDescription(tedSecondary)
                .withId(TransportEndpointId.of(SECONDARY_ID))
                .addAttributes(getProtectionStateAttributes((portSecondary)))
                .build();

        tess.add(tesPrimary);
        tess.add(tesSecondary);
        return ProtectedTransportEndpointState.builder()
                .withDescription(getProtectedTransportEndpointDescription())
                .withPathStates(tess)
                .withActivePathIndex(getActiveIndex(tess))
                .build();
    }

    /*
     *   - Protection switch is controlled by setting up flow on the device
     *      - There is only one flow on the device at any point
     *      - A flow from virtual port to client port indicates the device is in auto switch mode
     *      - A flow from primary port to client port indicates the device is manually switched to primary
     *      - A flow from secondary port to client port indicates the device is manually switched to secondary
     */
    private void addFlow(PortNumber workingPort) {
        // set working port as flow's input port
        TrafficSelector selector = DefaultTrafficSelector.builder()
                .matchInPort(workingPort)
                .build();
        // the flow's  output port is always the clinet port
        TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                .setOutput(PortNumber.portNumber(CLIENT_PORT))
                .build();
        FlowRule flowRule = DefaultFlowRule.builder()
                .forDevice(data().deviceId())
                .fromApp(handler().get(CoreService.class).getAppId(APP_ID))
                .withPriority(FLOWRULE_PRIORITY)
                .withSelector(selector)
                .withTreatment(treatment)
                .makePermanent()
                .build();

        // install flow rule
        handler().get(FlowRuleService.class).applyFlowRules(flowRule);
    }
    /*
        Delete all the flows to put device in default mode.
     */
    private void deleteFlow() {
        // remove all the flows.
        handler().get(FlowRuleService.class).purgeFlowRules(data().deviceId());
    }

    private void addLinkToPeer(DeviceId peerId) {

        if (peerId == null) {
            log.warn("PeerID is null for device {}", data().deviceId());
            return;
        }
        ConnectPoint dstCp = new ConnectPoint(data().deviceId(), PortNumber.portNumber(VIRTUAL_PORT));
        ConnectPoint srcCp = new ConnectPoint(peerId, PortNumber.portNumber(VIRTUAL_PORT));
        LinkKey link = linkKey(srcCp, dstCp);
        BasicLinkConfig cfg = handler().get(NetworkConfigService.class)
                .addConfig(link, BasicLinkConfig.class);
        cfg.type(Link.Type.VIRTUAL);
        cfg.apply();
    }

    private void removeLinkToPeer(DeviceId peerId) {
        if (peerId == null) {
            log.warn("PeerID is null for device {}", data().deviceId());
            return;
        }
        ConnectPoint dstCp = new ConnectPoint(data().deviceId(), PortNumber.portNumber(VIRTUAL_PORT));
        ConnectPoint srcCp = new ConnectPoint(peerId, PortNumber.portNumber(VIRTUAL_PORT));
        LinkKey link = linkKey(srcCp, dstCp);
        handler().get(NetworkConfigService.class).removeConfig(link, BasicLinkConfig.class);
    }

    private DeviceId getPeerId() {
        ConnectPoint dstCp = new ConnectPoint(data().deviceId(), PortNumber.portNumber(VIRTUAL_PORT));
        Set<Link> links = handler().get(LinkService.class).getIngressLinks(dstCp);

        for (Link l : links) {
            if (l.type() == Link.Type.VIRTUAL) {
                // this device is the destination and peer is the source.
                return l.src().deviceId();
            }
        }

        return data().deviceId();
    }
}
