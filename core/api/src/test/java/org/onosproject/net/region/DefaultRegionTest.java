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

package org.onosproject.net.region;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.testing.EqualsTester;
import org.junit.Test;
import org.onosproject.cluster.NodeId;
import org.onosproject.net.Annotations;
import org.onosproject.net.DefaultAnnotations;

import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.onosproject.cluster.NodeId.nodeId;
import static org.onosproject.net.region.Region.Type.METRO;

/**
 * Suite of tests of the default region implementation.
 */
public class DefaultRegionTest {

    private static final RegionId ID1 = RegionId.regionId("r1");
    private static final Annotations NO_ANNOTS = DefaultAnnotations.EMPTY;

    @Test
    public void basics() {
        ImmutableList<Set<NodeId>> masters =
                ImmutableList.of(
                        ImmutableSet.of(nodeId("n1"), nodeId("n2")),
                        ImmutableSet.of(nodeId("n3"), nodeId("n4"))
                );
        Region r = new DefaultRegion(ID1, "R1", METRO, NO_ANNOTS, masters);
        assertEquals("incorrect id", ID1, r.id());
        assertEquals("incorrect name", "R1", r.name());
        assertEquals("incorrect type", METRO, r.type());
        assertEquals("incorrect masters", masters, r.masters());
    }

    @Test
    public void equality() {
        Region a = new DefaultRegion(ID1, "R1", METRO, NO_ANNOTS, null);
        Region b = new DefaultRegion(ID1, "R1", METRO, NO_ANNOTS, null);
        Region c = new DefaultRegion(ID1, "R2", METRO, NO_ANNOTS, null);

        new EqualsTester().addEqualityGroup(a, b).addEqualityGroup(c).testEquals();
    }

}