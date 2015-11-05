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
package org.onosproject.cordvtn;

import org.onlab.packet.Ip4Address;
import org.onlab.util.ItemNotFoundException;
import org.onosproject.core.ApplicationId;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Port;
import org.onosproject.net.behaviour.ExtensionTreatmentResolver;
import org.onosproject.net.driver.DefaultDriverData;
import org.onosproject.net.driver.DefaultDriverHandler;
import org.onosproject.net.driver.Driver;
import org.onosproject.net.driver.DriverHandler;
import org.onosproject.net.driver.DriverService;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flow.instructions.ExtensionPropertyException;
import org.onosproject.net.flow.instructions.ExtensionTreatment;
import org.onosproject.net.flowobjective.DefaultForwardingObjective;
import org.onosproject.net.flowobjective.FlowObjectiveService;
import org.onosproject.net.flowobjective.ForwardingObjective;
import org.slf4j.Logger;

import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.onosproject.net.flow.instructions.ExtensionTreatmentType.ExtensionTreatmentTypes.NICIRA_SET_TUNNEL_DST;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Populates rules for virtual tenant network.
 */
public final class CordVtnRuleInstaller {
    protected final Logger log = getLogger(getClass());

    private static final int DEFAULT_PRIORITY = 5000;

    private final ApplicationId appId;
    private final FlowObjectiveService flowObjectiveService;
    private final DriverService driverService;
    private final String tunnelType;

    /**
     * Creates a new rule installer.
     *
     * @param appId application id
     * @param flowObjectiveService flow objective service
     * @param driverService driver service
     * @param tunnelType tunnel type
     */
    public CordVtnRuleInstaller(ApplicationId appId,
                                FlowObjectiveService flowObjectiveService,
                                DriverService driverService,
                                String tunnelType) {
        this.appId = appId;
        this.flowObjectiveService = flowObjectiveService;
        this.driverService = driverService;
        this.tunnelType = checkNotNull(tunnelType);
    }

    /**
     * Installs flow rules for tunnel in traffic.
     *
     * @param deviceId device id to install flow rules
     * @param inPort in port
     * @param dstInfos list of destination info
     */
    public void installFlowRulesTunnelIn(DeviceId deviceId, Port inPort, List<DestinationInfo> dstInfos) {
        dstInfos.stream().forEach(dstInfo -> {
            ForwardingObjective.Builder fBuilder = vtnRulesSameNode(inPort, dstInfo);
            if (fBuilder != null) {
                flowObjectiveService.forward(deviceId, fBuilder.add());
            }
        });
    }

    /**
     * Installs flow rules for local in traffic.
     *
     * @param deviceId device id to install flow rules
     * @param inPort in port
     * @param dstInfos list of destination info
     */
    public void installFlowRulesLocalIn(DeviceId deviceId, Port inPort, List<DestinationInfo> dstInfos) {
        dstInfos.stream().forEach(dstInfo -> {
            ForwardingObjective.Builder fBuilder = isTunnelPort(dstInfo.output()) ?
                    vtnRulesRemoteNode(deviceId, inPort, dstInfo) : vtnRulesSameNode(inPort, dstInfo);

            if (fBuilder != null) {
                flowObjectiveService.forward(deviceId, fBuilder.add());
            }
        });
    }

    /**
     * Uninstalls flow rules associated with a given port from a given device.
     *
     * @param deviceId device id
     * @param inPort port associated with removed host
     * @param dstInfos list of destination info
     */
    public void uninstallFlowRules(DeviceId deviceId, Port inPort, List<DestinationInfo> dstInfos) {
        dstInfos.stream().forEach(dstInfo -> {
            ForwardingObjective.Builder fBuilder = isTunnelPort(dstInfo.output()) ?
                    vtnRulesRemoteNode(deviceId, inPort, dstInfo) : vtnRulesSameNode(inPort, dstInfo);

            if (fBuilder != null) {
                flowObjectiveService.forward(deviceId, fBuilder.remove());
            }
        });
    }

    /**
     * Returns forwarding objective builder to provision basic virtual tenant network.
     * This method cares for the traffics whose source and destination device is the same.
     *
     * @param inPort in port
     * @param dstInfo destination information
     * @return forwarding objective builder
     */
    private ForwardingObjective.Builder vtnRulesSameNode(Port inPort, DestinationInfo dstInfo) {
        checkArgument(inPort.element().id().equals(dstInfo.output().element().id()));

        TrafficSelector.Builder sBuilder = DefaultTrafficSelector.builder();
        TrafficTreatment.Builder tBuilder = DefaultTrafficTreatment.builder();

        sBuilder.matchInPort(inPort.number())
                .matchEthDst(dstInfo.mac());
        if (isTunnelPort(inPort)) {
            sBuilder.matchTunnelId(dstInfo.tunnelId());
        }

        tBuilder.setOutput(dstInfo.output().number());

        return DefaultForwardingObjective.builder()
                .withSelector(sBuilder.build())
                .withTreatment(tBuilder.build())
                .withPriority(DEFAULT_PRIORITY)
                .withFlag(ForwardingObjective.Flag.VERSATILE)
                .fromApp(appId)
                .makePermanent();
    }

    /**
     * Returns forwarding objective builder to provision basic virtual tenant network.
     * This method cares for the traffics whose source and destination is not the same.
     *
     * @param deviceId device id to install flow rules
     * @param inPort in port
     * @param dstInfo destination information
     * @return forwarding objective, or null if it fails to build it
     */
    private ForwardingObjective.Builder vtnRulesRemoteNode(DeviceId deviceId, Port inPort, DestinationInfo dstInfo) {
        checkArgument(isTunnelPort(dstInfo.output()));

        TrafficSelector.Builder sBuilder = DefaultTrafficSelector.builder();
        TrafficTreatment.Builder tBuilder = DefaultTrafficTreatment.builder();

        ExtensionTreatment extTreatment =
                getTunnelDstInstruction(deviceId, dstInfo.remoteIp().getIp4Address());
        if (extTreatment == null) {
            return null;
        }

        sBuilder.matchInPort(inPort.number())
                .matchEthDst(dstInfo.mac());

        tBuilder.extension(extTreatment, deviceId)
                .setTunnelId(dstInfo.tunnelId())
                .setOutput(dstInfo.output().number());

        return DefaultForwardingObjective.builder()
                .withSelector(sBuilder.build())
                .withTreatment(tBuilder.build())
                .withPriority(DEFAULT_PRIORITY)
                .withFlag(ForwardingObjective.Flag.VERSATILE)
                .fromApp(appId)
                .makePermanent();
    }

    /**
     * Checks if a given port is tunnel interface or not.
     * It assumes the tunnel interface contains tunnelType string in its name.
     *
     * @param port port
     * @return true if the port is tunnel interface, false otherwise.
     */
    private boolean isTunnelPort(Port port) {
        return port.annotations().value("portName").contains(tunnelType);
    }

    /**
     * Returns extension instruction to set tunnel destination.
     *
     * @param deviceId device id
     * @param remoteIp tunnel destination address
     * @return extension treatment or null if it fails to get instruction
     */
    private ExtensionTreatment getTunnelDstInstruction(DeviceId deviceId, Ip4Address remoteIp) {
        try {
            Driver driver = driverService.getDriver(deviceId);
            DriverHandler handler = new DefaultDriverHandler(new DefaultDriverData(driver, deviceId));
            ExtensionTreatmentResolver resolver =  handler.behaviour(ExtensionTreatmentResolver.class);

            ExtensionTreatment treatment = resolver.getExtensionInstruction(NICIRA_SET_TUNNEL_DST.type());
            treatment.setPropertyValue("tunnelDst", remoteIp);

            return treatment;
        } catch (ItemNotFoundException | UnsupportedOperationException | ExtensionPropertyException e) {
            log.error("Failed to get extension instruction to set tunnel dst {}", deviceId);
            return null;
        }
    }
}
