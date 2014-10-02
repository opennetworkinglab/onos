package org.onlab.onos.net.topology;

import com.google.common.testing.EqualsTester;
import org.junit.Test;
import org.onlab.onos.net.DeviceId;

import static org.junit.Assert.*;
import static org.onlab.onos.net.DeviceId.deviceId;

/**
 * Tests of the topology graph vertex.
 */
public class DefaultTopologyVertexTest {

    private static final DeviceId D1 = deviceId("1");
    private static final DeviceId D2 = deviceId("2");

    @Test
    public void basics() {
        DefaultTopologyVertex v = new DefaultTopologyVertex(D1);
        assertEquals("incorrect device id", D1, v.deviceId());

        new EqualsTester()
                .addEqualityGroup(new DefaultTopologyVertex(D1),
                                  new DefaultTopologyVertex(D1))
                .addEqualityGroup(new DefaultTopologyVertex(D2),
                                  new DefaultTopologyVertex(D2)).testEquals();
    }

}