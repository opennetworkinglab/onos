/*
 * Copyright 2014 Open Networking Laboratory
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
package org.onosproject.store.mastership.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.onosproject.net.MastershipRole.*;

import org.junit.Test;
import org.onosproject.cluster.NodeId;

import com.google.common.collect.Sets;

public class RoleValueTest {

    private static final RoleValue RV = new RoleValue();

    private static final NodeId NID1 = new NodeId("node1");
    private static final NodeId NID2 = new NodeId("node2");
    private static final NodeId NID3 = new NodeId("node3");

    @Test
    public void add() {
        assertEquals("faulty initialization: ", 3, RV.value.size());
        RV.add(MASTER, NID1);
        RV.add(STANDBY, NID2);
        RV.add(STANDBY, NID3);

        assertEquals("wrong nodeID: ", NID1, RV.get(MASTER));
        assertTrue("wrong nodeIDs: ",
                Sets.newHashSet(NID3, NID2).containsAll(RV.nodesOfRole(STANDBY)));
    }
}
