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

package org.onosproject.patchpanel.impl;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Deactivate;
import org.onosproject.cli.net.ConnectPointCompleter;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.PortNumber;
import org.onosproject.net.flow.DefaultFlowRule;
import org.onosproject.net.flow.FlowRuleService;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.packet.PacketPriority;
import org.onosproject.net.ConnectPoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.ArrayList;
import java.util.List;

/**
 * This class acts as a software patch panel application.
 * The user specifies 2 connectpoint on the same device that he/she would like to patch.
 * Using a flow rule, the 2 connectpoints are patched.
 */
@Component(immediate = true)
@Service
public class PatchPanel implements PatchPanelService {

    // OSGI: help bundle plugin discover runtime package dependency.
    @SuppressWarnings("unused")
    private ConnectPointCompleter connectPointCompleter;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected FlowRuleService flowRuleService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    private List<ConnectPoint> previous = new ArrayList<>();
    private final Logger log = LoggerFactory.getLogger(getClass());
    private ApplicationId appId;
    private ConnectPoint cp1, cp2;


    @Activate
    protected void activate() throws NullPointerException {
        appId = coreService.registerApplication("org.onosproject.patchpanel");
        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        log.info("Stopped");
    }

    @Override
    public boolean addPatch(ConnectPoint num, ConnectPoint num2) {
        cp1 = num;
        cp2 = num2;
        if ((cp1.port().equals(cp2.port())) || (previous.contains(cp1) || previous.contains(cp2))) {
            log.info("One or both of these ports are already in use, NO FLOW");
            return false;
        } else {
            previous.add(cp1);
            previous.add(cp2);
            setFlowRuleService();
            return true;
        }
    }

    public void setFlowRuleService() {
        PortNumber outPort = cp2.port();
        PortNumber inPort = cp1.port();
        FlowRule fr = DefaultFlowRule.builder()
                .forDevice(cp1.deviceId())
                .withSelector(DefaultTrafficSelector.builder().matchInPort(inPort).build())
                .withTreatment(DefaultTrafficTreatment.builder().setOutput(outPort).build())
                .withPriority(PacketPriority.REACTIVE.priorityValue())
                .makePermanent()
                .fromApp(appId).build();

        FlowRule fr2 = DefaultFlowRule.builder()
                .forDevice(cp1.deviceId())
                .withSelector(DefaultTrafficSelector.builder().matchInPort(outPort).build())
                .withTreatment(DefaultTrafficTreatment.builder().setOutput(inPort).build())
                .withPriority(PacketPriority.REACTIVE.priorityValue())
                .makePermanent()
                .fromApp(appId).build();

        flowRuleService.applyFlowRules(fr, fr2);

    }

}
