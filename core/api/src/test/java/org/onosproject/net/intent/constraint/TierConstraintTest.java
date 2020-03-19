/*
 * Copyright 2014-present Open Networking Foundation
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
package org.onosproject.net.intent.constraint;

import com.google.common.testing.EqualsTester;
import org.junit.Before;
import org.junit.Test;
import org.onlab.graph.ScalarWeight;
import org.onosproject.net.Annotations;
import org.onosproject.net.DefaultAnnotations;
import org.onosproject.net.DefaultLink;
import org.onosproject.net.DefaultPath;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Path;
import org.onosproject.net.PortNumber;
import org.onosproject.net.intent.Constraint;
import org.onosproject.net.intent.ResourceContext;
import org.onosproject.net.provider.ProviderId;

import java.util.Arrays;

import static org.easymock.EasyMock.createMock;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.onosproject.net.AnnotationKeys.TIER;
import static org.onosproject.net.DefaultLinkTest.cp;
import static org.onosproject.net.DeviceId.deviceId;
import static org.onosproject.net.Link.Type.DIRECT;

/**
 * Test for constraint of intermediate elements.
 */
public class TierConstraintTest {

    private static final DeviceId DID1 = deviceId("of:1");
    private static final DeviceId DID2 = deviceId("of:2");
    private static final DeviceId DID3 = deviceId("of:3");
    private static final DeviceId DID4 = deviceId("of:4");
    private static final PortNumber PN1 = PortNumber.portNumber(1);
    private static final PortNumber PN2 = PortNumber.portNumber(2);
    private static final PortNumber PN3 = PortNumber.portNumber(3);
    private static final PortNumber PN4 = PortNumber.portNumber(4);
    private static final PortNumber PN5 = PortNumber.portNumber(5);
    private static final PortNumber PN6 = PortNumber.portNumber(6);
    private static final ProviderId PROVIDER_ID = new ProviderId("of", "foo");
    private static final String TIER1 = "1";
    private static final String TIER2 = "2";
    private static final String TIER3 = "3";

    private ResourceContext resourceContext;

    private Path path12;
    private Path path13;
    private Path path23;
    private DefaultLink link1;
    private DefaultLink link2;
    private DefaultLink link3;

    @Before
    public void setUp() {
        resourceContext = createMock(ResourceContext.class);

        Annotations annotations1 = DefaultAnnotations.builder().set(TIER, TIER1).build();
        Annotations annotations2 = DefaultAnnotations.builder().set(TIER, TIER2).build();
        Annotations annotations3 = DefaultAnnotations.builder().set(TIER, TIER3).build();

        link1 = DefaultLink.builder()
                .providerId(PROVIDER_ID)
                .src(cp(DID1, PN1))
                .dst(cp(DID2, PN2))
                .type(DIRECT)
                .annotations(annotations1)
                .build();
        link2 = DefaultLink.builder()
                .providerId(PROVIDER_ID)
                .src(cp(DID2, PN3))
                .dst(cp(DID3, PN4))
                .type(DIRECT)
                .annotations(annotations2)
                .build();
        link3 = DefaultLink.builder()
                .providerId(PROVIDER_ID)
                .src(cp(DID2, PN5))
                .dst(cp(DID4, PN6))
                .type(DIRECT)
                .annotations(annotations3)
                .build();

        path12 = new DefaultPath(PROVIDER_ID, Arrays.asList(link1, link2), ScalarWeight.toWeight(10));
        path13 = new DefaultPath(PROVIDER_ID, Arrays.asList(link1, link3), ScalarWeight.toWeight(10));
        path23 = new DefaultPath(PROVIDER_ID, Arrays.asList(link2, link3), ScalarWeight.toWeight(10));
    }

    /**
     * Tests that all of the links in the specified path have a tier included in the specified included tiers.
     */
    @Test
    public void testSatisfyIncludedTiers() {
        TierConstraint constraint12 = new TierConstraint(true, 1, 2);
        assertThat(constraint12.validate(path12, resourceContext), is(true));

        TierConstraint constraint13 = new TierConstraint(true, 1, 3);
        assertThat(constraint13.validate(path13, resourceContext), is(true));

        TierConstraint constraint23 = new TierConstraint(true, 2, 3);
        assertThat(constraint23.validate(path23, resourceContext), is(true));
    }

    /**
     * Tests that at least one link in the specified path has a tier not included the specified included tiers.
     */
    @Test
    public void testNotSatisfyIncludedTiers() {
        TierConstraint constraint12 = new TierConstraint(true, 1, 2);
        assertThat(constraint12.validate(path13, resourceContext), is(false));
        assertThat(constraint12.validate(path23, resourceContext), is(false));

        TierConstraint constraint13 = new TierConstraint(true, 1, 3);
        assertThat(constraint13.validate(path12, resourceContext), is(false));
        assertThat(constraint13.validate(path23, resourceContext), is(false));

        TierConstraint constraint23 = new TierConstraint(true, 2, 3);
        assertThat(constraint23.validate(path12, resourceContext), is(false));
        assertThat(constraint23.validate(path13, resourceContext), is(false));
    }

    /**
     * Tests that all of the links in the specified path do not have a tier in the specified excluded tiers.
     */
    @Test
    public void testSatisfyExcludedTiers() {
        TierConstraint constraint12 = new TierConstraint(false, 1);
        assertThat(constraint12.validate(path23, resourceContext), is(true));

        TierConstraint constraint13 = new TierConstraint(false, 2);
        assertThat(constraint13.validate(path13, resourceContext), is(true));

        TierConstraint constraint23 = new TierConstraint(false, 3);
        assertThat(constraint23.validate(path12, resourceContext), is(true));
    }

    /**
     * Tests that at least one link in the specified path has a tier in the specified excluded tiers.
     */
    @Test
    public void testNotSatisfyExcludedTiers() {
        TierConstraint constraint12 = new TierConstraint(false, 1);
        assertThat(constraint12.validate(path12, resourceContext), is(false));
        assertThat(constraint12.validate(path13, resourceContext), is(false));

        TierConstraint constraint13 = new TierConstraint(false, 2);
        assertThat(constraint13.validate(path12, resourceContext), is(false));
        assertThat(constraint13.validate(path23, resourceContext), is(false));

        TierConstraint constraint23 = new TierConstraint(false, 3);
        assertThat(constraint23.validate(path13, resourceContext), is(false));
        assertThat(constraint23.validate(path23, resourceContext), is(false));
    }

    /**
     * Tests the link cost is equal to order in which a tier was added to the constraint.
     */
    @Test
    public void testOrderCost() {
        TierConstraint constraint32 = new TierConstraint(true, TierConstraint.CostType.ORDER, 3, 2);

        assertThat(constraint32.cost(link1, resourceContext), is(-1.0));
        assertThat(constraint32.cost(link2, resourceContext), is(2.0));
        assertThat(constraint32.cost(link3, resourceContext), is(1.0));

        TierConstraint constraint123 = new TierConstraint(true, TierConstraint.CostType.ORDER, 1, 2, 3);

        assertThat(constraint123.cost(link1, resourceContext), is(1.0));
        assertThat(constraint123.cost(link2, resourceContext), is(2.0));
        assertThat(constraint123.cost(link3, resourceContext), is(3.0));

        TierConstraint constraint231 = new TierConstraint(true, TierConstraint.CostType.ORDER, 2, 3, 1);

        assertThat(constraint231.cost(link1, resourceContext), is(3.0));
        assertThat(constraint231.cost(link2, resourceContext), is(1.0));
        assertThat(constraint231.cost(link3, resourceContext), is(2.0));

        TierConstraint constraint312 = new TierConstraint(true, TierConstraint.CostType.ORDER, 3, 1, 2);

        assertThat(constraint312.cost(link1, resourceContext), is(2.0));
        assertThat(constraint312.cost(link2, resourceContext), is(3.0));
        assertThat(constraint312.cost(link3, resourceContext), is(1.0));
    }

    /**
     * Tests the link cost is equal to order in which a tier was added to the constraint.
     */
    @Test
    public void testOrderCostWithDuplicates() {
        TierConstraint constraint32 = new TierConstraint(true, TierConstraint.CostType.ORDER, 3, 2, 1, 1, 2, 3);

        assertThat(constraint32.cost(link1, resourceContext), is(3.0));
        assertThat(constraint32.cost(link2, resourceContext), is(2.0));
        assertThat(constraint32.cost(link3, resourceContext), is(1.0));
    }

    /**
     * Tests the link cost is equal to tier value.
     */
    @Test
    public void testTierCost() {
        TierConstraint constraint123 = new TierConstraint(true, TierConstraint.CostType.TIER, 3, 1);

        assertThat(constraint123.cost(link1, resourceContext), is(1.0));
        assertThat(constraint123.cost(link2, resourceContext), is(-1.0));
        assertThat(constraint123.cost(link3, resourceContext), is(3.0));
    }

    /**
     * Tests the link cost is 1 if valid and -1 if invalid.
     */
    @Test
    public void testValidCost() {
        TierConstraint constraint = new TierConstraint(true, TierConstraint.CostType.VALID, 2, 1);

        assertThat(constraint.cost(link1, resourceContext), is(1.0));
        assertThat(constraint.cost(link2, resourceContext), is(1.0));
        assertThat(constraint.cost(link3, resourceContext), is(-1.0));
    }

    @Test
    public void testEquality() {
        Constraint c1 = new TierConstraint(true, TierConstraint.CostType.ORDER, 3, 2, 1);
        Constraint c2 = new TierConstraint(true, TierConstraint.CostType.ORDER, 3, 2, 1);

        Constraint c3 = new TierConstraint(false, TierConstraint.CostType.TIER, 1);
        Constraint c4 = new TierConstraint(false, TierConstraint.CostType.TIER, 1);

        new EqualsTester()
                .addEqualityGroup(c1, c2)
                .addEqualityGroup(c3, c4)
                .testEquals();
    }
}
