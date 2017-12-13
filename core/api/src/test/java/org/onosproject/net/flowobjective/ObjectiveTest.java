/*
 * Copyright 2015-present Open Networking Foundation
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

import com.google.common.testing.EqualsTester;
import org.junit.Test;
import org.onosproject.TestApplicationId;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flow.criteria.Criteria;
import org.onosproject.net.flow.criteria.Criterion;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.not;
import static org.onlab.junit.ImmutableClassChecker.assertThatClassIsImmutable;
import static org.onosproject.net.NetTestTools.APP_ID;
import static org.onosproject.net.flowobjective.FilteringObjective.Type.DENY;
import static org.onosproject.net.flowobjective.ForwardingObjective.Flag.SPECIFIC;
import static org.onosproject.net.flowobjective.ForwardingObjective.Flag.VERSATILE;
import static org.onosproject.net.flowobjective.NextObjective.Type.HASHED;
import static org.onosproject.net.flowobjective.Objective.Operation.ADD;
import static org.onosproject.net.flowobjective.Objective.Operation.ADD_TO_EXISTING;
import static org.onosproject.net.flowobjective.Objective.Operation.REMOVE;
import static org.onosproject.net.flowobjective.Objective.Operation.REMOVE_FROM_EXISTING;

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
                .withMeta(selector)
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
        assertThat(objective.meta(), is(selector));
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
        ObjectiveContext context = new DefaultObjectiveContext(null, null);
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
        ObjectiveContext context = new DefaultObjectiveContext(null, null);
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
                .withMeta(treatment)
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
        assertThat(objective.meta(), is(treatment));
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
        ObjectiveContext context = new DefaultObjectiveContext(null, null);
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
        ObjectiveContext context = new DefaultObjectiveContext(null, null);
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
                .withMeta(selector)
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
        assertThat(objective.meta(), is(selector));
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
        ObjectiveContext context = new DefaultObjectiveContext(null, null);
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
        ObjectiveContext context = new DefaultObjectiveContext(null, null);
        checkNextBase(baseNextBuilder().remove(context), REMOVE, context);
    }

    /**
     * Tests equality, hash, and toString operations on next objectives.
     */
    @Test
    public void testEqualsNextObjective() {
        NextObjective next1 = baseNextBuilder().add();
        NextObjective sameAsNext1 = next1.copy().add();
        NextObjective next2 = baseNextBuilder().verify();
        NextObjective next3 = baseNextBuilder().fromApp(new TestApplicationId("foo2")).verify();
        NextObjective next4 = baseNextBuilder().withType(NextObjective.Type.FAILOVER).verify();
        NextObjective next5 = baseNextBuilder().addTreatment(DefaultTrafficTreatment.emptyTreatment()).verify();
        new EqualsTester()
                .addEqualityGroup(next1, sameAsNext1)
                .addEqualityGroup(next2)
                .addEqualityGroup(next3)
                .addEqualityGroup(next4)
                .addEqualityGroup(next5)
                .testEquals();
    }

    /**
     * Tests add to an existing next objective.
     */
    @Test
    public void testToFromExistingNextObjective() {
        NextObjective next1 = baseNextBuilder().addToExisting();

        checkNextBase(next1, ADD_TO_EXISTING, null);
    }

    /**
     * Tests remove from an existing next objective.
     */
    @Test
    public void testRemoveFromExistingNextObjective() {
        NextObjective next1 = baseNextBuilder().removeFromExisting();

        checkNextBase(next1, REMOVE_FROM_EXISTING, null);
    }

    /**
     * Tests equality, hash, and toString operations on filtering objectives.
     */
    @Test
    public void testEqualsFilteringObjective() {
        FilteringObjective filter1 = baseFilteringBuilder().add();
        FilteringObjective sameAsFilter1 = filter1.copy().add();
        FilteringObjective filter2 = baseFilteringBuilder().permit().add();
        FilteringObjective filter3 = baseFilteringBuilder().fromApp(new TestApplicationId("foo2")).add();
        FilteringObjective filter4 = baseFilteringBuilder().permit().remove();
        FilteringObjective filter5 = baseFilteringBuilder().withPriority(55).add();
        FilteringObjective filter6 = baseFilteringBuilder().makePermanent().add();
        new EqualsTester()
                .addEqualityGroup(filter1, sameAsFilter1)
                .addEqualityGroup(filter2)
                .addEqualityGroup(filter3)
                .addEqualityGroup(filter4)
                .addEqualityGroup(filter5)
                .addEqualityGroup(filter6)
                .testEquals();
    }

    /**
     * Tests add to an existing filtering objective.
     */
    @Test
    public void testToFromExistingFilteringObjective() {
        FilteringObjective filter1 = baseFilteringBuilder().add(null);

        checkFilteringBase(filter1, ADD, null);
    }

    /**
     * Tests remove from an existing filtering objective.
     */
    @Test
    public void testRemoveFromExistingFilteringObjective() {
        FilteringObjective filter1 = baseFilteringBuilder().remove(null);

        checkFilteringBase(filter1, REMOVE, null);
    }

    /**
     * Tests equality, hash, and toString operations on filtering objectives.
     */
    @Test
    public void testEqualsForwardingObjective() {
        ForwardingObjective forward1 = baseForwardingBuilder().add();
        ForwardingObjective sameAsForward1 = forward1.copy().add();
        ForwardingObjective forward2 = baseForwardingBuilder().withFlag(VERSATILE).add();
        ForwardingObjective forward3 = baseForwardingBuilder().fromApp(new TestApplicationId("foo2")).add();
        ForwardingObjective forward4 = baseForwardingBuilder().remove();
        ForwardingObjective forward5 = baseForwardingBuilder().withPriority(55).add();
        ForwardingObjective forward6 = baseForwardingBuilder().makePermanent().add();
        new EqualsTester()
                .addEqualityGroup(forward1, sameAsForward1)
                .addEqualityGroup(forward2)
                .addEqualityGroup(forward3)
                .addEqualityGroup(forward4)
                .addEqualityGroup(forward5)
                .addEqualityGroup(forward6)
                .testEquals();
    }

    /**
     * Tests add to an existing forwarding objective.
     */
    @Test
    public void testToFromExistingForwardingObjective() {
        ForwardingObjective forward1 = baseForwardingBuilder().add(null);

        checkForwardingBase(forward1, ADD, null);
    }

    /**
     * Tests remove from an existing forwarding objective.
     */
    @Test
    public void testRemoveFromExistingForwardingObjective() {
        ForwardingObjective forward1 = baseForwardingBuilder().remove(null);

        checkForwardingBase(forward1, REMOVE, null);
    }


    enum ContextType {
        BOTH,
        ERROR_ONLY,
        SUCCESS_ONLY
    }

    class ObjectiveContextAdapter implements ObjectiveContext {
        DefaultObjectiveContext context;
        int successCount = 0;
        int errorCount = 0;
        ObjectiveError objectiveError = ObjectiveError.UNKNOWN;
        Objective objectiveInError = null;

        ObjectiveContextAdapter(ContextType type) {
            switch (type) {

                case ERROR_ONLY:
                    context = new DefaultObjectiveContext(
                            (failedObjective, error) -> {
                                errorCount++;
                                objectiveInError = failedObjective;
                                objectiveError = error;
                            });
                    break;

                case SUCCESS_ONLY:
                    context = new DefaultObjectiveContext(
                            (successfulObjective) -> successCount++);
                    break;

                case BOTH:
                default:
                    context = new DefaultObjectiveContext(
                            (successfulObjective) -> successCount++,
                            (failedObjective, error) -> {
                                errorCount++;
                                objectiveInError = failedObjective;
                                objectiveError = error;
                            });
                    break;
            }
        }

        @Override
        public void onSuccess(Objective objective) {
            context.onSuccess(objective);
        }

        @Override
        public void onError(Objective objective, ObjectiveError error) {
            context.onError(objective, error);
        }

        int successCount() {
            return successCount;
        }

        int errorCount() {
            return errorCount;
        }

        ObjectiveError objectiveError() {
            return objectiveError;
        }

        Objective objectiveInError() {
            return objectiveInError;
        }
    }

    /**
     * Tests default objective context.
     */
    @Test
    public void testDefaultContext() {
        Objective objective = baseFilteringBuilder().add();
        ObjectiveContextAdapter context;

        context = new ObjectiveContextAdapter(ContextType.BOTH);
        context.onSuccess(objective);
        assertThat(context.successCount(), is(1));
        assertThat(context.errorCount(), is(0));
        assertThat(context.objectiveError(), is(ObjectiveError.UNKNOWN));
        assertThat(context.objectiveInError(), nullValue());

        context = new ObjectiveContextAdapter(ContextType.BOTH);
        context.onError(objective, ObjectiveError.UNSUPPORTED);
        assertThat(context.successCount(), is(0));
        assertThat(context.errorCount(), is(1));
        assertThat(context.objectiveError(), is(ObjectiveError.UNSUPPORTED));
        assertThat(context.objectiveInError(), equalTo(objective));

        context = new ObjectiveContextAdapter(ContextType.SUCCESS_ONLY);
        context.onSuccess(objective);
        assertThat(context.successCount(), is(1));
        assertThat(context.errorCount(), is(0));
        assertThat(context.objectiveError(), is(ObjectiveError.UNKNOWN));
        assertThat(context.objectiveInError(), nullValue());

        context = new ObjectiveContextAdapter(ContextType.ERROR_ONLY);
        context.onError(objective, ObjectiveError.UNSUPPORTED);
        assertThat(context.successCount(), is(0));
        assertThat(context.errorCount(), is(1));
        assertThat(context.objectiveError(), is(ObjectiveError.UNSUPPORTED));
        assertThat(context.objectiveInError(), equalTo(objective));
    }

}
