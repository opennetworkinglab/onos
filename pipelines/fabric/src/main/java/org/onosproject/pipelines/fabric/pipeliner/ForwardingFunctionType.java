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

package org.onosproject.pipelines.fabric.pipeliner;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import org.onlab.packet.MacAddress;
import org.onosproject.net.flow.criteria.Criterion;
import org.onosproject.net.flow.criteria.EthCriterion;
import org.onosproject.net.flowobjective.ForwardingObjective;

import java.util.Map;
import java.util.Set;

import static org.onosproject.net.flow.criteria.Criterion.Type.ETH_DST;
import static org.onosproject.net.flow.criteria.Criterion.Type.ETH_TYPE;
import static org.onosproject.net.flow.criteria.Criterion.Type.IPV4_DST;
import static org.onosproject.net.flow.criteria.Criterion.Type.IPV6_DST;
import static org.onosproject.net.flow.criteria.Criterion.Type.MPLS_BOS;
import static org.onosproject.net.flow.criteria.Criterion.Type.MPLS_LABEL;
import static org.onosproject.net.flow.criteria.Criterion.Type.VLAN_VID;

public enum ForwardingFunctionType {
    /**
     * L2 unicast, with vlan id + mac address criterion.
     */
    L2_UNICAST,

    /**
     * L2 broadcast, with vlan id criterion only.
     */
    L2_BROADCAST,

    /**
     * IPv4 unicast, with EtherType and IPv4 unicast destination address.
     */
    IPV4_UNICAST,

    /**
     * IPv4 multicast, with EtherType and IPv4 multicast destination address.
     */
    IPV4_MULTICAST,

    /**
     * IPv6 unicast, with EtherType and IPv6 unicast destination address.
     */
    IPV6_UNICAST,

    /**
     * IPv6 multicast, with EtherType and IPv6 multicast destination address.
     */
    IPV6_MULTICAST,

    /**
     * MPLS, with EtherType, MPLS label and MPLS BOS(true) criterion.
     */
    MPLS,

    /**
     * Pseudo-wire, with EtherType, MPLS label and MPLS BOS(false) criterion.
     */
    PW,

    /**
     * Unsupported type.
     */
    UNSUPPORTED;

    // Different criteria combinations for different FFT
    private static final Set<Criterion.Type> L2_UNI_CRITERIA_TYPE =
            ImmutableSet.of(VLAN_VID, ETH_DST);
    private static final Set<Criterion.Type> L2_BRC_CRITERIA_TYPE =
            ImmutableSet.of(VLAN_VID);
    private static final Set<Criterion.Type> IPV4_UNI_CRITERIA_TYPE =
            ImmutableSet.of(ETH_TYPE, IPV4_DST);
    private static final Set<Criterion.Type> IPV4_MCAST_CRITERIA_TYPE =
            ImmutableSet.of(ETH_TYPE, VLAN_VID, IPV4_DST);
    private static final Set<Criterion.Type> IPV6_UNI_CRITERIA_TYPE =
            ImmutableSet.of(ETH_TYPE, IPV6_DST);
    private static final Set<Criterion.Type> IPV6_MCAST_CRITERIA_TYPE =
            ImmutableSet.of(ETH_TYPE, VLAN_VID, IPV6_DST);
    private static final Set<Criterion.Type> MPLS_UNI_CRITERIA_TYPE =
            ImmutableSet.of(ETH_TYPE, MPLS_LABEL, MPLS_BOS);

    private static final Map<Set<Criterion.Type>, ForwardingFunctionType> FFT_MAP =
            ImmutableMap.<Set<Criterion.Type>, ForwardingFunctionType>builder()
                    .put(L2_UNI_CRITERIA_TYPE, L2_UNICAST)
                    .put(L2_BRC_CRITERIA_TYPE, L2_BROADCAST)
                    .put(IPV4_UNI_CRITERIA_TYPE, IPV4_UNICAST)
                    .put(IPV4_MCAST_CRITERIA_TYPE, IPV4_MULTICAST)
                    .put(IPV6_UNI_CRITERIA_TYPE, IPV6_UNICAST)
                    .put(IPV6_MCAST_CRITERIA_TYPE, IPV6_MULTICAST)
                    .put(MPLS_UNI_CRITERIA_TYPE, MPLS)
                    .build();

    /**
     * Gets forwarding function type of the forwarding objective.
     *
     * @param fwd the forwarding objective
     * @return forwarding function type of the forwarding objective
     */
    public static ForwardingFunctionType getForwardingFunctionType(ForwardingObjective fwd) {
        Set<Criterion.Type> criteriaType = Sets.newHashSet();
        //omit the criterion of ethDst type with the value of MacAddress.NONE since it indicates L2 broadcast
        //if not, the objective is treated as L2 unicast
        fwd.selector().criteria().stream().filter(criterion -> !ethDstIndicatesBroadcast(criterion))
                .map(Criterion::type)
                .forEach(criteriaType::add);

        if (fwd.meta() != null) {
            fwd.meta().criteria().stream().filter(criterion -> !ethDstIndicatesBroadcast(criterion))
                    .map(Criterion::type)
                    .forEach(criteriaType::add);
        }

        return FFT_MAP.getOrDefault(criteriaType, UNSUPPORTED);
    }

    /**
     * Segment Routing (SR) app. sets ethDst criterion to MacAddress.NONE for broadcast rules
     * and demands drivers to treat the flow rules containing this criterion as broadcast rule.
     *
     * This method checks the type and value of the criterion and detects whether it constitutes
     * broadcast or not.
     *
     * For more details check RoutingRulePopulator.updateSubnetBroadcastRule method of SR app.
     *
     * @param criterion Criterion object
     * @return true if the type is ETH_DST and the mac value equals to MacAddress.NONE; false otherwise.
     */
    private static boolean ethDstIndicatesBroadcast(Criterion criterion) {
        return criterion.type().equals(Criterion.Type.ETH_DST) &&
                ((EthCriterion) criterion).mac().equals(MacAddress.NONE);
    }
}
