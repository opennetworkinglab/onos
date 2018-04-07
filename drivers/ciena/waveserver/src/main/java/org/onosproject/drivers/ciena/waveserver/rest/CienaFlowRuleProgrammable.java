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
package org.onosproject.drivers.ciena.waveserver.rest;

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
import java.util.Objects;
import java.util.stream.Collectors;

import static org.slf4j.LoggerFactory.getLogger;

public class CienaFlowRuleProgrammable extends AbstractHandlerBehaviour implements FlowRuleProgrammable {
    private CienaRestDevice restCiena;
    private final Logger log = getLogger(getClass());

    @Override
    public Collection<FlowEntry> getFlowEntries() {
        DeviceId deviceId = handler().data().deviceId();
        log.debug("getting flow entries for device {}", deviceId);
        try {
            restCiena = new CienaRestDevice(handler());
        } catch (NullPointerException e) {
            log.error("unable to create CienaRestDevice:\n{}", e);
            return Collections.emptyList();
        }
        return restCiena.getFlowEntries();
    }

    @Override
    public Collection<FlowRule> applyFlowRules(Collection<FlowRule> rules) {
        log.debug("installing flow rules: {}", rules);
        try {
            restCiena = new CienaRestDevice(handler());
        } catch (NullPointerException e) {
            log.error("unable to create CienaRestDevice:\n{}", e);
            return Collections.emptyList();
        }
        // Apply the valid rules on the device
        Collection<FlowRule> added = rules.stream()
                .map(this::createCrossConnectFlowRule)
                .filter(this::installCrossConnect)
                .collect(Collectors.toList());
        restCiena.setCrossConnectCache(added);
        return added;
    }

    @Override
    public Collection<FlowRule> removeFlowRules(Collection<FlowRule> rules) {
        log.debug("removing flow rules: {}", rules);
        try {
            restCiena = new CienaRestDevice(handler());
        } catch (NullPointerException e) {
            log.error("unable to create CienaRestDevice:\n{}", e);
            return Collections.emptyList();
        }
        Collection<FlowRule> removed = rules.stream()
                .map(this::createCrossConnectFlowRule)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        restCiena.removeCrossConnectCache(removed);
        return removed;
    }

    private CrossConnectFlowRule createCrossConnectFlowRule(FlowRule r) {
        List<PortNumber> linePorts = CienaRestDevice.getLinesidePortId().stream()
                .map(PortNumber::portNumber)
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
            OchSignal signal = OchSignal.newDwdmSlot(xc.ochSignal().channelSpacing(), 0);
            return install(outPort, signal);
        }
        return false;
    }

    private boolean install(PortNumber outPort, OchSignal signal) {
        /*
         * rule is installed in three steps
         * 1- disable port
         * 2- change frequency
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
            return false;
        }
        //2- change frequency
        if (!restCiena.changeFrequency(signal, outPort)) {
            return false;
        }
        //3- enable port
        if (!restCiena.enablePort(outPort)) {
            return false;
        }
        return true;
    }

}
