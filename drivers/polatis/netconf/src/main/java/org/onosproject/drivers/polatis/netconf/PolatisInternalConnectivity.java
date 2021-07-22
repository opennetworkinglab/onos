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

package org.onosproject.drivers.polatis.netconf;

import org.onosproject.net.DeviceId;
import org.onosproject.net.Port;
import org.onosproject.net.PortNumber;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.behaviour.InternalConnectivity;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.driver.AbstractHandlerBehaviour;
import org.onosproject.net.flow.FlowEntry;
import org.onosproject.net.flow.criteria.Criterion;
import org.onosproject.net.flow.criteria.PortCriterion;
import org.onosproject.net.flow.instructions.Instructions.OutputInstruction;
import java.util.Set;
import java.util.HashSet;
import java.util.Collection;
import java.util.stream.Collectors;
import org.slf4j.Logger;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.slf4j.LoggerFactory.getLogger;
import static org.onosproject.drivers.polatis.netconf.PolatisUtility.*;
import static org.onosproject.drivers.polatis.netconf.PolatisNetconfUtility.KEY_PORTDIR;


/**
 * Implements InternalConnectivity behaviour for Polatis optical switches.
 */
public class PolatisInternalConnectivity extends AbstractHandlerBehaviour
        implements InternalConnectivity {

    private final Logger log = getLogger(getClass());

    private static final String JSON_PEERPORT_PEER_KEY = "peer";
    private static final String JSON_PEERPORT_PEER_DEVICEID_KEY = "id";
    private static final String JSON_PEERPORT_PEER_PORTID_KEY = "port";
    private static final String SCHEME_NAME = "linkdiscovery";

    /**
     * Constructor just used to initiate logging when InternalConnectivity behaviour is invoked.
     */
    public PolatisInternalConnectivity() {
        log.debug("Running PolatisInternalConnectivity handler");
    }

    /**
     * Returns boolean in response to test of whether 2 ports on a given device can be internally connected.
     * <p>
     * This is a callback method required by the InternalConnectivity behaviour.
     * @param  inputPortNum  Input port number on device
     * @param  outputPortNum Output port number on device
     * @return               Indication whether internal connection can be made
     */
    @Override
    public boolean testConnectivity(PortNumber inputPortNum, PortNumber outputPortNum) {

        // NOTE: this is implemented symmetrically reflecting the fact that reverse propagation
        // is possible through a Polatis switch
        if (inputPortNum.equals(outputPortNum)) {
            log.debug("Input and output ports cannot be one and the same");
            return false;
        }
        DeviceId deviceID = handler().data().deviceId();
        DeviceService deviceService = checkNotNull(this.handler().get(DeviceService.class));
        Port inputPort = deviceService.getPort(new ConnectPoint(deviceID, inputPortNum));
        Port outputPort = deviceService.getPort(new ConnectPoint(deviceID, outputPortNum));
        if (!inputPort.isEnabled()) {
            log.debug("Input port is DISABLED");
            return false;
        }
        if (!outputPort.isEnabled()) {
            log.debug("Output port is DISABLED");
            return false;
        }
        if (!inputPort.annotations().value(KEY_PORTDIR).equals(VALUE_CC)) {
            if (inputPort.annotations().value(KEY_PORTDIR).equals(outputPort.annotations().value(KEY_PORTDIR))) {
                log.debug("Dual sided switch and provided input & output ports on same side");
                return false;
            }
        }
        // Check if either port is used in an active cross-connect
        Set<PortNumber> usedPorts = getUsedPorts();
        if (usedPorts.contains(inputPortNum)) {
            log.debug("Input port {} is used in an active cross-connect", inputPortNum);
            return false;
        }
        if (usedPorts.contains(outputPortNum)) {
            log.debug("Output port {} is used in an active cross-connect", outputPortNum);
            return false;
        }
        return true;
    }

    /**
     * Returns a set of possible output PortNumbers to which a given input port can be internally connected
     * on a given device.
     * <p>
     * This is a callback method required by the InternalConnectivity behaviour.
     * @param inputPortNum Input port number on device
     * @return             List of possible output ports
     */
    @Override
    public Set<PortNumber> getOutputPorts(PortNumber inputPortNum) {

        // NOTE: in this implementation, inputPortNum is the supplied port number and
        // can be an "input" port OR an "output" port
        // This reflects the fact that reverse propagation is possible through a Polatis switch
        // (output port -> input port)

        Set<PortNumber> ports = new HashSet<PortNumber>();
        Set<PortNumber> enabledPorts = new HashSet<PortNumber>();
        Collection<FlowEntry> flowEntries = parseConnections(this);
        // Flow entries are very simple for an all-optical circuit switch which doesn't multicast
        // e.g. one selector (IN_PORT) and one action (set output port)
        if (flowEntries.stream().map(flow -> ((PortCriterion) flow.selector()
                                             .getCriterion(Criterion.Type.IN_PORT)).port())
                                .collect(Collectors.toSet())
                                .contains(inputPortNum) ||
            flowEntries.stream().map(flow -> ((OutputInstruction) flow.treatment()
                                             .allInstructions().get(0)).port())
                                .collect(Collectors.toSet())
                                .contains(inputPortNum)) {
            log.warn("Queried port {} is already used in a cross-connect", inputPortNum);
            return ports;
        } else {
            DeviceId deviceID = handler().data().deviceId();
            DeviceService deviceService = checkNotNull(this.handler().get(DeviceService.class));
            Port inputPort = deviceService.getPort(new ConnectPoint(deviceID, inputPortNum));
            if (inputPort.annotations().value(KEY_PORTDIR).equals(VALUE_CC)) {
                ports = deviceService.getPorts(deviceID).stream()
                        .filter(p -> !p.equals(inputPortNum))
                        .map(p -> p.number())
                        .collect(Collectors.toSet());
            } else {
                ports = deviceService.getPorts(deviceID).stream()
                        .filter((p) -> {
                            Port port = deviceService.getPort(new ConnectPoint(deviceID, p.number()));
                            return !port.annotations().value(KEY_PORTDIR).equals(
                                inputPort.annotations().value(KEY_PORTDIR));
                         })
                        .map(p -> p.number())
                        .collect(Collectors.toSet());
            }
            // Remove disabled ports
            enabledPorts = ports.stream()
                .filter(p -> deviceService.getPort(new ConnectPoint(deviceID, p)).isEnabled())
                .collect(Collectors.toSet());
        }
        log.debug("Ports before filtering out used and disabled ones: " + ports);
        return filterUsedPorts(enabledPorts, flowEntries);
    }

    /**
     * Returns a set of possible input PortNumbers to which a given output port can be internally connected
     * on a given device.
     * <p>
     * This is a callback method required by the InternalConnectivity behaviour.
     * This just calls the getOutputPorts method since reverse path optical transmission is possible through
     * a Polatis switch.
     * @param outputPortNum Input port number on device
     * @return              List of possible input ports
     */
    @Override
    public Set<PortNumber> getInputPorts(PortNumber outputPortNum) {

        return getOutputPorts(outputPortNum);
    }

    private Set<PortNumber> filterUsedPorts(Set<PortNumber> ports, Collection<FlowEntry> flowEntries) {

        if (ports.isEmpty() || flowEntries.isEmpty()) {
            return ports;
        }
        for (FlowEntry flowEntry : flowEntries) {
            PortNumber inputPort = ((PortCriterion) flowEntry.selector().getCriterion(Criterion.Type.IN_PORT)).port();
            ports.remove(inputPort);
            PortNumber outputPort = ((OutputInstruction) flowEntry.treatment().allInstructions().get(0)).port();
            ports.remove(outputPort);
            log.debug("Cross-connection {}-{} removed from output port list", inputPort, outputPort);
        }
        log.debug("Ports after filtering out used ones: " + ports);
        return ports;
    }

    private Set<PortNumber> getUsedPorts() {
        Set<PortNumber> usedPorts = new HashSet<PortNumber>();
        Collection<FlowEntry> flowEntries = parseConnections(this);
        for (FlowEntry flowEntry : flowEntries) {
            usedPorts.add(((PortCriterion) flowEntry.selector().getCriterion(Criterion.Type.IN_PORT)).port());
            usedPorts.add(((OutputInstruction) flowEntry.treatment().allInstructions().get(0)).port());
        }
        return usedPorts;
    }
}
