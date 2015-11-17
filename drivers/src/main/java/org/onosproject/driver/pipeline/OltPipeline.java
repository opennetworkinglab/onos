/*
 * Copyright 2015 Open Networking Laboratory
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
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.DefaultAnnotations;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.MastershipRole;
import org.onosproject.net.PortNumber;
import org.onosproject.net.behaviour.Pipeliner;
import org.onosproject.net.behaviour.PipelinerContext;
import org.onosproject.net.device.DefaultDeviceDescription;
import org.onosproject.net.device.DeviceDescription;
import org.onosproject.net.device.DeviceProvider;
import org.onosproject.net.device.DeviceProviderRegistry;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.driver.AbstractHandlerBehaviour;
import org.onosproject.net.flow.DefaultFlowRule;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.FlowRuleOperations;
import org.onosproject.net.flow.FlowRuleOperationsContext;
import org.onosproject.net.flow.FlowRuleService;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flow.instructions.Instructions;
import org.onosproject.net.flowobjective.FilteringObjective;
import org.onosproject.net.flowobjective.ForwardingObjective;
import org.onosproject.net.flowobjective.NextObjective;
import org.onosproject.net.flowobjective.ObjectiveError;
import org.onosproject.net.packet.PacketPriority;
import org.onosproject.net.provider.AbstractProvider;
import org.onosproject.net.provider.ProviderId;
import org.slf4j.Logger;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Pipeliner for OLT device.
 */
public class OltPipeline extends AbstractHandlerBehaviour implements Pipeliner {

    private final Logger log = getLogger(getClass());

    static final ProviderId PID = new ProviderId("olt", "org.onosproject.olt", true);

    static final String DEVICE = "isAccess";
    static final String OLT = "true";

    private ServiceDirectory serviceDirectory;
    private FlowRuleService flowRuleService;
    private DeviceId deviceId;
    private CoreService coreService;

    private ApplicationId appId;

    private DeviceProvider provider = new AnnotationProvider();


    @Override
    public void init(DeviceId deviceId, PipelinerContext context) {
        this.serviceDirectory = context.directory();
        this.deviceId = deviceId;
        DeviceProviderRegistry registry =
               serviceDirectory.get(DeviceProviderRegistry.class);
        flowRuleService = serviceDirectory.get(FlowRuleService.class);
        coreService = serviceDirectory.get(CoreService.class);

        /*try {
            DeviceProviderService providerService = registry.register(provider);
            providerService.deviceConnected(deviceId,
                                            description(deviceId, DEVICE, OLT));
        } finally {
            registry.unregister(provider);
        }*/

        appId = coreService.registerApplication(
                "org.onosproject.driver.OLTPipeline");

        TrafficSelector selector = DefaultTrafficSelector.builder()
                .matchEthType(EthType.EtherType.EAPOL.ethType().toShort())
                .build();

        TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                .punt()
                .build();

        FlowRule flowRule = new DefaultFlowRule(deviceId, selector, treatment,
                                                PacketPriority.CONTROL.priorityValue(),
                                                appId, 0, true, null);

        //flowRuleService.applyFlowRules(flowRule);
    }

    @Override
    public void filter(FilteringObjective filter) {
        throw new UnsupportedOperationException("OLT does not filter.");
    }

    @Override
    public void forward(ForwardingObjective fwd) {
        FlowRuleOperations.Builder flowBuilder = FlowRuleOperations.builder();

        if (fwd.flag() != ForwardingObjective.Flag.VERSATILE) {
            throw new UnsupportedOperationException(
                    "Only VERSATILE is supported.");
        }

        boolean isPunt = fwd.treatment().immediate().stream().anyMatch(i -> {
            if (i instanceof Instructions.OutputInstruction) {
                Instructions.OutputInstruction out = (Instructions.OutputInstruction) i;
                return out.port().equals(PortNumber.CONTROLLER);
            }
            return false;
        });

        if (isPunt) {
            return;
        }

        TrafficSelector selector = fwd.selector();
        TrafficTreatment treatment = fwd.treatment();

        FlowRule.Builder ruleBuilder = DefaultFlowRule.builder()
                .forDevice(deviceId)
                .withSelector(selector)
                .withTreatment(treatment)
                .fromApp(fwd.appId())
                .withPriority(fwd.priority());

        if (fwd.permanent()) {
            ruleBuilder.makePermanent();
        } else {
            ruleBuilder.makeTemporary(fwd.timeout());
        }

        switch (fwd.op()) {
            case ADD:
                flowBuilder.add(ruleBuilder.build());
                break;
            case REMOVE:
                flowBuilder.remove(ruleBuilder.build());
                break;
            default:
                log.warn("Unknown operation {}", fwd.op());
        }

        flowRuleService.apply(flowBuilder.build(new FlowRuleOperationsContext() {
            @Override
            public void onSuccess(FlowRuleOperations ops) {
                if (fwd.context().isPresent()) {
                    fwd.context().get().onSuccess(fwd);
                }
            }

            @Override
            public void onError(FlowRuleOperations ops) {
                if (fwd.context().isPresent()) {
                    fwd.context().get().onError(fwd, ObjectiveError.FLOWINSTALLATIONFAILED);
                }
            }
        }));
    }

    @Override
    public void next(NextObjective nextObjective) {
        throw new UnsupportedOperationException("OLT does not next hop.");
    }

    /**
     * Build a device description.
     *
     * @param deviceId a deviceId
     * @param key the key of the annotation
     * @param value the value for the annotation
     * @return a device description
     */
    private DeviceDescription description(DeviceId deviceId, String key, String value) {
        DeviceService deviceService = serviceDirectory.get(DeviceService.class);
        Device device = deviceService.getDevice(deviceId);

        checkNotNull(device, "Device not found in device service.");

        DefaultAnnotations.Builder builder = DefaultAnnotations.builder();
        if (value != null) {
            builder.set(key, value);
        } else {
            builder.remove(key);
        }
        return new DefaultDeviceDescription(device.id().uri(), device.type(),
                                            device.manufacturer(), device.hwVersion(),
                                            device.swVersion(), device.serialNumber(),
                                            device.chassisId(), builder.build());
    }

    /**
     * Simple ancillary provider used to annotate device.
     */
    private static final class AnnotationProvider
            extends AbstractProvider implements DeviceProvider {
        private AnnotationProvider() {
            super(PID);
        }

        @Override
        public void triggerProbe(DeviceId deviceId) {
        }

        @Override
        public void roleChanged(DeviceId deviceId, MastershipRole newRole) {
        }

        @Override
        public boolean isReachable(DeviceId deviceId) {
            return false;
        }
    }

}
