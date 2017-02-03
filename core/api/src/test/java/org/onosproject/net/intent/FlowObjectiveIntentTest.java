/*
 * Copyright 2016-present Open Networking Laboratory
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

package org.onosproject.net.intent;

import java.util.Collection;
import java.util.List;

import org.junit.Test;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.DefaultApplicationId;
import org.onosproject.net.DeviceId;
import org.onosproject.net.NetworkResource;
import org.onosproject.net.ResourceGroup;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.criteria.Criteria;
import org.onosproject.net.flowobjective.DefaultFilteringObjective;
import org.onosproject.net.flowobjective.DefaultForwardingObjective;
import org.onosproject.net.flowobjective.ForwardingObjective;
import org.onosproject.net.flowobjective.Objective;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.testing.EqualsTester;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.onlab.junit.ImmutableClassChecker.assertThatClassIsImmutable;

/**
 * Tests of the flow objective intent.
 */
public class FlowObjectiveIntentTest extends IntentTest {

    private static final ApplicationId APP_ID = new DefaultApplicationId(1, "foo");
    private static final Key KEY = Key.of("bar", APP_ID);

    private static final Objective FO1 = DefaultFilteringObjective.builder()
            .fromApp(APP_ID).addCondition(Criteria.matchEthType(123))
            .permit().add();
    private static final Objective FO2 = DefaultForwardingObjective.builder()
            .fromApp(APP_ID)
            .withSelector(DefaultTrafficSelector.builder().matchEthType((short) 123).build())
            .withTreatment(DefaultTrafficTreatment.emptyTreatment())
            .withFlag(ForwardingObjective.Flag.VERSATILE).add();
    private static final List<Objective> OBJECTIVES = ImmutableList.of(FO1, FO2);
    private static final Collection<NetworkResource> RESOURCES = ImmutableSet.of();
    private static final List<DeviceId> DEVICE = ImmutableList.of(DeviceId.NONE, DeviceId.NONE);
    private static final ResourceGroup RESOURCE_GROUP = ResourceGroup.of(0L);

    /**
     * Tests basics of construction and getters.
     */
    @Test
    public void basics() {
        FlowObjectiveIntent intent =
                new FlowObjectiveIntent(APP_ID, KEY, DEVICE, OBJECTIVES, RESOURCES, RESOURCE_GROUP);
        assertEquals("incorrect app id", APP_ID, intent.appId());
        assertEquals("incorrect key", KEY, intent.key());
        assertEquals("incorrect objectives", OBJECTIVES, intent.objectives());
        assertEquals("incorrect resources", RESOURCES, intent.resources());
        assertEquals("incorrect resource group", RESOURCE_GROUP, intent.resourceGroup());
        assertTrue("should be installable", intent.isInstallable());
    }

    /**
     * Tests equality.
     */
    @Test
    public void equality() {
        Intent a = createOne();
        Intent b = createAnother();
        new EqualsTester().addEqualityGroup(a).addEqualityGroup(b).testEquals();
    }

    /**
     * Tests that instance is immutable.
     */
    @Test
    public void testImmutability() {
        assertThatClassIsImmutable(HostToHostIntent.class);
    }

    @Override
    protected Intent createOne() {
        return new FlowObjectiveIntent(APP_ID, DEVICE, OBJECTIVES, RESOURCES);
    }

    @Override
    protected Intent createAnother() {
        return new FlowObjectiveIntent(APP_ID, DEVICE, OBJECTIVES, RESOURCES);
    }
}
