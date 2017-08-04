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
import org.onosproject.net.PortNumber;
import org.onosproject.net.resource.DiscreteResource;
import org.onosproject.net.resource.Resources;
import org.onosproject.store.service.Serializer;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class EncodableDiscreteResourcesTest {
    private final Serializer serializer = ConsistentResourceStore.SERIALIZER;

    @Test
    public void testPortSerialize() {
        DiscreteResource device = Resources.discrete(DeviceId.deviceId("a")).resource();
        Set<DiscreteResource> resources = IntStream.range(0, 100)
                .mapToObj(PortNumber::portNumber)
                .map(device::child)
                .collect(Collectors.toSet());

        DiscreteResources original = EncodableDiscreteResources.of(resources);

        byte[] bytes = serializer.encode(original);
        DiscreteResources decoded = serializer.decode(bytes);
        assertThat(decoded, is(original));
    }

    @Test
    public void testIfResourceIsFound() {
        DiscreteResource res1 = Resources.discrete(DeviceId.deviceId("a"), PortNumber.portNumber(1)).resource();

        DiscreteResources sut = EncodableDiscreteResources.of(ImmutableSet.of(res1));

        assertThat(sut.lookup(res1.id()), is(Optional.of(res1)));
    }

    @Test
    public void testIfResourceIsNotFound() {
        DiscreteResource res1 = Resources.discrete(DeviceId.deviceId("a"), PortNumber.portNumber(1)).resource();
        DiscreteResource res2 = Resources.discrete(DeviceId.deviceId("a"), PortNumber.portNumber(2)).resource();

        DiscreteResources sut = EncodableDiscreteResources.of(ImmutableSet.of(res1));

        assertThat(sut.lookup(res2.id()), is(Optional.empty()));
    }

    @Test
    public void testIfDifferenceIsEmpty() {
        DiscreteResource res1 = Resources.discrete(DeviceId.deviceId("a"), PortNumber.portNumber(1)).resource();
        DiscreteResource res2 = Resources.discrete(DeviceId.deviceId("a"), PortNumber.portNumber(2)).resource();

        DiscreteResources sut = EncodableDiscreteResources.of(ImmutableSet.of(res1, res2));

        DiscreteResources other = EncodableDiscreteResources.of(ImmutableSet.of(res1, res2));

        DiscreteResources expected = DiscreteResources.empty();
        assertThat(sut.difference(other), is(expected));
    }

    @Test
    public void testSerializeInstanceContainingEmptyEncodedDiscreteResources() {
        DiscreteResource device = Resources.discrete(DeviceId.deviceId("a")).resource();
        List<PortNumber> ports = IntStream.range(0, 1)
                .mapToObj(PortNumber::portNumber)
                .collect(Collectors.toList());
        List<VlanId> vlans = IntStream.range(0, 2)
                .mapToObj(x -> VlanId.vlanId((short) x))
                .collect(Collectors.toList());

        Set<DiscreteResource> originalResources = Stream.concat(ports.stream(), vlans.stream())
                .map(device::child)
                .collect(Collectors.toSet());
        DiscreteResources sut = EncodableDiscreteResources.of(originalResources);

        Set<DiscreteResource> portOnlyResources = ports.stream().map(device::child).collect(Collectors.toSet());
        DiscreteResources other = EncodableDiscreteResources.of(portOnlyResources);

        DiscreteResources diff = sut.difference(other);

        byte[] bytes = serializer.encode(diff);
        assertThat(serializer.decode(bytes), is(diff));
    }

    @Test
    public void testIfDifferenceIsNotEmpty() {
        DiscreteResource res1 = Resources.discrete(DeviceId.deviceId("a"), PortNumber.portNumber(1)).resource();
        DiscreteResource res2 = Resources.discrete(DeviceId.deviceId("a"), PortNumber.portNumber(2)).resource();

        DiscreteResources sut = EncodableDiscreteResources.of(ImmutableSet.of(res1, res2));

        DiscreteResources other = EncodableDiscreteResources.of(ImmutableSet.of(res1));

        DiscreteResources expected = EncodableDiscreteResources.of(ImmutableSet.of(res2));
        assertThat(sut.difference(other), is(expected));
    }

    @Test
    public void testIfDifferenceIsNotChanged() {
        DiscreteResource res1 = Resources.discrete(DeviceId.deviceId("a"), PortNumber.portNumber(1)).resource();
        DiscreteResource res2 = Resources.discrete(DeviceId.deviceId("a"), PortNumber.portNumber(2)).resource();

        DiscreteResources sut = EncodableDiscreteResources.of(ImmutableSet.of(res1));

        DiscreteResources other = EncodableDiscreteResources.of(ImmutableSet.of(res2));

        DiscreteResources expected = EncodableDiscreteResources.of(ImmutableSet.of(res1));
        assertThat(sut.difference(other), is(expected));
    }

    @Test
    public void testDifferenceFromEmpty() {
        DiscreteResource res1 = Resources.discrete(DeviceId.deviceId("a"), PortNumber.portNumber(1)).resource();

        DiscreteResources sut = EncodableDiscreteResources.of(ImmutableSet.of(res1));

        DiscreteResources other = DiscreteResources.empty();

        DiscreteResources expected = EncodableDiscreteResources.of(ImmutableSet.of(res1));
        assertThat(sut.difference(other), is(expected));
    }

    @Test
    public void testEmpty() {
        DiscreteResources sut = EncodableDiscreteResources.of(ImmutableSet.of());

        assertThat(sut.isEmpty(), is(true));
    }

    @Test
    public void testNotEmpty() {
        DiscreteResource res1 = Resources.discrete(DeviceId.deviceId("a"), PortNumber.portNumber(1)).resource();

        DiscreteResources sut = EncodableDiscreteResources.of(ImmutableSet.of(res1));

        assertThat(sut.isEmpty(), is(false));
    }

    @Test
    public void testIfResourceIsContained() {
        DiscreteResource res1 = Resources.discrete(DeviceId.deviceId("a"), PortNumber.portNumber(1)).resource();

        DiscreteResources sut = EncodableDiscreteResources.of(ImmutableSet.of(res1));

        assertThat(sut.containsAny(ImmutableSet.of(res1)), is(true));
    }

    @Test
    public void testIfResourceIsNotContained() {
        DiscreteResource res1 = Resources.discrete(DeviceId.deviceId("a"), PortNumber.portNumber(1)).resource();
        DiscreteResource res2 = Resources.discrete(DeviceId.deviceId("a"), PortNumber.portNumber(2)).resource();

        DiscreteResources sut = EncodableDiscreteResources.of(ImmutableSet.of(res1));

        assertThat(sut.containsAny(ImmutableSet.of(res2)), is(false));
    }

    @Test
    public void testContainsWithEmpty() {
        DiscreteResource res1 = Resources.discrete(DeviceId.deviceId("a"), PortNumber.portNumber(1)).resource();

        DiscreteResources sut = EncodableDiscreteResources.of(ImmutableSet.of(res1));

        assertThat(sut.containsAny(ImmutableSet.of()), is(false));
    }

    @Test
    public void testAdd() {
        DiscreteResource res1 = Resources.discrete(DeviceId.deviceId("a"), PortNumber.portNumber(1)).resource();
        DiscreteResource res2 = Resources.discrete(DeviceId.deviceId("a"), PortNumber.portNumber(2)).resource();

        DiscreteResources sut = EncodableDiscreteResources.of(ImmutableSet.of(res1));

        DiscreteResources other = EncodableDiscreteResources.of(ImmutableSet.of(res2));

        DiscreteResources expected = EncodableDiscreteResources.of(ImmutableSet.of(res1, res2));
        assertThat(sut.add(other), is(expected));
    }

    @Test
    public void testValues() {
        DiscreteResource res1 = Resources.discrete(DeviceId.deviceId("a"), PortNumber.portNumber(1)).resource();
        DiscreteResource res2 = Resources.discrete(DeviceId.deviceId("a"), PortNumber.portNumber(2)).resource();

        DiscreteResources sut = EncodableDiscreteResources.of(ImmutableSet.of(res1, res2));

        assertThat(sut.values(), is(ImmutableSet.of(res1, res2)));
    }
}
