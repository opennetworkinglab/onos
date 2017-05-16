/*
 * Copyright 2014-present Open Networking Laboratory
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

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;

import static org.easymock.EasyMock.createMock;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.onosproject.net.AnnotationKeys.LATENCY;
import static org.onosproject.net.DefaultLinkTest.cp;
import static org.onosproject.net.DeviceId.deviceId;
import static org.onosproject.net.Link.Type.DIRECT;

public class LatencyConstraintTest {

    private static final DeviceId DID1 = deviceId("of:1");
    private static final DeviceId DID2 = deviceId("of:2");
    private static final DeviceId DID3 = deviceId("of:3");
    private static final PortNumber PN1 = PortNumber.portNumber(1);
    private static final PortNumber PN2 = PortNumber.portNumber(2);
    private static final PortNumber PN3 = PortNumber.portNumber(3);
    private static final PortNumber PN4 = PortNumber.portNumber(4);
    private static final ProviderId PROVIDER_ID = new ProviderId("of", "foo");
    private static final String LATENCY1 = "3.0";
    private static final String LATENCY2 = "4.0";

    private LatencyConstraint sut;
    private ResourceContext resourceContext;

    private Path path;
    private Link link1;
    private Link link2;

    @Before
    public void setUp() {
        resourceContext = createMock(ResourceContext.class);

        Annotations annotations1 = DefaultAnnotations.builder().set(LATENCY, LATENCY1).build();
        Annotations annotations2 = DefaultAnnotations.builder().set(LATENCY, LATENCY2).build();

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
        path = new DefaultPath(PROVIDER_ID, Arrays.asList(link1, link2), 10);
    }

    /**
     * Tests the path latency is less than the supplied constraint.
     */
    @Test
    public void testLessThanLatency() {
        sut = new LatencyConstraint(Duration.of(10, ChronoUnit.NANOS));

        assertThat(sut.validate(path, resourceContext), is(true));
    }

    /**
     * Tests the path latency is more than the supplied constraint.
     */
    @Test
    public void testMoreThanLatency() {
        sut = new LatencyConstraint(Duration.of(3, ChronoUnit.NANOS));

        assertThat(sut.validate(path, resourceContext), is(false));
    }

    /**
     * Tests the link latency is equal to "latency" annotated value.
     */
    @Test
    public void testCost() {
        sut = new LatencyConstraint(Duration.of(10, ChronoUnit.NANOS));

        assertThat(sut.cost(link1, resourceContext), is(closeTo(Double.parseDouble(LATENCY1), 1.0e-6)));
        assertThat(sut.cost(link2, resourceContext), is(closeTo(Double.parseDouble(LATENCY2), 1.0e-6)));
    }

    /**
     * Tests equality of the instances.
     */
    @Test
    public void testEquality() {
        LatencyConstraint c1 = new LatencyConstraint(Duration.of(1, ChronoUnit.SECONDS));
        LatencyConstraint c2 = new LatencyConstraint(Duration.of(1000, ChronoUnit.MILLIS));

        LatencyConstraint c3 = new LatencyConstraint(Duration.of(2, ChronoUnit.SECONDS));
        LatencyConstraint c4 = new LatencyConstraint(Duration.of(2000, ChronoUnit.MILLIS));

        new EqualsTester()
                .addEqualityGroup(c1, c2)
                .addEqualityGroup(c3, c4)
                .testEquals();
    }
}
