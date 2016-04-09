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
package org.onosproject.codec.impl;

import java.io.IOException;
import java.io.InputStream;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.onosproject.codec.JsonCodec;
import org.onosproject.core.CoreService;
import org.onosproject.net.Link;
import org.onosproject.net.intent.Constraint;
import org.onosproject.net.intent.constraint.AnnotationConstraint;
import org.onosproject.net.intent.constraint.AsymmetricPathConstraint;
import org.onosproject.net.intent.constraint.BandwidthConstraint;
import org.onosproject.net.intent.constraint.LatencyConstraint;
import org.onosproject.net.intent.constraint.LinkTypeConstraint;
import org.onosproject.net.intent.constraint.ObstacleConstraint;
import org.onosproject.net.intent.constraint.WaypointConstraint;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.onosproject.net.NetTestTools.APP_ID;
import static org.onosproject.net.NetTestTools.did;

/**
 * Unit tests for Constraint codec.
 */
public class ConstraintCodecTest {

    MockCodecContext context;
    JsonCodec<Constraint> constraintCodec;
    final CoreService mockCoreService = createMock(CoreService.class);

    /**
     * Sets up for each test.  Creates a context and fetches the flow rule
     * codec.
     */
    @Before
    public void setUp() {
        context = new MockCodecContext();
        constraintCodec = context.codec(Constraint.class);
        assertThat(constraintCodec, notNullValue());

        expect(mockCoreService.registerApplication(FlowRuleCodec.REST_APP_ID))
                .andReturn(APP_ID).anyTimes();
        replay(mockCoreService);
        context.registerService(CoreService.class, mockCoreService);
    }

    /**
     * Reads in a constraint from the given resource and decodes it.
     *
     * @param resourceName resource to use to read the JSON for the constraint
     * @return decoded constraint
     */
    private Constraint getConstraint(String resourceName) {
        InputStream jsonStream = ConstraintCodecTest.class
                .getResourceAsStream(resourceName);
        try {
            JsonNode json = context.mapper().readTree(jsonStream);
            assertThat(json, notNullValue());
            Constraint constraint = constraintCodec.decode((ObjectNode) json, context);
            assertThat(constraint, notNullValue());
            return checkNotNull(constraint);
        } catch (IOException ioe) {
            Assert.fail(ioe.getMessage());
            throw new IllegalStateException("cannot happen");
        }
    }


    /**
     * Tests link type constraint.
     */
    @Test
    public void linkTypeConstraint() {
        Constraint constraint = getConstraint("LinkTypeConstraint.json");
        assertThat(constraint, instanceOf(LinkTypeConstraint.class));

        LinkTypeConstraint linkTypeConstraint = (LinkTypeConstraint) constraint;
        assertThat(linkTypeConstraint.isInclusive(), is(false));
        assertThat(linkTypeConstraint.types(), hasSize(2));
        assertThat(linkTypeConstraint.types(), hasItem(Link.Type.OPTICAL));
        assertThat(linkTypeConstraint.types(), hasItem(Link.Type.DIRECT));
    }

    /**
     * Tests annotation constraint.
     */
    @Test
    public void annotationConstraint() {
        Constraint constraint = getConstraint("AnnotationConstraint.json");
        assertThat(constraint, instanceOf(AnnotationConstraint.class));

        AnnotationConstraint annotationConstraint = (AnnotationConstraint) constraint;
        assertThat(annotationConstraint.key(), is("key"));
        assertThat(annotationConstraint.threshold(), is(123.0D));
    }

    /**
     * Tests bandwidth constraint.
     */
    @Test
    public void bandwidthConstraint() {
        Constraint constraint = getConstraint("BandwidthConstraint.json");
        assertThat(constraint, instanceOf(BandwidthConstraint.class));

        BandwidthConstraint bandwidthConstraint = (BandwidthConstraint) constraint;
        assertThat(bandwidthConstraint.bandwidth().bps(), is(345.678D));
    }

    /**
     * Tests latency constraint.
     */
    @Test
    public void latencyConstraint() {
        Constraint constraint = getConstraint("LatencyConstraint.json");
        assertThat(constraint, instanceOf(LatencyConstraint.class));

        LatencyConstraint latencyConstraint = (LatencyConstraint) constraint;
        assertThat(latencyConstraint.latency().toMillis(), is(111L));
    }

    /**
     * Tests obstacle constraint.
     */
    @Test
    public void obstacleConstraint() {
        Constraint constraint = getConstraint("ObstacleConstraint.json");
        assertThat(constraint, instanceOf(ObstacleConstraint.class));

        ObstacleConstraint obstacleConstraint = (ObstacleConstraint) constraint;

        assertThat(obstacleConstraint.obstacles(), hasItem(did("dev1")));
        assertThat(obstacleConstraint.obstacles(), hasItem(did("dev2")));
        assertThat(obstacleConstraint.obstacles(), hasItem(did("dev3")));
    }

    /**
     * Tests waypoint constaint.
     */
    @Test
    public void waypointConstraint() {
        Constraint constraint = getConstraint("WaypointConstraint.json");
        assertThat(constraint, instanceOf(WaypointConstraint.class));

        WaypointConstraint waypointConstraint = (WaypointConstraint) constraint;

        assertThat(waypointConstraint.waypoints(), hasItem(did("devA")));
        assertThat(waypointConstraint.waypoints(), hasItem(did("devB")));
        assertThat(waypointConstraint.waypoints(), hasItem(did("devC")));
    }

    /**
     * Tests asymmetric path constraint.
     */
    @Test
    public void asymmetricPathConstraint() {
        Constraint constraint = getConstraint("AsymmetricPathConstraint.json");
        assertThat(constraint, instanceOf(AsymmetricPathConstraint.class));
    }
}
