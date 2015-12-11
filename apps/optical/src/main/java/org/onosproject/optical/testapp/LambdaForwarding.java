/*
 * Copyright 2014-2015 Open Networking Laboratory
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
package org.onosproject.optical.testapp;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.IndexedLambda;
import org.onosproject.net.PortNumber;
import org.onosproject.net.device.DeviceEvent;
import org.onosproject.net.device.DeviceListener;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.flow.DefaultFlowRule;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.FlowRuleService;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flow.criteria.Criteria;
import org.onosproject.net.flow.instructions.Instructions;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Sample reactive forwarding application.
 *
 * @deprecated in Emu (ONOS 1.4).
 */
@Deprecated
//@Component(immediate = true)
public class LambdaForwarding {

    private final Logger log = getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected FlowRuleService flowRuleService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceService deviceService;

    private ApplicationId appId;

    private final InternalDeviceListener listener = new InternalDeviceListener();

    private final Map<DeviceId, Integer> uglyMap = new HashMap<>();

    @Activate
    public void activate() {
        appId = coreService.registerApplication("org.onosproject.lambdafwd");

        uglyMap.put(DeviceId.deviceId("of:0000ffffffffff01"), 1);
        uglyMap.put(DeviceId.deviceId("of:0000ffffffffff02"), 2);
        uglyMap.put(DeviceId.deviceId("of:0000ffffffffff03"), 3);

        deviceService.addListener(listener);

        for (Device d : deviceService.getDevices()) {
            pushRules(d);
        }


        log.info("Started with Application ID {}", appId.id());
    }

    @Deactivate
    public void deactivate() {
        flowRuleService.removeFlowRulesById(appId);

        log.info("Stopped");
    }


    private void pushRules(Device device) {

        TrafficSelector.Builder sbuilder = DefaultTrafficSelector.builder();
        TrafficTreatment.Builder tbuilder = DefaultTrafficTreatment.builder();
        int inport;
        int outport;
        short lambda = 10;
        byte sigType = 1;
        Integer switchNumber = uglyMap.get(device.id());
        if (switchNumber == null) {
            return;
        }

        switch (switchNumber) {
        case 1:
            inport = 10;
            outport = 20;
            sbuilder.matchInPort(PortNumber.portNumber(inport));
            tbuilder.setOutput(PortNumber.portNumber(outport))
                    .add(Instructions.modL0Lambda(new IndexedLambda(lambda)));
            break;
        case 2:
            inport = 21;
            outport = 11;
            sbuilder.add(Criteria.matchLambda(new IndexedLambda(lambda))).
                    matchInPort(PortNumber.portNumber(inport)); // match sigtype
            tbuilder.setOutput(PortNumber.portNumber(outport));
            break;
        case 3:
            inport = 30;
            outport = 31;
            sbuilder.add(Criteria.matchLambda(new IndexedLambda(lambda))).
                    matchInPort(PortNumber.portNumber(inport));
            tbuilder.setOutput(PortNumber.portNumber(outport))
                    .add(Instructions.modL0Lambda(new IndexedLambda(lambda)));
            break;
        default:
        }

        TrafficTreatment treatment = tbuilder.build();
        TrafficSelector selector = sbuilder.build();

        FlowRule f = DefaultFlowRule.builder()
                .forDevice(device.id())
                .withSelector(selector)
                .withTreatment(treatment)
                .withPriority(100)
                .fromApp(appId)
                .makeTemporary(600)
                .build();

        flowRuleService.applyFlowRules(f);



    }

    public class InternalDeviceListener implements DeviceListener {

        @Override
        public void event(DeviceEvent event) {
            switch (event.type()) {
            case DEVICE_ADDED:
                pushRules(event.subject());
                break;
            case DEVICE_AVAILABILITY_CHANGED:
                break;
            case DEVICE_REMOVED:
                break;
            case DEVICE_SUSPENDED:
                break;
            case DEVICE_UPDATED:
                break;
            case PORT_ADDED:
                break;
            case PORT_REMOVED:
                break;
            case PORT_UPDATED:
                break;
            default:
                break;

            }

        }

    }


}


