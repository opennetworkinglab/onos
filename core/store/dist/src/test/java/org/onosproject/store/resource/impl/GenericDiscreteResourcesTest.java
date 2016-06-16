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

package org.onosproject.store.resource.impl;

import com.google.common.collect.ImmutableSet;
import org.junit.Test;
import org.onosproject.net.DeviceId;
import org.onosproject.net.resource.DiscreteResource;
import org.onosproject.net.resource.Resources;
import org.onosproject.store.service.Serializer;

import java.util.Optional;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class GenericDiscreteResourcesTest {
    private final Serializer serializer = ConsistentResourceStore.SERIALIZER;

    @Test
    public void testIfResourceIsFound() {
        DiscreteResource res1 = Resources.discrete(DeviceId.deviceId("a")).resource();

        DiscreteResources sut = GenericDiscreteResources.of(ImmutableSet.of(res1));

        assertThat(sut.lookup(res1.id()), is(Optional.of(res1)));
    }

    @Test
    public void testIfResourceIsNotFound() {
        DiscreteResource res1 = Resources.discrete(DeviceId.deviceId("a")).resource();
        DiscreteResource res2 = Resources.discrete(DeviceId.deviceId("b")).resource();

        DiscreteResources sut = GenericDiscreteResources.of(ImmutableSet.of(res1));

        assertThat(sut.lookup(res2.id()), is(Optional.empty()));
    }

    @Test
    public void testIfDifferenceIsEmpty() {
        DiscreteResource res1 = Resources.discrete(DeviceId.deviceId("a")).resource();
        DiscreteResource res2 = Resources.discrete(DeviceId.deviceId("b")).resource();

        DiscreteResources sut = GenericDiscreteResources.of(ImmutableSet.of(res1, res2));

        DiscreteResources other = GenericDiscreteResources.of(ImmutableSet.of(res1, res2));

        DiscreteResources expected = GenericDiscreteResources.of(ImmutableSet.of());
        assertThat(sut.difference(other), is(expected));
    }

    @Test
    public void testIfDifferenceIsNotEmpty() {
        DiscreteResource res1 = Resources.discrete(DeviceId.deviceId("a")).resource();
        DiscreteResource res2 = Resources.discrete(DeviceId.deviceId("b")).resource();

        DiscreteResources sut = GenericDiscreteResources.of(ImmutableSet.of(res1, res2));

        DiscreteResources other = GenericDiscreteResources.of(ImmutableSet.of(res1));

        DiscreteResources expected = GenericDiscreteResources.of(ImmutableSet.of(res2));
        assertThat(sut.difference(other), is(expected));
    }

    @Test
    public void testIfDifferenceIsNotChanged() {
        DiscreteResource res1 = Resources.discrete(DeviceId.deviceId("a")).resource();
        DiscreteResource res2 = Resources.discrete(DeviceId.deviceId("b")).resource();

        DiscreteResources sut = GenericDiscreteResources.of(ImmutableSet.of(res1));

        DiscreteResources other = GenericDiscreteResources.of(ImmutableSet.of(res2));

        DiscreteResources expected = GenericDiscreteResources.of(ImmutableSet.of(res1));
        assertThat(sut.difference(other), is(expected));
    }

    @Test
    public void testDifferenceFromEmpty() {
        DiscreteResource res1 = Resources.discrete(DeviceId.deviceId("a")).resource();

        DiscreteResources sut = GenericDiscreteResources.of(ImmutableSet.of(res1));

        DiscreteResources other = GenericDiscreteResources.of(ImmutableSet.of());

        DiscreteResources expected = GenericDiscreteResources.of(ImmutableSet.of(res1));
        assertThat(sut.difference(other), is(expected));
    }

    @Test
    public void testEmpty() {
        DiscreteResources sut = GenericDiscreteResources.of(ImmutableSet.of());

        assertThat(sut.isEmpty(), is(true));
    }

    @Test
    public void testNotEmpty() {
        DiscreteResource res1 = Resources.discrete(DeviceId.deviceId("a")).resource();

        DiscreteResources sut = GenericDiscreteResources.of(ImmutableSet.of(res1));

        assertThat(sut.isEmpty(), is(false));
    }

    @Test
    public void testIfResourceIsContained() {
        DiscreteResource res1 = Resources.discrete(DeviceId.deviceId("a")).resource();

        DiscreteResources sut = GenericDiscreteResources.of(ImmutableSet.of(res1));

        assertThat(sut.containsAny(ImmutableSet.of(res1)), is(true));
    }

    @Test
    public void testIfResourceIsNotContained() {
        DiscreteResource res1 = Resources.discrete(DeviceId.deviceId("a")).resource();
        DiscreteResource res2 = Resources.discrete(DeviceId.deviceId("b")).resource();

        DiscreteResources sut = GenericDiscreteResources.of(ImmutableSet.of(res1));

        assertThat(sut.containsAny(ImmutableSet.of(res2)), is(false));
    }

    @Test
    public void testContainsWithEmpty() {
        DiscreteResource res1 = Resources.discrete(DeviceId.deviceId("a")).resource();

        DiscreteResources sut = GenericDiscreteResources.of(ImmutableSet.of(res1));

        assertThat(sut.containsAny(ImmutableSet.of()), is(false));
    }

    @Test
    public void testAdd() {
        DiscreteResource res1 = Resources.discrete(DeviceId.deviceId("a")).resource();
        DiscreteResource res2 = Resources.discrete(DeviceId.deviceId("b")).resource();

        DiscreteResources sut = GenericDiscreteResources.of(ImmutableSet.of(res1));

        DiscreteResources other = GenericDiscreteResources.of(ImmutableSet.of(res2));

        DiscreteResources expected = GenericDiscreteResources.of(ImmutableSet.of(res1, res2));
        assertThat(sut.add(other), is(expected));
    }

    @Test
    public void testValues() {
        DiscreteResource res1 = Resources.discrete(DeviceId.deviceId("a")).resource();
        DiscreteResource res2 = Resources.discrete(DeviceId.deviceId("b")).resource();

        DiscreteResources sut = GenericDiscreteResources.of(ImmutableSet.of(res1, res2));

        assertThat(sut.values(), is(ImmutableSet.of(res1, res2)));
    }

    @Test
    public void testDifferenceSerializable() {
        DiscreteResource res1 = Resources.discrete(DeviceId.deviceId("a")).resource();
        DiscreteResource res2 = Resources.discrete(DeviceId.deviceId("b")).resource();

        DiscreteResources set1 = GenericDiscreteResources.of(ImmutableSet.of(res1, res2));
        DiscreteResources set2 = GenericDiscreteResources.of(ImmutableSet.of(res1));

        DiscreteResources difference = set1.difference(set2);
        assertThat(serializer.decode(serializer.encode(difference)), is(difference));
    }

    @Test
    public void testAddSerializable() {
        DiscreteResource res1 = Resources.discrete(DeviceId.deviceId("a")).resource();
        DiscreteResource res2 = Resources.discrete(DeviceId.deviceId("b")).resource();

        DiscreteResources set1 = GenericDiscreteResources.of(ImmutableSet.of(res1));
        DiscreteResources set2 = GenericDiscreteResources.of(ImmutableSet.of(res2));

        DiscreteResources add = set1.add(set2);
        assertThat(serializer.decode(serializer.encode(add)), is(add));
    }
}
