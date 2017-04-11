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
package org.onosproject.openstacknetworking.impl;

import org.onlab.packet.Ip4Address;
import org.onosproject.core.ApplicationId;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.behaviour.ExtensionTreatmentResolver;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flow.instructions.ExtensionPropertyException;
import org.onosproject.net.flow.instructions.ExtensionTreatment;
import org.onosproject.net.flowobjective.DefaultForwardingObjective;
import org.onosproject.net.flowobjective.FlowObjectiveService;
import org.onosproject.net.flowobjective.ForwardingObjective;
import org.slf4j.Logger;

import static org.onosproject.net.flow.instructions.ExtensionTreatmentType.ExtensionTreatmentTypes.NICIRA_SET_TUNNEL_DST;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Provides common methods to help populating flow rules for SONA applications.
 */
public final class RulePopulatorUtil {

    protected static final Logger log = getLogger(RulePopulatorUtil.class);

    private static final String TUNNEL_DST = "tunnelDst";

    private RulePopulatorUtil() {
    }

    /**
     * Returns tunnel destination extension treatment object.
     *
     * @param deviceService driver service
     * @param deviceId device id to apply this treatment
     * @param remoteIp tunnel destination ip address
     * @return extension treatment
     */
    public static ExtensionTreatment buildExtension(DeviceService deviceService,
                                                    DeviceId deviceId,
                                                    Ip4Address remoteIp) {
        Device device = deviceService.getDevice(deviceId);
        if (device == null) {
            return null;
        }
        if (!device.is(ExtensionTreatmentResolver.class)) {
            log.error("The extension treatment is not supported");
            return null;
        }
        ExtensionTreatmentResolver resolver = device.as(ExtensionTreatmentResolver.class);
        ExtensionTreatment treatment = resolver.getExtensionInstruction(NICIRA_SET_TUNNEL_DST.type());
        try {
            treatment.setPropertyValue(TUNNEL_DST, remoteIp);
            return treatment;
        } catch (ExtensionPropertyException e) {
            log.warn("Failed to get tunnelDst extension treatment for {}", deviceId);
            return null;
        }
    }

    /**
     * Adds flow rules with the supplied information.
     *
     * @param flowObjectiveService flow objective service
     * @param appId application id
     * @param deviceId device id to remove this flow rule
     * @param selector traffic selector
     * @param treatment traffic treatment
     * @param flag flag
     * @param priority priority
     * @param install populate flows if true, remove them otherwise
     */
    public static void setRule(FlowObjectiveService flowObjectiveService,
                               ApplicationId appId,
                               DeviceId deviceId,
                               TrafficSelector selector,
                               TrafficTreatment treatment,
                               ForwardingObjective.Flag flag,
                               int priority,
                               boolean install) {
        ForwardingObjective.Builder foBuilder = DefaultForwardingObjective.builder()
                .withSelector(selector)
                .withTreatment(treatment)
                .withFlag(flag)
                .withPriority(priority)
                .fromApp(appId);

        if (install) {
            flowObjectiveService.forward(deviceId, foBuilder.add());
        } else {
            flowObjectiveService.forward(deviceId, foBuilder.remove());
        }
    }
}
