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

package org.onosproject.pipelines.fabric.impl.behaviour.pipeliner;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.onosproject.net.flow.criteria.Criterion;
import org.onosproject.net.flowobjective.ForwardingObjective;
import org.slf4j.Logger;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.onosproject.net.flow.criteria.Criterion.Type.ETH_DST;
import static org.onosproject.net.flow.criteria.Criterion.Type.ETH_TYPE;
import static org.onosproject.net.flow.criteria.Criterion.Type.IPV4_DST;
import static org.onosproject.net.flow.criteria.Criterion.Type.IPV6_DST;
import static org.onosproject.net.flow.criteria.Criterion.Type.MPLS_BOS;
import static org.onosproject.net.flow.criteria.Criterion.Type.MPLS_LABEL;
import static org.onosproject.net.flow.criteria.Criterion.Type.VLAN_VID;
import static org.onosproject.pipelines.fabric.impl.behaviour.pipeliner.Commons.MATCH_ETH_DST_NONE;
import static org.onosproject.pipelines.fabric.impl.behaviour.pipeliner.Commons.MATCH_ETH_TYPE_IPV4;
import static org.onosproject.pipelines.fabric.impl.behaviour.pipeliner.Commons.MATCH_ETH_TYPE_IPV6;
import static org.onosproject.pipelines.fabric.impl.behaviour.pipeliner.Commons.MATCH_ETH_TYPE_MPLS;
import static org.onosproject.pipelines.fabric.impl.behaviour.pipeliner.Commons.MATCH_MPLS_BOS_FALSE;
import static org.onosproject.pipelines.fabric.impl.behaviour.pipeliner.Commons.MATCH_MPLS_BOS_TRUE;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Forwarding function types (FFTs) that can represent a given forwarding
 * objective. Each FFT is defined by a subset of criterion types expected to be
 * found in the selector of the given objective, and, optionally, by their
 * respective values (criterion instances) to match or to mismatch.
 */
enum ForwardingFunctionType {
    /**
     * L2 unicast.
     */
    L2_UNICAST(
            Sets.newHashSet(VLAN_VID, ETH_DST), // Expected criterion types.
            Collections.emptyList(), // Criteria to match.
            Lists.newArrayList(MATCH_ETH_DST_NONE)), // Criteria NOT to match.

    /**
     * L2 broadcast.
     */
    L2_BROADCAST(
            Sets.newHashSet(VLAN_VID, ETH_DST),
            Lists.newArrayList(MATCH_ETH_DST_NONE),
            Collections.emptyList()),
    L2_BROADCAST_ALIAS(
            Sets.newHashSet(VLAN_VID),
            Collections.emptyList(),
            Collections.emptyList(),
            L2_BROADCAST), // (Optional) FFT to return if selected.

    /**
     * IPv4 unicast.
     */
    IPV4_ROUTING(
            Sets.newHashSet(ETH_TYPE, IPV4_DST),
            Lists.newArrayList(MATCH_ETH_TYPE_IPV4),
            Collections.emptyList()),

    /**
     * IPv4 multicast.
     */
    IPV4_ROUTING_MULTICAST(
            Sets.newHashSet(ETH_TYPE, VLAN_VID, IPV4_DST),
            Lists.newArrayList(MATCH_ETH_TYPE_IPV4),
            Collections.emptyList()),

    /**
     * IPv6 unicast.
     */
    IPV6_ROUTING(
            Sets.newHashSet(ETH_TYPE, IPV6_DST),
            Lists.newArrayList(MATCH_ETH_TYPE_IPV6),
            Collections.emptyList()),

    /**
     * IPv6 multicast.
     */
    IPV6_ROUTING_MULTICAST(
            Sets.newHashSet(ETH_TYPE, VLAN_VID, IPV6_DST),
            Lists.newArrayList(MATCH_ETH_TYPE_IPV6),
            Collections.emptyList()),

    /**
     * MPLS segment routing.
     */
    MPLS_SEGMENT_ROUTING(
            Sets.newHashSet(ETH_TYPE, MPLS_LABEL, MPLS_BOS),
            Lists.newArrayList(MATCH_ETH_TYPE_MPLS, MATCH_MPLS_BOS_TRUE),
            Collections.emptyList()),

    /**
     * Pseudo-wire.
     */
    PSEUDO_WIRE(
            Sets.newHashSet(ETH_TYPE, MPLS_LABEL, MPLS_BOS),
            Lists.newArrayList(MATCH_ETH_TYPE_MPLS, MATCH_MPLS_BOS_FALSE),
            Collections.emptyList()),

    /**
     * Unsupported type.
     */
    UNKNOWN(
            Collections.emptySet(),
            Collections.emptyList(),
            Collections.emptyList());

    private static final Logger log = getLogger(ForwardingFunctionType.class);

    private final Set<Criterion.Type> expectedCriterionTypes;
    private final Map<Criterion.Type, List<Criterion>> matchCriteria;
    private final Map<Criterion.Type, List<Criterion>> mismatchCriteria;
    private final ForwardingFunctionType originalType;

    /**
     * Creates a new FFT.
     *
     * @param expectedCriterionTypes expected criterion types
     * @param matchCriteria          criterion instances to match
     * @param mismatchCriteria       criterion instance not to be matched
     */
    ForwardingFunctionType(Set<Criterion.Type> expectedCriterionTypes,
                           Collection<Criterion> matchCriteria,
                           Collection<Criterion> mismatchCriteria) {
        this(expectedCriterionTypes, matchCriteria, mismatchCriteria, null);
    }

    /**
     * Creates a new alias FFT that if matched, should return the given original
     * FFT.
     *
     * @param expectedCriterionTypes expected criterion types
     * @param matchCriteria          criterion instances to match
     * @param mismatchCriteria       criterion instance not to be matched
     * @param original               original FFT to return
     */
    ForwardingFunctionType(Set<Criterion.Type> expectedCriterionTypes,
                           Collection<Criterion> matchCriteria,
                           Collection<Criterion> mismatchCriteria,
                           ForwardingFunctionType original) {
        this.expectedCriterionTypes = ImmutableSet.copyOf(expectedCriterionTypes);
        this.matchCriteria = typeToCriteriaMap(matchCriteria);
        this.mismatchCriteria = typeToCriteriaMap(mismatchCriteria);
        this.originalType = original == null ? this : original;
    }

    /**
     * Attempts to guess the forwarding function type of the given forwarding
     * objective.
     *
     * @param fwd the forwarding objective
     * @return forwarding function type. {@link #UNKNOWN} if the FFT cannot be
     * determined.
     */
    public static ForwardingFunctionType getForwardingFunctionType(ForwardingObjective fwd) {
        final Set<Criterion> criteria = criteriaIncludingMeta(fwd);
        final Set<Criterion.Type> criterionTypes = criteria.stream()
                .map(Criterion::type).collect(Collectors.toSet());

        final List<ForwardingFunctionType> candidates = Arrays.stream(ForwardingFunctionType.values())
                // Keep FFTs which expected criterion types are the same found
                // in the fwd objective.
                .filter(fft -> fft.expectedCriterionTypes.equals(criterionTypes))
                // Keep FFTs which match criteria are found in the fwd objective.
                .filter(fft -> matchFft(criteria, fft))
                // Keep FFTs which mismatch criteria are NOT found in the objective.
                .filter(fft -> mismatchFft(criteria, fft))
                .collect(Collectors.toList());

        switch (candidates.size()) {
            case 1:
                return candidates.get(0).originalType;
            case 0:
                return UNKNOWN;
            default:
                log.warn("Multiple FFT candidates found: {} [{}]", candidates, fwd);
                return UNKNOWN;
        }
    }

    private static boolean matchFft(Collection<Criterion> criteria, ForwardingFunctionType fft) {
        return matchOrMismatchFft(criteria, fft.matchCriteria, false);
    }

    private static boolean mismatchFft(Collection<Criterion> criteria, ForwardingFunctionType fft) {
        return matchOrMismatchFft(criteria, fft.mismatchCriteria, true);
    }

    private static boolean matchOrMismatchFft(
            Collection<Criterion> criteria,
            Map<Criterion.Type, List<Criterion>> criteriaToMatch,
            boolean mismatch) {
        final Map<Criterion.Type, Criterion> givenCriteria = typeToCriterionMap(criteria);
        for (Criterion.Type typeToMatch : criteriaToMatch.keySet()) {
            if (!givenCriteria.containsKey(typeToMatch)) {
                return false;
            }
            final boolean matchFound = criteriaToMatch.get(typeToMatch).stream()
                    .anyMatch(c -> mismatch != givenCriteria.get(c.type()).equals(c));
            if (!matchFound) {
                return false;
            }
        }
        return true;
    }

    private static Set<Criterion> criteriaIncludingMeta(ForwardingObjective fwd) {
        final Set<Criterion> criteria = Sets.newHashSet();
        criteria.addAll(fwd.selector().criteria());
        // FIXME: Is this really needed? Meta is such an ambiguous field...
        if (fwd.meta() != null) {
            criteria.addAll(fwd.meta().criteria());
        }
        return criteria;
    }

    private static Map<Criterion.Type, List<Criterion>> typeToCriteriaMap(Collection<Criterion> criteria) {
        return criteria.stream().collect(Collectors.groupingBy(Criterion::type));
    }

    private static Map<Criterion.Type, Criterion> typeToCriterionMap(Collection<Criterion> criteria) {
        final ImmutableMap.Builder<Criterion.Type, Criterion> mapBuilder = ImmutableMap.builder();
        criteria.forEach(c -> mapBuilder.put(c.type(), c));
        return mapBuilder.build();
    }
}
