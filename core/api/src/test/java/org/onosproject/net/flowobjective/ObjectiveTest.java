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
package org.onosproject.net.flowobjective;

import org.junit.Test;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flow.criteria.Criteria;
import org.onosproject.net.flow.criteria.Criterion;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.onlab.junit.ImmutableClassChecker.assertThatClassIsImmutable;
import static org.onosproject.net.NetTestTools.APP_ID;
import static org.onosproject.net.flowobjective.FilteringObjective.Type.DENY;
import static org.onosproject.net.flowobjective.ForwardingObjective.Flag.SPECIFIC;
import static org.onosproject.net.flowobjective.NextObjective.Type.HASHED;

/**
 * Unit tests for forwarding objective class.
 */
public class ObjectiveTest {

    private final TrafficTreatment treatment =
            DefaultTrafficTreatment.emptyTreatment();
    private final TrafficSelector selector =
            DefaultTrafficSelector.emptySelector();
    private final Criterion criterion = Criteria.dummy();
    private final Criterion key = Criteria.dummy();

    /**
     * Mock objective context.
     */
    private static class MockObjectiveContext implements ObjectiveContext {
        @Override
        public void onSuccess(Objective objective) {
            // stub
        }

        @Override
        public void onError(Objective objective, ObjectiveError error) {
            // stub
        }
    }

    /**
     * Checks immutability of objective classes.
     */
    @Test
    public void testImmutability() {
        assertThatClassIsImmutable(DefaultFilteringObjective.class);
        assertThatClassIsImmutable(DefaultForwardingObjective.class);
        assertThatClassIsImmutable(DefaultNextObjective.class);
    }

    //  Forwarding Objectives

    /**
     * Makes a forwarding objective builder with a set of default values.
     *
     * @return forwarding objective builder
     */
    private ForwardingObjective.Builder baseForwardingBuilder() {
        return DefaultForwardingObjective.builder()
                .withSelector(selector)
                .withTreatment(treatment)
                .withFlag(SPECIFIC)
                .fromApp(APP_ID)
                .nextStep(33);
    }

    /**
     * Checks the default values of a forwarding objective object.
     *
     * @param objective forwarding objective to check
     */
    private void checkForwardingBase(ForwardingObjective objective) {
        assertThat(objective.selector(), is(selector));
        assertThat(objective.treatment(), is(treatment));
        assertThat(objective.flag(), is(SPECIFIC));
        assertThat(objective.appId(), is(APP_ID));
        assertThat(objective.nextId(), is(33));
    }

    /**
     * Tests that forwarding objective objects are built correctly using the
     * add() method.
     */
    @Test
    public void testForwardingAdd() {
        checkForwardingBase(baseForwardingBuilder().add());
    }

    /**
     * Tests that forwarding objective objects are built correctly using the
     * add(context) method.
     */
    @Test
    public void testForwardingAddWithContext() {
        ObjectiveContext context = new MockObjectiveContext();
        checkForwardingBase(baseForwardingBuilder().add(context));
    }

    /**
     * Tests that forwarding objective objects are built correctly using the
     * remove() method.
     */
    @Test
    public void testForwardingRemove() {
        checkForwardingBase(baseForwardingBuilder().remove());
    }

    /**
     * Tests that forwarding objective objects are built correctly using the
     * remove(context) method.
     */
    @Test
    public void testForwardingRemoveWithContext() {
        ObjectiveContext context = new MockObjectiveContext();
        checkForwardingBase(baseForwardingBuilder().remove(context));
    }

    // Filtering objectives

    /**
     * Makes a filtering objective builder with a set of default values.
     *
     * @return filtering objective builder
     */
    private FilteringObjective.Builder baseFilteringBuilder() {
        return DefaultFilteringObjective.builder()
                .withKey(key)
                .withPriority(5)
                .addCondition(criterion)
                .fromApp(APP_ID)
                .makeTemporary(2)
                .deny();
    }

    /**
     * Checks the default values of a filtering objective object.
     *
     * @param objective filtering objective to check
     */
    private void checkFilteringBase(FilteringObjective objective) {
        assertThat(objective.key(), is(key));
        assertThat(objective.conditions(), hasItem(criterion));
        assertThat(objective.permanent(), is(false));
        assertThat(objective.timeout(), is(2));
        assertThat(objective.priority(), is(5));
        assertThat(objective.appId(), is(APP_ID));
        assertThat(objective.type(), is(DENY));
    }

    /**
     * Tests that forwarding objective objects are built correctly using the
     * add() method.
     */
    @Test
    public void testFilteringAdd() {
        checkFilteringBase(baseFilteringBuilder().add());
    }

    /**
     * Tests that forwarding objective objects are built correctly using the
     * add(context) method.
     */
    @Test
    public void testFilteringAddWithContext() {
        ObjectiveContext context = new MockObjectiveContext();
        checkFilteringBase(baseFilteringBuilder().add(context));
    }

    /**
     * Tests that forwarding objective objects are built correctly using the
     * remove() method.
     */
    @Test
    public void testFilteringRemove() {
        checkFilteringBase(baseFilteringBuilder().remove());
    }

    /**
     * Tests that forwarding objective objects are built correctly using the
     * remove(context) method.
     */
    @Test
    public void testFilteringRemoveWithContext() {
        ObjectiveContext context = new MockObjectiveContext();
        checkFilteringBase(baseFilteringBuilder().remove(context));
    }

    // Next objectives

    /**
     * Makes a next objective builder with a set of default values.
     *
     * @return next objective builder
     */
    private NextObjective.Builder baseNextBuilder() {
        return DefaultNextObjective.builder()
                .addTreatment(treatment)
                .withId(12)
                .withType(HASHED)
                .fromApp(APP_ID);
    }

    /**
     * Checks the default values of a next objective object.
     *
     * @param objective next objective to check
     */
    private void checkNextBase(NextObjective objective) {
        assertThat(objective.id(), is(12));
        assertThat(objective.appId(), is(APP_ID));
        assertThat(objective.type(), is(HASHED));
        assertThat(objective.next(), hasItem(treatment));
    }

    /**
     * Tests that forwarding objective objects are built correctly using the
     * add() method.
     */
    @Test
    public void testNextAdd() {
        checkNextBase(baseNextBuilder().add());
    }

    /**
     * Tests that forwarding objective objects are built correctly using the
     * add(context) method.
     */
    @Test
    public void testNextAddWithContext() {
        ObjectiveContext context = new MockObjectiveContext();
        checkNextBase(baseNextBuilder().add(context));
    }

    /**
     * Tests that forwarding objective objects are built correctly using the
     * remove() method.
     */
    @Test
    public void testNextRemove() {
        checkNextBase(baseNextBuilder().remove());
    }

    /**
     * Tests that forwarding objective objects are built correctly using the
     * remove(context) method.
     */
    @Test
    public void testNextRemoveWithContext() {
        ObjectiveContext context = new MockObjectiveContext();
        checkNextBase(baseNextBuilder().remove(context));
    }
}
