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
package org.onosproject.cluster;

import java.util.List;

import org.junit.Test;

import com.google.common.collect.Lists;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

/**
 * Test to check behavioral correctness of the RoleInfo structure.
 */
public class RoleInfoTest {
    private static final NodeId N1 = new NodeId("n1");
    private static final NodeId N2 = new NodeId("n2");
    private static final NodeId N3 = new NodeId("n3");
    private static final NodeId N4 = new NodeId("n4");

    private static final List<NodeId> BKUP1 = Lists.newArrayList(N2, N3);
    private static final List<NodeId> BKUP2 = Lists.newArrayList(N3, N4);

    private static final RoleInfo RI1 = new RoleInfo(N1, BKUP1);
    private static final RoleInfo RI2 = new RoleInfo(N1, BKUP2);
    private static final RoleInfo RI3 = new RoleInfo(N2, BKUP1);

    @Test
    public void basics() {
        assertEquals("wrong master", new NodeId("n1"), RI1.master());
        assertEquals("wrong Backups", RI1.backups(), Lists.newArrayList(N2, N3));

        assertNotEquals("equals() broken", RI1, RI2);
        assertNotEquals("equals() broken", RI1, RI3);

        List<NodeId> bkup3 = Lists.newArrayList(N3, new NodeId("n4"));
        assertEquals("equals() broken", new RoleInfo(N1, bkup3), RI2);
    }
}
