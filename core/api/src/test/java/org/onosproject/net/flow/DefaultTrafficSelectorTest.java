/*
 * Copyright 2014 Open Networking Laboratory
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
package org.onosproject.net.flow;

import java.util.Set;

import org.hamcrest.Description;
import org.hamcrest.Factory;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Test;
import org.onosproject.net.PortNumber;
import org.onosproject.net.flow.criteria.Criteria;
import org.onosproject.net.flow.criteria.Criterion;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;

import com.google.common.testing.EqualsTester;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.onlab.junit.ImmutableClassChecker.assertThatClassIsImmutable;
import static org.onosproject.net.flow.criteria.Criterion.Type;

/**
 * Unit tests for default traffic selector class.
 */
public class DefaultTrafficSelectorTest {

    /**
     * Checks that the DefaultFlowRule class is immutable.
     */
    @Test
    public void testImmutability() {
        assertThatClassIsImmutable(DefaultTrafficSelector.class);
    }

    /**
     * Tests equals(), hashCode() and toString() methods.
     */
    @Test
    public void testEquals() {
        final short one = 1;
        final short two = 2;

        final TrafficSelector selector1 =
                DefaultTrafficSelector.builder().matchLambda(one).build();
        final TrafficSelector sameAsSelector1 =
                DefaultTrafficSelector.builder().matchLambda(one).build();
        final TrafficSelector selector2 =
                DefaultTrafficSelector.builder().matchLambda(two).build();

        new EqualsTester()
                .addEqualityGroup(selector1, sameAsSelector1)
                .addEqualityGroup(selector2)
                .testEquals();
    }

    /**
     * Hamcrest matcher to check that a selector contains a
     * Criterion with the specified type.
     */
    public static final class CriterionExistsMatcher
           extends TypeSafeMatcher<TrafficSelector> {
        private final Criterion.Type type;

        /**
         * Constructs a matcher for the given criterion type.
         *
         * @param typeValue criterion type to match
         */
        public CriterionExistsMatcher(Criterion.Type typeValue) {
            type = typeValue;
        }

        @Override
        public boolean matchesSafely(TrafficSelector selector) {
            final Set<Criterion> criteria = selector.criteria();

            return notNullValue().matches(criteria) &&
                   hasSize(1).matches(criteria) &&
                   notNullValue().matches(selector.getCriterion(type));
        }

        @Override
        public void describeTo(Description description) {
            description.appendText("a criterion with type \" ").
                    appendText(type.toString()).
                    appendText("\"");
        }
    }


    /**
     * Creates a criterion type matcher.  Returns a matcher
     * for a criterion with the given type.
     *
     * @param type type of Criterion to match
     * @return Matcher object
     */
    @Factory
    public static Matcher<TrafficSelector> hasCriterionWithType(Criterion.Type type) {
        return new CriterionExistsMatcher(type);
    }


    /**
     * Tests the builder functions that add specific criteria.
     */
    @Test
    public void testCriteriaCreation() {
        TrafficSelector selector;

        final short shortValue = 33;
        final byte byteValue = 44;
        final IpPrefix ipPrefixValue = IpPrefix.valueOf("192.168.1.0/24");
        final IpPrefix ipv6PrefixValue = IpPrefix.valueOf("fe80::1/64");

        selector = DefaultTrafficSelector.builder()
                .matchInport(PortNumber.portNumber(11)).build();
        assertThat(selector, hasCriterionWithType(Type.IN_PORT));

        selector = DefaultTrafficSelector.builder()
                .matchEthSrc(MacAddress.BROADCAST).build();
        assertThat(selector, hasCriterionWithType(Type.ETH_SRC));

        selector = DefaultTrafficSelector.builder()
                .matchEthDst(MacAddress.BROADCAST).build();
        assertThat(selector, hasCriterionWithType(Type.ETH_DST));

        selector = DefaultTrafficSelector.builder()
                .matchEthType(shortValue).build();
        assertThat(selector, hasCriterionWithType(Type.ETH_TYPE));

        selector = DefaultTrafficSelector.builder()
                .matchVlanId(VlanId.vlanId(shortValue)).build();
        assertThat(selector, hasCriterionWithType(Type.VLAN_VID));

        selector = DefaultTrafficSelector.builder()
                .matchVlanPcp(byteValue).build();
        assertThat(selector, hasCriterionWithType(Type.VLAN_PCP));

        selector = DefaultTrafficSelector.builder()
                .matchIPProtocol(byteValue).build();
        assertThat(selector, hasCriterionWithType(Type.IP_PROTO));

        selector = DefaultTrafficSelector.builder()
                .matchIPSrc(ipPrefixValue).build();
        assertThat(selector, hasCriterionWithType(Type.IPV4_SRC));

        selector = DefaultTrafficSelector.builder()
                .matchIPDst(ipPrefixValue).build();
        assertThat(selector, hasCriterionWithType(Type.IPV4_DST));

        selector = DefaultTrafficSelector.builder()
                .matchTcpSrc(shortValue).build();
        assertThat(selector, hasCriterionWithType(Type.TCP_SRC));

        selector = DefaultTrafficSelector.builder()
                .matchTcpDst(shortValue).build();
        assertThat(selector, hasCriterionWithType(Type.TCP_DST));

        selector = DefaultTrafficSelector.builder()
                .matchIPv6Src(ipv6PrefixValue).build();
        assertThat(selector, hasCriterionWithType(Type.IPV6_SRC));

        selector = DefaultTrafficSelector.builder()
                .matchIPv6Dst(ipv6PrefixValue).build();
        assertThat(selector, hasCriterionWithType(Type.IPV6_DST));

        selector = DefaultTrafficSelector.builder()
                .matchMplsLabel(3).build();
        assertThat(selector, hasCriterionWithType(Type.MPLS_LABEL));

        selector = DefaultTrafficSelector.builder()
                .matchLambda(shortValue).build();
        assertThat(selector, hasCriterionWithType(Type.OCH_SIGID));

        selector = DefaultTrafficSelector.builder()
                .matchOpticalSignalType(shortValue).build();
        assertThat(selector, hasCriterionWithType(Type.OCH_SIGTYPE));

        final TrafficSelector baseSelector = DefaultTrafficSelector.builder()
                .matchOpticalSignalType(shortValue).build();
        selector = DefaultTrafficSelector.builder(baseSelector)
                .build();
        assertThat(selector, hasCriterionWithType(Type.OCH_SIGTYPE));

        final Criterion criterion = Criteria.matchLambda(shortValue);
        selector = DefaultTrafficSelector.builder()
                .add(criterion).build();
        assertThat(selector, hasCriterionWithType(Type.OCH_SIGID));
    }
}
