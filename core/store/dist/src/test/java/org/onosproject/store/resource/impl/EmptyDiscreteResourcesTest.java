/*
 * Copyright 2016-present Open Networking Foundation
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

package org.onosproject.store.resource.impl;

import com.google.common.collect.ImmutableSet;
import org.junit.Test;
import org.onlab.packet.VlanId;
import org.onosproject.net.DeviceId;
import org.onosproject.net.resource.DiscreteResource;
import org.onosproject.net.resource.Resources;

import java.util.Optional;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;

public class EmptyDiscreteResourcesTest {
    private EmptyDiscreteResources sut = EmptyDiscreteResources.INSTANCE;

    @Test
    public void testLookup() {
        assertThat(sut.lookup(Resources.discrete(DeviceId.deviceId("a")).id()), is(Optional.empty()));
    }

    @Test
    public void testDifference() {
        DiscreteResource res1 = Resources.discrete(DeviceId.deviceId("a")).resource();
        DiscreteResource res2 = Resources.discrete(DeviceId.deviceId("b")).resource();

        assertThat(sut.difference(DiscreteResources.of(ImmutableSet.of(res1))),
                is(EmptyDiscreteResources.INSTANCE));
        assertThat(sut.difference(DiscreteResources.of(ImmutableSet.of(res2))),
                is(EmptyDiscreteResources.INSTANCE));
    }

    @Test
    public void testIsEmpty() {
        assertThat(sut.isEmpty(), is(true));
    }

    @Test
    public void testValuesOf() {
        // doesn't match any types of resources
        assertThat(sut.valuesOf(Object.class), is(ImmutableSet.of()));
        assertThat(sut.valuesOf(VlanId.class), is(ImmutableSet.of()));
    }

    @Test
    public void testContainsAny() {
        DiscreteResource res1 = Resources.discrete(DeviceId.deviceId("a")).resource();
        DiscreteResource res2 = Resources.discrete(DeviceId.deviceId("b")).resource();

        assertThat(sut.containsAny(ImmutableSet.of(res1)), is(false));
        assertThat(sut.containsAny(ImmutableSet.of(res2)), is(false));
    }

    @Test
    public void testAdd() {
        DiscreteResource res1 = Resources.discrete(DeviceId.deviceId("a")).resource();
        DiscreteResource res2 = Resources.discrete(DeviceId.deviceId("b")).resource();

        assertThat(sut.add(DiscreteResources.of(ImmutableSet.of(res1))),
                is(DiscreteResources.of(ImmutableSet.of(res1))));
        assertThat(sut.add(DiscreteResources.of(ImmutableSet.of(res2))),
                is(DiscreteResources.of(ImmutableSet.of(res2))));
    }

    @Test
    public void testValues() {
        assertThat(sut.values(), is(ImmutableSet.of()));
    }
}
