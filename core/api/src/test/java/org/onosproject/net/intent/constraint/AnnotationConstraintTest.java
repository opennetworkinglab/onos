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
import org.onosproject.net.DefaultAnnotations;
import org.onosproject.net.DefaultLink;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Link;
import org.onosproject.net.PortNumber;
import org.onosproject.net.intent.ResourceContext;
import org.onosproject.net.provider.ProviderId;

import static org.easymock.EasyMock.createMock;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.onosproject.net.DefaultLinkTest.cp;
import static org.onosproject.net.DeviceId.deviceId;
import static org.onosproject.net.Link.Type.DIRECT;
import static org.onosproject.net.PortNumber.portNumber;

/**
 * Test for link annotated value threshold.
 */
public class AnnotationConstraintTest {

    private static final ProviderId PID = new ProviderId("of", "foo");
    private static final DeviceId DID1 = deviceId("of:1");
    private static final DeviceId DID2 = deviceId("of:2");
    private static final PortNumber PID1 = portNumber(1);
    private static final PortNumber PID2 = portNumber(2);
    private static final String KEY = "distance";
    private static final String MISSING_KEY = "loss";
    private static final double VALUE = 100;

    private AnnotationConstraint sut;
    private Link link;
    private ResourceContext resourceContext;

    @Before
    public void setUp() {
        resourceContext = createMock(ResourceContext.class);

        DefaultAnnotations annotations = DefaultAnnotations.builder().set(KEY, String.valueOf(VALUE)).build();

        link = DefaultLink.builder()
                .providerId(PID)
                .src(cp(DID1, PID1))
                .dst(cp(DID2, PID2))
                .type(DIRECT)
                .annotations(annotations).build();
    }

    /**
     * Tests the specified annotated value is less than the threshold.
     */
    @Test
    public void testLessThanThreshold() {
        double value = 120;
        sut = new AnnotationConstraint(KEY, value);

        assertThat(sut.isValid(link, resourceContext), is(true));
        assertThat(sut.cost(link, resourceContext), is(closeTo(VALUE, 1.0e-6)));
    }

    /**
     * Tests the specified annotated value is more than the threshold.
     */
    @Test
    public void testMoreThanThreshold() {
        double value = 80;
        sut = new AnnotationConstraint(KEY, value);

        assertThat(sut.isValid(link, resourceContext), is(false));
        assertThat(sut.cost(link, resourceContext), is(lessThan(0.0)));
    }

    @Test
    public void testEquality() {
        new EqualsTester()
                .addEqualityGroup(new AnnotationConstraint(KEY, 100), new AnnotationConstraint(KEY, 100))
                .addEqualityGroup(new AnnotationConstraint(KEY, 120))
                .addEqualityGroup(new AnnotationConstraint("latency", 100))
                .testEquals();
    }

    /**
     * Tests the annotated constraint is invalid and has negative cost if the key is not annotated on the link.
     */
    @Test
    public void testNotAnnotated() {
        sut = new AnnotationConstraint(MISSING_KEY, 80);

        assertThat(link.annotations().value(MISSING_KEY), nullValue());
        assertThat(sut.isValid(link, resourceContext), is(false));
        assertThat(sut.cost(link, resourceContext), is(lessThan(0.0)));
    }
}
