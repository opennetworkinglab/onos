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

package org.onosproject.driver.pipeline;

import org.onosproject.core.CoreService;
import org.onosproject.net.DeviceId;
import org.onosproject.net.behaviour.PipelinerContext;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.FlowRuleService;
import org.onosproject.net.flowobjective.ForwardingObjective;
import org.onosproject.net.group.GroupService;

import java.util.Collection;

/**
 * Driver for software switch emulation of the OFDPA 3.0 pipeline.
 * The software switch is the CPqD OF 1.3 switch. Unfortunately the CPqD switch
 * does not handle vlan tags and mpls labels simultaneously, which requires us
 * to do some workarounds in the driver. This driver is meant for the use of
 * the cpqd switch when MPLS is required. As a result this driver works only
 * on incoming untagged packets.
 */
public class CpqdOfdpa3Pipeline extends CpqdOfdpa2Pipeline {
    @Override
    public void init(DeviceId deviceId, PipelinerContext context) {
        this.deviceId = deviceId;

        // Initialize OFDPA group handler
        groupHandler = new Ofdpa3GroupHandler();
        groupHandler.init(deviceId, context);

        serviceDirectory = context.directory();
        coreService = serviceDirectory.get(CoreService.class);
        flowRuleService = serviceDirectory.get(FlowRuleService.class);
        groupService = serviceDirectory.get(GroupService.class);
        flowObjectiveStore = context.store();
        deviceService = serviceDirectory.get(DeviceService.class);

        driverId = coreService.registerApplication(
                "org.onosproject.driver.CpqdOfdpa3Pipeline");

        initializePipeline();
    }

    @Override
    protected Collection<FlowRule> processEthTypeSpecific(ForwardingObjective fwd) {
        return processEthTypeSpecificInternal(fwd, true);
    }
}
