/*
 * Copyright 2015-present Open Networking Laboratory
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

package org.onosproject.segmentrouting;

import org.onlab.packet.Ethernet;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.TpPort;
import org.onosproject.cli.net.IpProtocol;
import org.onosproject.core.ApplicationId;
import org.onosproject.net.DeviceId;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flowobjective.DefaultForwardingObjective;
import org.onosproject.net.flowobjective.FlowObjectiveService;
import org.onosproject.net.flowobjective.ForwardingObjective;
import org.onosproject.segmentrouting.config.DeviceConfiguration;
import org.onosproject.store.service.EventuallyConsistentMap;
import org.slf4j.Logger;

import java.util.List;
import java.util.stream.Collectors;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Segment Routing Policy Handler.
 */
public class PolicyHandler {

    protected final Logger log = getLogger(getClass());

    private ApplicationId appId;
    private DeviceConfiguration deviceConfiguration;
    private FlowObjectiveService flowObjectiveService;
    private TunnelHandler tunnelHandler;
    private final EventuallyConsistentMap<String, Policy> policyStore;
    /**
     * Result of policy creation.
     */
    public enum Result {
        /**
         * Success.
         */
        SUCCESS,

        /**
         * The same policy exists already.
         */
        POLICY_EXISTS,

        /**
         * The policy ID exists already.
         */
        ID_EXISTS,

        /**
         * Cannot find associated tunnel.
         */
        TUNNEL_NOT_FOUND,

        /**
         * Policy was not found.
         */
        POLICY_NOT_FOUND,

        /**
         * Policy type {} is not supported yet.
         */
        UNSUPPORTED_TYPE
    }

    /**
     * Constructs policy handler.
     *
     * @param appId                segment routing application ID
     * @param deviceConfiguration  DeviceConfiguration reference
     * @param flowObjectiveService FlowObjectiveService reference
     * @param tunnelHandler        tunnel handler reference
     * @param policyStore          policy store
     */
    public PolicyHandler(ApplicationId appId,
                         DeviceConfiguration deviceConfiguration,
                         FlowObjectiveService flowObjectiveService,
                         TunnelHandler tunnelHandler,
                         EventuallyConsistentMap<String, Policy> policyStore) {
        this.appId = appId;
        this.deviceConfiguration = deviceConfiguration;
        this.flowObjectiveService = flowObjectiveService;
        this.tunnelHandler = tunnelHandler;
        this.policyStore = policyStore;
    }

    /**
     * Returns the policies.
     *
     * @return policy list
     */
    public List<Policy> getPolicies() {
        return policyStore.values()
                .stream()
                .filter(policy -> policy instanceof TunnelPolicy)
                .map(policy -> new TunnelPolicy((TunnelPolicy) policy))
                .collect(Collectors.toList());
    }

    /**
     * Creates a policy using the policy information given.
     *  @param policy policy reference to create
     *  @return ID_EXISTS if the same policy ID exists,
     *  POLICY_EXISTS if the same policy exists, TUNNEL_NOT_FOUND if the tunnel
     *  does not exists, UNSUPPORTED_TYPE if the policy type is not supported,
     *  SUCCESS if the policy is created successfully
     */
    public Result createPolicy(Policy policy) {

        if (policyStore.containsKey(policy.id())) {
            log.warn("The policy id {} exists already", policy.id());
            return Result.ID_EXISTS;
        }

        if (policyStore.containsValue(policy)) {
            log.warn("The same policy exists already");
            return Result.POLICY_EXISTS;
        }

        if (policy.type() == Policy.Type.TUNNEL_FLOW) {

            TunnelPolicy tunnelPolicy = (TunnelPolicy) policy;
            Tunnel tunnel = tunnelHandler.getTunnel(tunnelPolicy.tunnelId());
            if (tunnel == null) {
                return Result.TUNNEL_NOT_FOUND;
            }

            ForwardingObjective.Builder fwdBuilder = DefaultForwardingObjective
                    .builder()
                    .fromApp(appId)
                    .makePermanent()
                    .nextStep(tunnel.groupId())
                    .withPriority(tunnelPolicy.priority())
                    .withSelector(buildSelector(policy))
                    .withFlag(ForwardingObjective.Flag.VERSATILE);

            DeviceId source = deviceConfiguration.getDeviceId(tunnel.labelIds().get(0));
            flowObjectiveService.forward(source, fwdBuilder.add());

        } else {
            log.warn("Policy type {} is not supported yet.", policy.type());
            return Result.UNSUPPORTED_TYPE;
        }

        policyStore.put(policy.id(), policy);

        return Result.SUCCESS;
    }

    /**
     * Removes the policy given.
     *
     * @param policyInfo policy information to remove
     * @return POLICY_NOT_FOUND if the policy to remove does not exists,
     * SUCCESS if it is removed successfully
     */
    public Result removePolicy(Policy policyInfo) {

        if (policyStore.get(policyInfo.id()) != null) {
            Policy policy = policyStore.get(policyInfo.id());
            if (policy.type() == Policy.Type.TUNNEL_FLOW) {
                TunnelPolicy tunnelPolicy = (TunnelPolicy) policy;
                Tunnel tunnel = tunnelHandler.getTunnel(tunnelPolicy.tunnelId());

                ForwardingObjective.Builder fwdBuilder = DefaultForwardingObjective
                        .builder()
                        .fromApp(appId)
                        .makePermanent()
                        .withSelector(buildSelector(policy))
                        .withPriority(tunnelPolicy.priority())
                        .nextStep(tunnel.groupId())
                        .withFlag(ForwardingObjective.Flag.VERSATILE);

                DeviceId source = deviceConfiguration.getDeviceId(tunnel.labelIds().get(0));
                flowObjectiveService.forward(source, fwdBuilder.remove());

                policyStore.remove(policyInfo.id());
            }
        } else {
            log.warn("Policy {} was not found", policyInfo.id());
            return Result.POLICY_NOT_FOUND;
        }

        return Result.SUCCESS;
    }


    private TrafficSelector buildSelector(Policy policy) {

        TrafficSelector.Builder tsb = DefaultTrafficSelector.builder();
        tsb.matchEthType(Ethernet.TYPE_IPV4);
        if (policy.dstIp() != null && !policy.dstIp().isEmpty()) {
            tsb.matchIPDst(IpPrefix.valueOf(policy.dstIp()));
        }
        if (policy.srcIp() != null && !policy.srcIp().isEmpty()) {
            tsb.matchIPSrc(IpPrefix.valueOf(policy.srcIp()));
        }
        if (policy.ipProto() != null && !policy.ipProto().isEmpty()) {
            Short ipProto = IpProtocol.valueOf(policy.ipProto()).value();
            tsb.matchIPProtocol(ipProto.byteValue());
            if (IpProtocol.valueOf(policy.ipProto()).equals(IpProtocol.TCP)) {
                if (policy.srcPort() != 0) {
                    tsb.matchTcpSrc(TpPort.tpPort(policy.srcPort()));
                }
                if (policy.dstPort() != 0) {
                    tsb.matchTcpDst(TpPort.tpPort(policy.dstPort()));
                }
            } else if (IpProtocol.valueOf(policy.ipProto()).equals(IpProtocol.UDP)) {
                if (policy.srcPort() != 0) {
                    tsb.matchUdpSrc(TpPort.tpPort(policy.srcPort()));
                }
                if (policy.dstPort() != 0) {
                    tsb.matchUdpDst(TpPort.tpPort(policy.dstPort()));
                }
            }
        }

        return tsb.build();
    }

}
