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

package org.onosproject.segmentrouting;

import org.onlab.packet.Ethernet;
import org.onlab.packet.IpPrefix;
import org.onosproject.cli.net.IpProtocol;
import org.onosproject.net.DeviceId;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flowobjective.DefaultForwardingObjective;
import org.onosproject.net.flowobjective.ForwardingObjective;
import org.onosproject.store.service.EventuallyConsistentMap;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Segment Routing Policy Handler.
 */
public class PolicyHandler {

    protected final Logger log = getLogger(getClass());

    // FIXME: references to manager components should be avoided
    private final SegmentRoutingManager srManager;
    private final EventuallyConsistentMap<String, Policy> policyStore;

    /**
     * Creates a reference.
     *
     * @param srManager segment routing manager
     * @param policyStore policy store
     */
    public PolicyHandler(SegmentRoutingManager srManager,
                         EventuallyConsistentMap<String, Policy> policyStore) {
        this.srManager = srManager;
        this.policyStore = policyStore;
    }

    /**
     * Returns the policies.
     *
     * @return policy list
     */
    public List<Policy> getPolicies() {
        List<Policy> policies = new ArrayList<>();
        policyStore.values().forEach(policy -> policies.add(
                new TunnelPolicy((TunnelPolicy) policy)));

        return policies;
    }

    /**
     * Creates a policy using the policy information given.
     *
     * @param policy policy reference to create
     */
    public void createPolicy(Policy policy) {

        if (policyStore.containsKey(policy.id())) {
            log.warn("The policy id {} exists already", policy.id());
            return;
        }

        if (policyStore.containsValue(policy)) {
            log.warn("The same policy exists already");
            return;
        }

        if (policy.type() == Policy.Type.TUNNEL_FLOW) {

            TunnelPolicy tunnelPolicy = (TunnelPolicy) policy;
            Tunnel tunnel = srManager.getTunnel(tunnelPolicy.tunnelId());
            if (tunnel == null) {
                log.error("Tunnel {} does not exists");
                return;
            }

            ForwardingObjective.Builder fwdBuilder = DefaultForwardingObjective
                    .builder()
                    .fromApp(srManager.appId)
                    .makePermanent()
                    .nextStep(tunnel.groupId())
                    .withPriority(tunnelPolicy.priority())
                    .withSelector(buildSelector(policy))
                    .withFlag(ForwardingObjective.Flag.VERSATILE);

            DeviceId source = srManager.deviceConfiguration.getDeviceId(tunnel.labelIds().get(0));
            srManager.flowObjectiveService.forward(source, fwdBuilder.add());

        } else {
            log.warn("Policy type {} is not supported yet.", policy.type());
        }

        policyStore.put(policy.id(), policy);
    }

    /**
     * Removes the policy given.
     *
     * @param policyInfo policy information to remove
     */
    public void removePolicy(Policy policyInfo) {

        if (policyStore.get(policyInfo.id()) != null) {
            Policy policy = policyStore.get(policyInfo.id());
            if (policy.type() == Policy.Type.TUNNEL_FLOW) {
                TunnelPolicy tunnelPolicy = (TunnelPolicy) policy;
                Tunnel tunnel = srManager.getTunnel(tunnelPolicy.tunnelId());

                ForwardingObjective.Builder fwdBuilder = DefaultForwardingObjective
                        .builder()
                        .fromApp(srManager.appId)
                        .makePermanent()
                        .withSelector(buildSelector(policy))
                        .withPriority(tunnelPolicy.priority())
                        .nextStep(tunnel.groupId())
                        .withFlag(ForwardingObjective.Flag.VERSATILE);

                DeviceId source = srManager.deviceConfiguration.getDeviceId(tunnel.labelIds().get(0));
                srManager.flowObjectiveService.forward(source, fwdBuilder.remove());

                policyStore.remove(policyInfo.id());
            }
        } else {
            log.warn("Policy {} was not found", policyInfo.id());
        }
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
            Short ipProto = Short.valueOf(IpProtocol.valueOf(policy.ipProto()).value());
            tsb.matchIPProtocol(ipProto.byteValue());
            if (IpProtocol.valueOf(policy.ipProto()).equals(IpProtocol.TCP)) {
                if (policy.srcPort() != 0) {
                    tsb.matchTcpSrc(policy.srcPort());
                }
                if (policy.dstPort() != 0) {
                    tsb.matchTcpDst(policy.dstPort());
                }
            } else if (IpProtocol.valueOf(policy.ipProto()).equals(IpProtocol.UDP)) {
                if (policy.srcPort() != 0) {
                    tsb.matchUdpSrc(policy.srcPort());
                }
                if (policy.dstPort() != 0) {
                    tsb.matchUdpDst(policy.dstPort());
                }
            }
        }

        return tsb.build();
    }

}
