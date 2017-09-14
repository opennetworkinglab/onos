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

package org.onosproject.p4tutorial.icmpdropper;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onosproject.app.ApplicationAdminService;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.device.DeviceEvent;
import org.onosproject.net.device.DeviceListener;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.flow.DefaultFlowRule;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.FlowRuleService;
import org.onosproject.net.flow.criteria.PiCriterion;
import org.onosproject.net.pi.model.PiPipelineProgrammable;
import org.onosproject.net.pi.runtime.PiAction;
import org.onosproject.net.pi.runtime.PiActionId;
import org.onosproject.net.pi.runtime.PiHeaderFieldId;
import org.onosproject.net.pi.runtime.PiPipeconfService;
import org.onosproject.net.pi.runtime.PiTableId;
import org.onosproject.p4tutorial.pipeconf.PipeconfFactory;
import org.slf4j.Logger;

import static org.onosproject.net.device.DeviceEvent.Type.DEVICE_ADDED;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Simple application that drops all ICMP packets.
 */
@Component(immediate = true)
public class IcmpDropper {

    private static final Logger log = getLogger(IcmpDropper.class);

    private static final String APP_NAME = "org.onosproject.p4tutorial.icmpdropper";

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    private DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    private FlowRuleService flowRuleService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    private ApplicationAdminService appService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    private CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    private PiPipeconfService piPipeconfService;

    private final DeviceListener deviceListener = new InternalDeviceListener();

    private ApplicationId appId;

    @Activate
    public void activate() {
        log.info("Starting...");

        appId = coreService.registerApplication(APP_NAME);
        // Register listener for handling new devices.
        deviceService.addListener(deviceListener);
        // Install rules to existing devices.
        deviceService.getDevices()
                .forEach(device -> installDropRule(device.id()));

        log.info("STARTED", appId.id());
    }

    @Deactivate
    public void deactivate() {
        log.info("Stopping...");

        deviceService.removeListener(deviceListener);
        flowRuleService.removeFlowRulesById(appId);

        log.info("STOPPED");
    }

    private boolean checkPipeconf(Device device) {
        if (!device.is(PiPipelineProgrammable.class)) {
            // Device is not PI-pipeline programmable. Ignore.
            return false;
        }
        if (!piPipeconfService.ofDevice(device.id()).isPresent() ||
                !piPipeconfService.ofDevice(device.id()).get().equals(PipeconfFactory.PIPECONF_ID)) {
            log.warn("Device {} has pipeconf {} instead of {}, can't install flow rule for this device",
                     device.id(), piPipeconfService.ofDevice(device.id()).get(), PipeconfFactory.PIPECONF_ID);
            return false;
        }

        return true;
    }

    private void installDropRule(DeviceId deviceId) {
        PiHeaderFieldId ipv4ProtoFieldId = PiHeaderFieldId.of("ipv4", "protocol");
        PiActionId dropActionId = PiActionId.of("_drop");

        PiCriterion piCriterion = PiCriterion.builder()
                .matchExact(ipv4ProtoFieldId, (byte) 0x01)
                .build();
        PiAction dropAction = PiAction.builder()
                .withId(dropActionId)
                .build();

        FlowRule flowRule = DefaultFlowRule.builder()
                .forDevice(deviceId)
                .forTable(PiTableId.of("ip_proto_filter_table"))
                .fromApp(appId)
                .makePermanent()
                .withPriority(1000)
                .withSelector(DefaultTrafficSelector.builder()
                                      .matchPi(piCriterion)
                                      .build())
                .withTreatment(
                        DefaultTrafficTreatment.builder()
                                .piTableAction(dropAction)
                                .build())
                .build();

        log.warn("Installing ICMP drop rule to {}", deviceId);

        flowRuleService.applyFlowRules(flowRule);
    }

    /**
     * A listener of device events that installs a rule to drop packet for each new device.
     */
    private class InternalDeviceListener implements DeviceListener {
        @Override
        public void event(DeviceEvent event) {
            Device device = event.subject();
            if (checkPipeconf(device)) {
                installDropRule(device.id());
            }
        }

        @Override
        public boolean isRelevant(DeviceEvent event) {
            // Reacts only to new devices.
            return event.type() == DEVICE_ADDED;
        }
    }
}
