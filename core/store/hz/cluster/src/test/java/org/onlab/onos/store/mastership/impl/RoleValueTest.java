package org.onlab.onos.store.mastership.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.onlab.onos.net.MastershipRole.*;

import org.junit.Test;
import org.onlab.onos.cluster.NodeId;

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
