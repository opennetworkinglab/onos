/*
 * Copyright 2014-present Open Networking Foundation
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

import static com.google.common.base.Predicates.notNull;
import static org.junit.Assert.*;
import static org.onosproject.cluster.ControllerNodeToNodeId.toNodeId;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.onlab.packet.IpAddress;

import com.google.common.collect.FluentIterable;


public class ControllerNodeToNodeIdTest {

    private static final NodeId NID1 = new NodeId("foo");
    private static final NodeId NID2 = new NodeId("bar");
    private static final NodeId NID3 = new NodeId("buz");

    private static final IpAddress IP1 = IpAddress.valueOf("127.0.0.1");
    private static final IpAddress IP2 = IpAddress.valueOf("127.0.0.2");
    private static final IpAddress IP3 = IpAddress.valueOf("127.0.0.3");

    private static final ControllerNode CN1 = new DefaultControllerNode(NID1, IP1);
    private static final ControllerNode CN2 = new DefaultControllerNode(NID2, IP2);
    private static final ControllerNode CN3 = new DefaultControllerNode(NID3, IP3);


    @Test
    public final void testToNodeId() {

        final Iterable<ControllerNode> nodes = Arrays.asList(CN1, CN2, CN3, null);
        final List<NodeId> nodeIds = Arrays.asList(NID1, NID2, NID3);

        assertEquals(nodeIds,
                FluentIterable.from(nodes)
                    .transform(toNodeId())
                    .filter(notNull())
                    .toList());
    }

}
