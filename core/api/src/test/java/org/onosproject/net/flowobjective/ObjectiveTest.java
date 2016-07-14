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
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.not;
import static org.onlab.junit.ImmutableClassChecker.assertThatClassIsImmutable;
import static org.onosproject.net.NetTestTools.APP_ID;
import static org.onosproject.net.flowobjective.FilteringObjective.Type.DENY;
import static org.onosproject.net.flowobjective.ForwardingObjective.Flag.SPECIFIC;
import static org.onosproject.net.flowobjective.NextObjective.Type.HASHED;
import static org.onosproject.net.flowobjective.Objective.Operation.ADD;
import static org.onosproject.net.flowobjective.Objective.Operation.REMOVE;

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
                .withPriority(22)
                .makeTemporary(5)
                .nextStep(33);
    }

    /**
     * Checks the default values of a forwarding objective object.
     *
     * @param objective forwarding objective to check
     */
    private void checkForwardingBase(ForwardingObjective objective,
                                     Objective.Operation op,
                                     ObjectiveContext expectedContext) {
        assertThat(objective.permanent(), is(false));
        assertThat(objective.timeout(), is(5));
        assertThat(objective.selector(), is(selector));
        assertThat(objective.treatment(), is(treatment));
        assertThat(objective.flag(), is(SPECIFIC));
        assertThat(objective.appId(), is(APP_ID));
        assertThat(objective.nextId(), is(33));
        assertThat(objective.id(), is(not(0)));
        assertThat(objective.priority(), is(22));
        assertThat(objective.op(), is(op));
        if (objective.context().isPresent()) {
            assertThat(objective.context().get(), is(expectedContext));
        } else {
            assertThat(expectedContext, nullValue());
        }
    }

    /**
     * Tests that forwarding objective objects are built correctly using the
     * add() method.
     */
    @Test
    public void testForwardingAdd() {
        checkForwardingBase(baseForwardingBuilder().add(), ADD, null);
    }

    /**
     * Tests that forwarding objective objects are built correctly using the
     * add(context) method.
     */
    @Test
    public void testForwardingAddWithContext() {
        ObjectiveContext context = new MockObjectiveContext();
        checkForwardingBase(baseForwardingBuilder().add(context), ADD, context);
    }

    /**
     * Tests that forwarding objective objects are built correctly using the
     * remove() method.
     */
    @Test
    public void testForwardingRemove() {
        checkForwardingBase(baseForwardingBuilder().remove(), REMOVE, null);
    }

    /**
     * Tests that forwarding objective objects are built correctly using the
     * remove(context) method.
     */
    @Test
    public void testForwardingRemoveWithContext() {
        ObjectiveContext context = new MockObjectiveContext();
        checkForwardingBase(baseForwardingBuilder().remove(context), REMOVE, context);
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
    private void checkFilteringBase(FilteringObjective objective,
                                    Objective.Operation op,
                                    ObjectiveContext expectedContext) {
        assertThat(objective.key(), is(key));
        assertThat(objective.conditions(), hasItem(criterion));
        assertThat(objective.permanent(), is(false));
        assertThat(objective.timeout(), is(2));
        assertThat(objective.priority(), is(5));
        assertThat(objective.appId(), is(APP_ID));
        assertThat(objective.type(), is(DENY));
        assertThat(objective.id(), is(not(0)));
        assertThat(objective.op(), is(op));
        if (objective.context().isPresent()) {
            assertThat(objective.context().get(), is(expectedContext));
        } else {
            assertThat(expectedContext, nullValue());
        }
    }

    /**
     * Tests that forwarding objective objects are built correctly using the
     * add() method.
     */
    @Test
    public void testFilteringAdd() {
        checkFilteringBase(baseFilteringBuilder().add(), ADD, null);
    }

    /**
     * Tests that forwarding objective objects are built correctly using the
     * add(context) method.
     */
    @Test
    public void testFilteringAddWithContext() {
        ObjectiveContext context = new MockObjectiveContext();
        checkFilteringBase(baseFilteringBuilder().add(context), ADD, context);
    }

    /**
     * Tests that forwarding objective objects are built correctly using the
     * remove() method.
     */
    @Test
    public void testFilteringRemove() {
        checkFilteringBase(baseFilteringBuilder().remove(), REMOVE, null);
    }

    /**
     * Tests that forwarding objective objects are built correctly using the
     * remove(context) method.
     */
    @Test
    public void testFilteringRemoveWithContext() {
        ObjectiveContext context = new MockObjectiveContext();
        checkFilteringBase(baseFilteringBuilder().remove(context), REMOVE, context);
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
                .makeTemporary(777)
                .withPriority(33)
                .fromApp(APP_ID);
    }

    /**
     * Checks the default values of a next objective object.
     *
     * @param objective next objective to check
     */
    private void checkNextBase(NextObjective objective,
                               Objective.Operation op,
                               ObjectiveContext expectedContext) {
        assertThat(objective.id(), is(12));
        assertThat(objective.appId(), is(APP_ID));
        assertThat(objective.type(), is(HASHED));
        assertThat(objective.next(), hasItem(treatment));
        assertThat(objective.permanent(), is(false));
        assertThat(objective.timeout(), is(0));
        assertThat(objective.priority(), is(0));
        assertThat(objective.op(), is(op));
        if (objective.context().isPresent()) {
            assertThat(objective.context().get(), is(expectedContext));
        } else {
            assertThat(expectedContext, nullValue());
        }
    }

    /**
     * Tests that forwarding objective objects are built correctly using the
     * add() method.
     */
    @Test
    public void testNextAdd() {
        checkNextBase(baseNextBuilder().add(), ADD, null);
    }

    /**
     * Tests that forwarding objective objects are built correctly using the
     * add(context) method.
     */
    @Test
    public void testNextAddWithContext() {
        ObjectiveContext context = new MockObjectiveContext();
        checkNextBase(baseNextBuilder().add(context), ADD, context);
    }

    /**
     * Tests that forwarding objective objects are built correctly using the
     * remove() method.
     */
    @Test
    public void testNextRemove() {
        checkNextBase(baseNextBuilder().remove(), REMOVE, null);
    }

    /**
     * Tests that forwarding objective objects are built correctly using the
     * remove(context) method.
     */
    @Test
    public void testNextRemoveWithContext() {
        ObjectiveContext context = new MockObjectiveContext();
        checkNextBase(baseNextBuilder().remove(context), REMOVE, context);
    }
}
