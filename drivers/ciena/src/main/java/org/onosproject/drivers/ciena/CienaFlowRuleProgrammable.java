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
package org.onosproject.drivers.ciena;

import org.onosproject.net.DeviceId;
import org.onosproject.net.OchSignal;
import org.onosproject.net.PortNumber;
import org.onosproject.driver.optical.flowrule.CrossConnectFlowRule;
import org.onosproject.net.driver.AbstractHandlerBehaviour;
import org.onosproject.net.flow.FlowEntry;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.FlowRuleProgrammable;

import org.slf4j.Logger;


import java.util.List;
import java.util.Collection;
import java.util.Collections;
import java.util.stream.Collectors;

import static org.slf4j.LoggerFactory.getLogger;

public class CienaFlowRuleProgrammable extends AbstractHandlerBehaviour implements FlowRuleProgrammable {
    private CienaRestDevice restCiena;
    private final Logger log = getLogger(getClass());

    @Override
    public Collection<FlowEntry> getFlowEntries() {
        DeviceId deviceId = handler().data().deviceId();
        log.debug("getting flow entries for device {}", deviceId);
        log.debug("getFlowEntries not supported for device {}", deviceId);
        return Collections.EMPTY_LIST;
    }

    @Override
    public Collection<FlowRule> applyFlowRules(Collection<FlowRule> rules) {
        log.debug("installing flow rules: {}", rules);
        // Apply the valid rules on the device
        Collection<FlowRule> added = rules.stream()
                .map(r -> createCrossConnectFlowRule(r))
                .filter(xc -> installCrossConnect(xc))
                .collect(Collectors.toList());
        return added;
    }

    @Override
    public Collection<FlowRule> removeFlowRules(Collection<FlowRule> rules) {
        log.debug("removing flow rules: {}", rules);
        Collection<FlowRule> removed = rules.stream()
                .map(r -> createCrossConnectFlowRule(r))
                .filter(xc -> removeCrossConnect(xc))
                .collect(Collectors.toList());
        return removed;
    }

    private CrossConnectFlowRule createCrossConnectFlowRule(FlowRule r) {
        List<PortNumber> linePorts = CienaWaveserverDeviceDescription.getLinesidePortId().stream()
                .map(p -> PortNumber.portNumber(p))
                .collect(Collectors.toList());
        try {
            return new CrossConnectFlowRule(r, linePorts);
        } catch (IllegalArgumentException e) {
            log.debug("unable to create cross connect for rule:\n{}", r);
        }
        return null;
    }

    private boolean installCrossConnect(CrossConnectFlowRule xc) {
        if (xc == null) {
            return false;
        }
        // only handling lineside rule
        if (xc.isAddRule()) {
            PortNumber outPort = xc.addDrop();
            OchSignal signal = xc.ochSignal();
            return install(outPort, signal);
        }
        return false;
    }

    private boolean removeCrossConnect(CrossConnectFlowRule xc) {
        //for now setting channel to 0 for remove rule
        if (xc == null) {
            return false;
        }
        // only handling lineside rule
        if (xc.isAddRule()) {
            PortNumber outPort = xc.addDrop();
            OchSignal signal = OchSignal.newDwdmSlot(xc.ochSignal().channelSpacing(),
                                                     -CienaRestDevice.getMultiplierOffset());
            return install(outPort, signal);
        }
        return false;
    }

    private boolean install(PortNumber outPort, OchSignal signal) {
        /*
         * rule is installed in three steps
         * 1- disable port
         * 2- change channel
         * 3- enable port
         */
        try {
            restCiena = new CienaRestDevice(handler());
        } catch (NullPointerException e) {
            log.error("unable to create CienaRestDevice, {}", e);
            return false;
        }
        //1- disable port
        //blindly disabling port
        if (!restCiena.disablePort(outPort)) {
            log.error("unable to disable port {}", outPort);
            return false;
        }
        //2- change channel
        if (!restCiena.changeChannel(signal, outPort)) {
            log.error("unable to change the channel for port {}", outPort);
            return false;
        }
        //3- enable port
        if (!restCiena.enablePort(outPort)) {
            log.error("unable to enable port {}", outPort);
            return false;
        }
        return true;
    }
}
