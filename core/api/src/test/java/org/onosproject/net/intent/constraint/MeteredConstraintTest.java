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
import org.onosproject.net.Link;
import org.onosproject.net.Path;
import org.onosproject.net.PortNumber;
import org.onosproject.net.intent.ResourceContext;
import org.onosproject.net.provider.ProviderId;

import java.util.Arrays;

import static org.easymock.EasyMock.createMock;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.onosproject.net.AnnotationKeys.METERED;
import static org.onosproject.net.DefaultLinkTest.cp;
import static org.onosproject.net.DeviceId.deviceId;
import static org.onosproject.net.Link.Type.DIRECT;

public class MeteredConstraintTest {

    private static final DeviceId DID1 = deviceId("of:1");
    private static final DeviceId DID2 = deviceId("of:2");
    private static final DeviceId DID3 = deviceId("of:3");
    private static final PortNumber PN1 = PortNumber.portNumber(1);
    private static final PortNumber PN2 = PortNumber.portNumber(2);
    private static final PortNumber PN3 = PortNumber.portNumber(3);
    private static final PortNumber PN4 = PortNumber.portNumber(4);
    private static final PortNumber PN5 = PortNumber.portNumber(5);
    private static final PortNumber PN6 = PortNumber.portNumber(6);
    private static final ProviderId PROVIDER_ID = new ProviderId("of", "foo");
    private static final String METERED1 = String.valueOf(true);
    private static final String METERED2 = String.valueOf(false);

    private ResourceContext resourceContext;

    private Path meteredPath;
    private Path nonMeteredPath;
    private Link meteredLink;
    private Link nonMeteredLink;
    private Link unAnnotatedLink;

    @Before
    public void setUp() {
        resourceContext = createMock(ResourceContext.class);

        Annotations annotations1 = DefaultAnnotations.builder().set(METERED, METERED1).build();
        Annotations annotations2 = DefaultAnnotations.builder().set(METERED, METERED2).build();

        meteredLink = DefaultLink.builder()
                .providerId(PROVIDER_ID)
                .src(cp(DID1, PN1))
                .dst(cp(DID2, PN2))
                .type(DIRECT)
                .annotations(annotations1)
                .build();
        nonMeteredLink = DefaultLink.builder()
                .providerId(PROVIDER_ID)
                .src(cp(DID2, PN3))
                .dst(cp(DID3, PN4))
                .type(DIRECT)
                .annotations(annotations2)
                .build();
        unAnnotatedLink = DefaultLink.builder()
                .providerId(PROVIDER_ID)
                .src(cp(DID1, PN5))
                .dst(cp(DID3, PN6))
                .type(DIRECT)
                .build();
        meteredPath = new DefaultPath(PROVIDER_ID, Arrays.asList(meteredLink, nonMeteredLink),
                ScalarWeight.toWeight(10));
        nonMeteredPath = new DefaultPath(PROVIDER_ID, Arrays.asList(nonMeteredLink, unAnnotatedLink),
                ScalarWeight.toWeight(10));
    }

    /**
     * Tests the path is valid with a metered link and the supplied constraint.
     */
    @Test
    public void testAllowedOnAllPaths() {
        MeteredConstraint constraint = new MeteredConstraint(true);

        assertThat(constraint.validate(meteredPath, resourceContext), is(true));
        assertThat(constraint.validate(nonMeteredPath, resourceContext), is(true));
    }

    /**
     * Tests the path is not valid with a metered link and the supplied constraint.
     */
    @Test
    public void tesNotAllowedOntMeteredPath() {
        MeteredConstraint constraint = new MeteredConstraint(false);

        assertThat(constraint.validate(meteredPath, resourceContext), is(false));
    }

    /**
     * Tests the path is not valid with a metered link and the supplied constraint.
     */
    @Test
    public void testNotAllowsOnNonMeteredPath() {
        MeteredConstraint constraint = new MeteredConstraint(false);

        assertThat(constraint.validate(nonMeteredPath, resourceContext), is(true));
    }

    /**
     * Tests that all links are valid for a constraint allowing metered links.
     */
    @Test
    public void testMeteredAllowed() {
        MeteredConstraint constraint = new MeteredConstraint(true);

        assertThat(constraint.isValid(meteredLink, resourceContext), is(true));
        assertThat(constraint.isValid(nonMeteredLink, resourceContext), is(true));
        assertThat(constraint.isValid(unAnnotatedLink, resourceContext), is(true));
    }

    /**
     * Tests that only non-metered links are valid for a constraint not allowing metered links.
     */
    @Test
    public void testMeteredNotAllowed() {
        MeteredConstraint constraint = new MeteredConstraint(false);

        assertThat(constraint.isValid(meteredLink, resourceContext), is(false));
        assertThat(constraint.isValid(nonMeteredLink, resourceContext), is(true));
        assertThat(constraint.isValid(unAnnotatedLink, resourceContext), is(true));
    }

    /**
     * Tests the link costs for a constraint allowing metered links.
     */
    @Test
    public void testCostAllowed() {
        MeteredConstraint constraint = new MeteredConstraint(true);

        assertThat(constraint.cost(meteredLink, resourceContext), is(1.0));
        assertThat(constraint.cost(nonMeteredLink, resourceContext), is(1.0));
        assertThat(constraint.cost(unAnnotatedLink, resourceContext), is(1.0));
    }

    /**
     * Tests the link costs for a constraint not allowing metered links.
     */
    @Test
    public void testCostNotAllowed() {
        MeteredConstraint constraint = new MeteredConstraint(false);

        assertThat(constraint.cost(meteredLink, resourceContext), is(-1.0));
        assertThat(constraint.cost(nonMeteredLink, resourceContext), is(1.0));
        assertThat(constraint.cost(unAnnotatedLink, resourceContext), is(1.0));
    }

    /**
     * Tests equality of the constraint instances.
     */
    @Test
    public void testEquality() {
        MeteredConstraint c1 = new MeteredConstraint(true);
        MeteredConstraint c2 = new MeteredConstraint(true);

        MeteredConstraint c3 = new MeteredConstraint(false);
        MeteredConstraint c4 = new MeteredConstraint(false);

        new EqualsTester()
                .addEqualityGroup(c1, c2)
                .addEqualityGroup(c3, c4)
                .testEquals();
    }
}
