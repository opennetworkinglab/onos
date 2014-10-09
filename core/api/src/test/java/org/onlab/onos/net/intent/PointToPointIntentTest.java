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
        assertEquals("incorrect id", IID, intent.id());
        assertEquals("incorrect match", MATCH, intent.selector());
        assertEquals("incorrect ingress", P1, intent.ingressPoint());
        assertEquals("incorrect egress", P2, intent.egressPoint());
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
