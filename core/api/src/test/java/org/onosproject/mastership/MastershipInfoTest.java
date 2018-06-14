/*
 * Copyright 2018-present Open Networking Foundation
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
package org.onosproject.mastership;

import java.util.Optional;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import org.junit.Test;
import org.onosproject.cluster.NodeId;
import org.onosproject.net.MastershipRole;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

/**
 * Mastership info test.
 */
public class MastershipInfoTest {
    private final NodeId node1 = new NodeId("1");
    private final NodeId node2 = new NodeId("2");
    private final NodeId node3 = new NodeId("3");
    private final NodeId node4 = new NodeId("4");

    private final MastershipInfo mastershipInfo = new MastershipInfo(
        1,
        Optional.of(node1),
        ImmutableMap.<NodeId, MastershipRole>builder()
            .put(node1, MastershipRole.MASTER)
            .put(node2, MastershipRole.STANDBY)
            .put(node3, MastershipRole.STANDBY)
            .put(node4, MastershipRole.NONE)
            .build());

    @Test
    public void testMastershipInfo() throws Exception {
        assertEquals(1, mastershipInfo.term());
        assertEquals(node1, mastershipInfo.master().get());
        assertEquals(Lists.newArrayList(node1), mastershipInfo.getRoles(MastershipRole.MASTER));
        assertEquals(Lists.newArrayList(node2, node3), mastershipInfo.backups());
        assertEquals(Lists.newArrayList(node2, node3), mastershipInfo.getRoles(MastershipRole.STANDBY));
        assertEquals(Lists.newArrayList(node4), mastershipInfo.getRoles(MastershipRole.NONE));
    }

    @Test
    public void testEquals() throws Exception {
        assertEquals(mastershipInfo, mastershipInfo);
        assertNotEquals(mastershipInfo, new MastershipInfo(1, Optional.of(node1), ImmutableMap.of()));
    }
}
