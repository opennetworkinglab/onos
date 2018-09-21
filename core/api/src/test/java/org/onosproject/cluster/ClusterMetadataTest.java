/*
 * Copyright 2017-present Open Networking Foundation
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
package org.onosproject.cluster;

import com.google.common.collect.ImmutableSet;
import com.google.common.testing.EqualsTester;
import org.junit.Test;
import org.onlab.packet.IpAddress;
import org.onosproject.net.provider.ProviderId;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

/**
 * Unit tests for ClusterMetadata.
 */
public class ClusterMetadataTest {
    private final NodeId nid1 = NodeId.nodeId("10.0.0.1");
    private final NodeId nid2 = NodeId.nodeId("10.0.0.2");

    private final ControllerNode n1 =
            new DefaultControllerNode(nid1, IpAddress.valueOf("10.0.0.1"), 9876);
    private final ControllerNode n2 =
            new DefaultControllerNode(nid2, IpAddress.valueOf("10.0.0.2"), 9876);

    private final ClusterMetadata metadata1 =
            new ClusterMetadata("foo", n1, ImmutableSet.of(), ImmutableSet.of(n1), "");
    private final ClusterMetadata sameAsMetadata1 =
            new ClusterMetadata("foo", n1, ImmutableSet.of(), ImmutableSet.of(n1), "");
    private final ClusterMetadata metadata2 =
            new ClusterMetadata("bar", n1, ImmutableSet.of(n1), ImmutableSet.of(n1, n2), "");
    private final ProviderId defaultProvider =
            new ProviderId("none", "none");
    /**
     * Tests for proper operation of equals(), hashCode() and toString() methods.
     */
    @Test
    public void checkEquals() {
        new EqualsTester()
                .addEqualityGroup(metadata1, sameAsMetadata1)
                .addEqualityGroup(metadata2)
                .testEquals();
    }

    /**
     * Tests that objects are created properly and accessor methods return
     * the correct values.
     */
    @Test
    public void checkConstruction() {
        assertThat(metadata2.getName(), is("bar"));
        assertThat(metadata2.getControllerNodes(), hasSize(1));
        assertThat(metadata2.getControllerNodes(), contains(n1));
        assertThat(metadata2.getStorageNodes(), hasSize(2));
        assertThat(metadata2.getStorageNodes(), contains(n1, n2));
        assertThat(metadata1.providerId(), is(defaultProvider));
    }
}
