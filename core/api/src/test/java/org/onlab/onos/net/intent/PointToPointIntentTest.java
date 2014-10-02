package org.onlab.onos.net.intent;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Suite of tests of the point-to-point intent descriptor.
 */
public class PointToPointIntentTest extends ConnectivityIntentTest {

    @Test
    public void basics() {
        PointToPointIntent intent = createOne();
        assertEquals("incorrect id", IID, intent.getId());
        assertEquals("incorrect match", MATCH, intent.getTrafficSelector());
        assertEquals("incorrect ingress", P1, intent.getIngressPort());
        assertEquals("incorrect egress", P2, intent.getEgressPort());
    }

    @Override
    protected PointToPointIntent createOne() {
        return new PointToPointIntent(IID, MATCH, NOP, P1, P2);
    }

    @Override
    protected PointToPointIntent createAnother() {
        return new PointToPointIntent(IID, MATCH, NOP, P2, P1);
    }
}
