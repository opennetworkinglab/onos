package org.onlab.onos.net.intent;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.onlab.onos.net.NetTestTools;
import org.onlab.onos.net.Path;

public class PathIntentTest extends ConnectivityIntentTest {
    // 111:11 --> 222:22
    private static final Path PATH1 = NetTestTools.createPath("111", "222");

    // 111:11 --> 333:33
    private static final Path PATH2 = NetTestTools.createPath("222", "333");

    @Test
    public void basics() {
        PathIntent intent = createOne();
        assertEquals("incorrect id", IID, intent.id());
        assertEquals("incorrect match", MATCH, intent.selector());
        assertEquals("incorrect action", NOP, intent.treatment());
        assertEquals("incorrect ingress", P1, intent.ingressPoint());
        assertEquals("incorrect egress", P2, intent.egressPoint());
        assertEquals("incorrect path", PATH1, intent.path());
    }

    @Override
    protected PathIntent createOne() {
        return new PathIntent(IID, MATCH, NOP, P1, P2, PATH1);
    }

    @Override
    protected PathIntent createAnother() {
        return new PathIntent(IID, MATCH, NOP, P1, P3, PATH2);
    }
}
