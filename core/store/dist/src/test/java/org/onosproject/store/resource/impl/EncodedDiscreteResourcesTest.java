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

import java.util.Set;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;

public class EncodedDiscreteResourcesTest {
    private static final DeviceId DID = DeviceId.deviceId("device1");
    private static final PortNumber PN = PortNumber.portNumber(1);
    private static final VlanId VID1 = VlanId.vlanId((short) 1);
    private static final VlanId VID2 = VlanId.vlanId((short) 2);
    private static final VlanId VID3 = VlanId.vlanId((short) 3);

    @Test
    public void testContains() {
        DiscreteResource res1 = Resources.discrete(DID, PN, VID1).resource();
        DiscreteResource res2 = Resources.discrete(DID, PN, VID2).resource();
        DiscreteResource res3 = Resources.discrete(DID, PN, VID3).resource();

        Set<DiscreteResource> resources = ImmutableSet.of(res1, res2);

        EncodedDiscreteResources sut = EncodedDiscreteResources.of(resources, new VlanIdCodec());

        assertThat(sut.contains(res1), is(true));
        assertThat(sut.contains(res3), is(false));
    }

    @Test
    public void testDifference() {
        DiscreteResource res1 = Resources.discrete(DID, PN, VID1).resource();
        DiscreteResource res2 = Resources.discrete(DID, PN, VID2).resource();
        DiscreteResource res3 = Resources.discrete(DID, PN, VID3).resource();

        EncodedDiscreteResources sut = EncodedDiscreteResources.of(ImmutableSet.of(res1, res2), new VlanIdCodec());
        EncodedDiscreteResources other = EncodedDiscreteResources.of(ImmutableSet.of(res1, res3), new VlanIdCodec());

        assertThat(sut.difference(other), is(EncodedDiscreteResources.of(ImmutableSet.of(res2), new VlanIdCodec())));
    }

    @Test
    public void testAdd() {
        DiscreteResource res1 = Resources.discrete(DID, PN, VID1).resource();
        DiscreteResource res2 = Resources.discrete(DID, PN, VID2).resource();
        DiscreteResource res3 = Resources.discrete(DID, PN, VID3).resource();

        EncodedDiscreteResources sut = EncodedDiscreteResources.of(ImmutableSet.of(res1, res2), new VlanIdCodec());
        EncodedDiscreteResources other = EncodedDiscreteResources.of(ImmutableSet.of(res1, res3), new VlanIdCodec());

        assertThat(sut.add(other),
                is(EncodedDiscreteResources.of(ImmutableSet.of(res1, res2, res3), new VlanIdCodec())));
    }

}
