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

import com.google.common.collect.ImmutableList;
import org.junit.Test;
import org.onlab.util.Bandwidth;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.intent.IntentId;
import org.onosproject.net.resource.ContinuousResource;
import org.onosproject.net.resource.ResourceAllocation;
import org.onosproject.net.resource.ResourceConsumer;
import org.onosproject.net.resource.Resources;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

/**
 * Unit tests for ContinuousResourceAllocation.
 */
public class ContinuousResourceAllocationTest {

    private static final DeviceId DID = DeviceId.deviceId("a");
    private static final PortNumber PN1 = PortNumber.portNumber(1);

    @Test
    public void testNoAllocationHasEnoughResource() {
        ContinuousResource original =
                Resources.continuous(DID, PN1, Bandwidth.class).resource(Bandwidth.gbps(1).bps());

        ContinuousResourceAllocation sut = ContinuousResourceAllocation.empty(original);

        ContinuousResource request =
                Resources.continuous(DID, PN1, Bandwidth.class).resource(Bandwidth.mbps(100).bps());

        assertThat(sut.hasEnoughResource(request), is(true));
    }

    @Test
    public void testHasEnoughResourceWhenSmallResourceIsRequested() {
        ContinuousResource original =
                Resources.continuous(DID, PN1, Bandwidth.class).resource(Bandwidth.gbps(1).bps());
        ContinuousResource allocated =
                Resources.continuous(DID, PN1, Bandwidth.class).resource(Bandwidth.mbps(500).bps());
        ResourceConsumer consumer = IntentId.valueOf(1);

        ContinuousResourceAllocation sut = new ContinuousResourceAllocation(original,
                        ImmutableList.of(new ResourceAllocation(allocated, consumer)));

        ContinuousResource request =
                Resources.continuous(DID, PN1, Bandwidth.class).resource(Bandwidth.mbps(200).bps());
        assertThat(sut.hasEnoughResource(request), is(true));
    }

    @Test
    public void testHasEnoughResourceWhenLargeResourceIsRequested() {
        ContinuousResource original =
                Resources.continuous(DID, PN1, Bandwidth.class).resource(Bandwidth.gbps(1).bps());
        ContinuousResource allocated =
                Resources.continuous(DID, PN1, Bandwidth.class).resource(Bandwidth.mbps(500).bps());
        ResourceConsumer consumer = IntentId.valueOf(1);

        ContinuousResourceAllocation sut = new ContinuousResourceAllocation(original,
                ImmutableList.of(new ResourceAllocation(allocated, consumer)));

        ContinuousResource request =
                Resources.continuous(DID, PN1, Bandwidth.class).resource(Bandwidth.mbps(600).bps());
        assertThat(sut.hasEnoughResource(request), is(false));
    }

    @Test
    public void testHasEnoughResourceWhenExactResourceIsRequested() {
        ContinuousResource original =
                Resources.continuous(DID, PN1, Bandwidth.class).resource(Bandwidth.gbps(1).bps());
        ContinuousResource allocated =
                Resources.continuous(DID, PN1, Bandwidth.class).resource(Bandwidth.mbps(500).bps());
        ResourceConsumer consumer = IntentId.valueOf(1);

        ContinuousResourceAllocation sut = new ContinuousResourceAllocation(original,
                ImmutableList.of(new ResourceAllocation(allocated, consumer)));

        ContinuousResource request =
                Resources.continuous(DID, PN1, Bandwidth.class).resource(Bandwidth.mbps(500).bps());
        assertThat(sut.hasEnoughResource(request), is(true));
    }

    @Test
    public void testReleaseWhenAllocatedResourceIsRequested() {
        ContinuousResource original =
                Resources.continuous(DID, PN1, Bandwidth.class).resource(Bandwidth.gbps(1).bps());
        ContinuousResource allocated =
                Resources.continuous(DID, PN1, Bandwidth.class).resource(Bandwidth.mbps(500).bps());
        ResourceConsumer consumer = IntentId.valueOf(1);

        ContinuousResourceAllocation sut = new ContinuousResourceAllocation(original,
                ImmutableList.of(new ResourceAllocation(allocated, consumer)));

        ContinuousResource request =
                Resources.continuous(DID, PN1, Bandwidth.class).resource(Bandwidth.mbps(500).bps());

        ContinuousResourceAllocation released = sut.release(request, consumer.consumerId());

        assertThat(released.allocations().isEmpty(), is(true));
    }

    @Test
    public void testReleaseWhenDifferentConsumerIsSpecified() {
        ContinuousResource original =
                Resources.continuous(DID, PN1, Bandwidth.class).resource(Bandwidth.gbps(1).bps());
        ContinuousResource allocated =
                Resources.continuous(DID, PN1, Bandwidth.class).resource(Bandwidth.mbps(500).bps());
        ResourceConsumer consumer = IntentId.valueOf(1);
        ResourceConsumer otherConsumer = IntentId.valueOf(2);

        ContinuousResourceAllocation sut = new ContinuousResourceAllocation(original,
                ImmutableList.of(new ResourceAllocation(allocated, consumer)));

        ContinuousResource request =
                Resources.continuous(DID, PN1, Bandwidth.class).resource(Bandwidth.mbps(500).bps());

        ImmutableList<ResourceAllocation> allocations = sut.release(request, otherConsumer.consumerId()).allocations();

        assertThat(allocations.size(), is(1));
        assertThat(allocations.get(0).resource().equals(allocated), is(true));
    }

    @Test
    public void testAllocateDifferentValue() {
        ContinuousResource original =
                Resources.continuous(DID, PN1, Bandwidth.class).resource(Bandwidth.gbps(1).bps());
        ContinuousResource allocated =
                Resources.continuous(DID, PN1, Bandwidth.class).resource(Bandwidth.mbps(500).bps());
        ResourceConsumer consumer = IntentId.valueOf(1);

        ContinuousResourceAllocation sut = new ContinuousResourceAllocation(original,
                ImmutableList.of(new ResourceAllocation(allocated, consumer)));

        ContinuousResource request =
                Resources.continuous(DID, PN1, Bandwidth.class).resource(Bandwidth.mbps(200).bps());

        ContinuousResourceAllocation newValue = sut.allocate(new ResourceAllocation(request, consumer));

        assertThat(newValue.allocations().size(), is(2));
        assertThat(newValue.allocations(), hasItem(new ResourceAllocation(allocated, consumer)));
        assertThat(newValue.allocations(), hasItem(new ResourceAllocation(request, consumer)));
    }

    @Test
    public void testAllocateSameValue() {
        ContinuousResource original =
                Resources.continuous(DID, PN1, Bandwidth.class).resource(Bandwidth.gbps(1).bps());
        ContinuousResource allocated =
                Resources.continuous(DID, PN1, Bandwidth.class).resource(Bandwidth.mbps(300).bps());
        ResourceConsumer consumer = IntentId.valueOf(1);

        ContinuousResourceAllocation sut = new ContinuousResourceAllocation(original,
                ImmutableList.of(new ResourceAllocation(allocated, consumer)));

        ContinuousResource request =
                Resources.continuous(DID, PN1, Bandwidth.class).resource(Bandwidth.mbps(300).bps());

        ContinuousResourceAllocation newValue = sut.allocate(new ResourceAllocation(request, consumer));

        assertThat(newValue.allocations().size(), is(2));
        assertThat(newValue.allocations()
                .stream()
                .allMatch(x -> x.equals(new ResourceAllocation(allocated, consumer))), is(true));
    }
}
