/*
 * Copyright 2018-present Open Networking Foundation
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

import org.onlab.osgi.ServiceDirectory;
import org.onlab.packet.EthType;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.behaviour.PipelinerContext;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flow.criteria.Criterion;
import org.onosproject.net.flow.criteria.EthTypeCriterion;
import org.onosproject.net.flowobjective.DefaultForwardingObjective;
import org.onosproject.net.flowobjective.ForwardingObjective;
import org.slf4j.Logger;

import java.util.Optional;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Cisco N9K Switch single table pipeline abstraction.
 */
public class CiscoN9kPipeliner extends DefaultSingleTablePipeline {

    private final Logger log = getLogger(getClass());
    private ServiceDirectory serviceDirectory;
    private DeviceId deviceId;
    protected DeviceService deviceService;

    @Override
    public void init(DeviceId deviceId, PipelinerContext context) {
        super.init(deviceId, context);
        this.deviceId = deviceId;
        this.serviceDirectory = context.directory();
        deviceService = serviceDirectory.get(DeviceService.class);
    }

    @Override
    public void forward(ForwardingObjective forwardObjective) {
        ForwardingObjective newFwd = forwardObjective;
        Device device = deviceService.getDevice(deviceId);

        if (forwardObjective.treatment() != null && forwardObjective.treatment().clearedDeferred()) {
            log.warn("Using 'clear actions' instruction which is not supported by {} {} {} Switch"
                            + " removing the clear deferred from the forwarding objective",
                    device.id(), device.manufacturer(), device.hwVersion());
            newFwd = forwardingObjectiveWithoutCleardDef(forwardObjective).orElse(forwardObjective);
        }

        EthTypeCriterion ethType =
                (EthTypeCriterion) newFwd.selector().getCriterion(Criterion.Type.ETH_TYPE);
        if (ethType != null && ethType.ethType() == EthType.EtherType.IPV6.ethType()) {
            log.error("IPv6 type not supported for {} {} {} Switch, " +
                            "The FlowRule associated with IPv6 is dropped.",
                    device.id(), device.manufacturer(), device.hwVersion());
            return;
        }

        super.forward(newFwd);
    }


    private Optional<ForwardingObjective> forwardingObjectiveWithoutCleardDef(ForwardingObjective forwardingObjective) {
        TrafficTreatment treatment = trafficTreatmentWithoutClearedDeffered(forwardingObjective.treatment());

        DefaultForwardingObjective.Builder foBuilder = (DefaultForwardingObjective.Builder) forwardingObjective.copy();
        foBuilder.withTreatment(treatment);

        switch (forwardingObjective.op()) {
            case ADD:
                return Optional.of(foBuilder.add(forwardingObjective.context().orElse(null)));
            case REMOVE:
                return Optional.of(foBuilder.remove(forwardingObjective.context().orElse(null)));
            default:
                log.warn("Driver does not support other operations for forwarding objective");
                return Optional.empty();
        }

    }


    private TrafficTreatment trafficTreatmentWithoutClearedDeffered(TrafficTreatment treatment) {
        return DefaultTrafficTreatment.builder(treatment)
                .notWipeDeferred()
                .build();
    }

}
