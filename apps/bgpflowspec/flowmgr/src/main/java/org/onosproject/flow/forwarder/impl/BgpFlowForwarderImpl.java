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
package org.onosproject.flow.forwarder.impl;

import org.onosproject.core.ApplicationId;
import org.onosproject.flow.forwarder.BgpFlowForwarderService;

import org.onosproject.flowapi.ExtFlowContainer;
import org.onosproject.net.Annotations;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.behaviour.ExtensionSelectorResolver;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.driver.DriverHandler;
import org.onosproject.net.driver.DriverService;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flow.criteria.ExtensionSelector;
import org.onosproject.net.flowobjective.DefaultForwardingObjective;
import org.onosproject.net.flowobjective.FlowObjectiveService;
import org.onosproject.net.flowobjective.ForwardingObjective;
import org.onosproject.net.flowobjective.ForwardingObjective.Flag;
import org.onosproject.net.flowobjective.Objective;
import org.slf4j.Logger;

import java.util.Iterator;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onosproject.net.flow.criteria.ExtensionSelectorType.ExtensionSelectorTypes.EXT_MATCH_FLOW_TYPE;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Provides Bgp flow forwarder implementation.
 */
public class BgpFlowForwarderImpl implements BgpFlowForwarderService {

    protected DriverService driverService;
    protected DeviceService deviceService;
    protected FlowObjectiveService flowObjectiveService;
    private final Logger log = getLogger(getClass());
    protected ApplicationId appId;

    private static final String BGP_FLOW_CONTAINER_NOT_NULL = "Bgp flow container cannot be null";
    private static final String APP_ID_NOT_NULL = "Application-Id cannot be null";
    public static final String FLOW_PEER = "flowPeer";

    /**
     * Default constructor.
     */
    public BgpFlowForwarderImpl() {
    }

    /**
     * Explicit constructor.
     *
     * @param appId Application id
     * @param flowObjectiveService flow service
     * @param deviceService device service
     * @param driverService driver service
     */
    public BgpFlowForwarderImpl(ApplicationId appId, FlowObjectiveService flowObjectiveService,
                                DeviceService deviceService, DriverService driverService) {
        this.appId = checkNotNull(appId, APP_ID_NOT_NULL);
        this.flowObjectiveService = flowObjectiveService;
        this.deviceService = deviceService;
        this.driverService = driverService;
    }

    @Override
    public boolean installForwardingRule(ExtFlowContainer container) {
        checkNotNull(container, BGP_FLOW_CONTAINER_NOT_NULL);
        return pushBgpFlowRuleForwarder(container, Objective.Operation.ADD);
    }

    @Override
    public boolean unInstallForwardingRule(ExtFlowContainer container) {
        checkNotNull(container, BGP_FLOW_CONTAINER_NOT_NULL);
        return pushBgpFlowRuleForwarder(container, Objective.Operation.REMOVE);
    }

    /**
     * Find the bgp device and push the rule.
     *
     * @param container is a flow rule container
     * @param type either add or remove the service rule
     * @return a true if success else false
     */
    public boolean pushBgpFlowRuleForwarder(ExtFlowContainer container,
                                             Objective.Operation type) {
        DeviceId deviceId = null;

        Iterable<Device> devices = deviceService.getAvailableDevices();
        Iterator<Device> itr = devices.iterator();
        while (itr.hasNext()) {
            DeviceId tmp = itr.next().id();
            if (tmp.toString().equals(container.deviceId())) {
                if (validatePeer(tmp)) {
                    deviceId = tmp;
                    break;
                }
            }
        }

        if (deviceId != null) {
            // pack traffic selector
            TrafficSelector.Builder selector = packTrafficSelector(deviceId, container);

            // pack traffic treatment
            TrafficTreatment.Builder treatment = packTrafficTreatment(container);

            sendBgpFlowRuleForwarder(selector, treatment, deviceId, type);
        } else {
            log.error("Bgp devices are not available..");
            return false;
        }

        return true;
    }

    /**
     * Validates the device id is a flow peer or not.
     *
     * @param deviceId device to which the flow needed to be pushed.
     * @return true if success else false
     */
    boolean validatePeer(DeviceId deviceId) {
        boolean ret = false;
        Device d = deviceService.getDevice(deviceId);
        Annotations a = d != null ? d.annotations() : null;
        String ipAddress = a.value(FLOW_PEER);
        if (ipAddress != null) {
            ret = true;
        }
        return ret;
    }

    /**
     * Traffic selector builder function.
     *
     * @param deviceId device id.
     * @param container container need to be pushed.
     * @return the traffic selector builder
     */
    public TrafficSelector.Builder packTrafficSelector(DeviceId deviceId, ExtFlowContainer container) {
        TrafficSelector.Builder selector = DefaultTrafficSelector.builder();

        DriverHandler handler = driverService.createHandler(deviceId);
        ExtensionSelectorResolver resolver = handler.behaviour(ExtensionSelectorResolver.class);
        ExtensionSelector bgpExtSelector = resolver.getExtensionSelector(EXT_MATCH_FLOW_TYPE.type());

        try {
            bgpExtSelector.setPropertyValue("container", container);
        } catch (Exception e) {
            log.error("Failed to get extension instruction for bgp flow {}", deviceId);
        }

        selector.extension(bgpExtSelector, deviceId);
        return selector;
    }

    /**
     * Traffic treatment builder function.
     *
     * @param container container need to be pushed.
     * @return the traffic treatment builder
     */
    public TrafficTreatment.Builder packTrafficTreatment(ExtFlowContainer container) {
        TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder();

        return treatment;
    }

    /**
     * Send bgp flow forwarder to bgp provider.
     *
     * @param selector traffic selector
     * @param treatment traffic treatment
     * @param deviceId device id
     * @param type operation type
     */
    public void sendBgpFlowRuleForwarder(TrafficSelector.Builder selector, TrafficTreatment.Builder treatment,
            DeviceId deviceId, Objective.Operation type) {
        ForwardingObjective.Builder objective = DefaultForwardingObjective.builder().withTreatment(treatment.build())
                .withSelector(selector.build()).fromApp(appId).makePermanent().withFlag(Flag.VERSATILE);
        if (type.equals(Objective.Operation.ADD)) {
            log.debug("ADD");
            flowObjectiveService.forward(deviceId, objective.add());
        } else {
            log.debug("REMOVE");
            flowObjectiveService.forward(deviceId, objective.remove());
        }
    }
}
