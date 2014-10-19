package org.onlab.onos.net.topology;

import com.google.common.collect.ImmutableSet;
import org.junit.Test;
import org.onlab.onos.net.DefaultDevice;
import org.onlab.onos.net.Device;
import org.onlab.onos.net.DeviceId;

import static org.junit.Assert.assertEquals;
import static org.onlab.onos.net.Device.Type.SWITCH;
import static org.onlab.onos.net.DeviceId.deviceId;
import static org.onlab.onos.net.topology.DefaultTopologyEdgeTest.*;

public class DefaultGraphDescriptionTest {

    static final DefaultTopologyEdge E1 = new DefaultTopologyEdge(V1, V2, L1);
    static final DefaultTopologyEdge E2 = new DefaultTopologyEdge(V1, V2, L1);

    private static final DeviceId D3 = deviceId("3");

    static final Device DEV1 = new DefaultDevice(PID, D1, SWITCH, "", "", "", "", null);
    static final Device DEV2 = new DefaultDevice(PID, D2, SWITCH, "", "", "", "", null);
    static final Device DEV3 = new DefaultDevice(PID, D3, SWITCH, "", "", "", "", null);

    @Test
    public void basics() {
        DefaultGraphDescription desc =
                new DefaultGraphDescription(4321L, ImmutableSet.of(DEV1, DEV2, DEV3),
                                            ImmutableSet.of(L1, L2));
        assertEquals("incorrect time", 4321L, desc.timestamp());
        assertEquals("incorrect vertex count", 3, desc.vertexes().size());
        assertEquals("incorrect edge count", 2, desc.edges().size());
    }

    @Test(expected = IllegalArgumentException.class)
    public void missingVertex() {
        new DefaultGraphDescription(4321L, ImmutableSet.of(DEV1, DEV3),
                                    ImmutableSet.of(L1, L2));
    }
}
