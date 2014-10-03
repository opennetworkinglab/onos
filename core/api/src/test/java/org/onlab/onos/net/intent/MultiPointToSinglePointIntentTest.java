package org.onlab.onos.net.intent;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Suite of tests of the multi-to-single point intent descriptor.
 */
public class MultiPointToSinglePointIntentTest extends ConnectivityIntentTest {

    @Test
    public void basics() {
        MultiPointToSinglePointIntent intent = createOne();
        assertEquals("incorrect id", IID, intent.getId());
        assertEquals("incorrect match", MATCH, intent.getTrafficSelector());
        assertEquals("incorrect ingress", PS1, intent.getIngressPorts());
        assertEquals("incorrect egress", P2, intent.getEgressPort());
    }

    @Override
    protected MultiPointToSinglePointIntent createOne() {
        return new MultiPointToSinglePointIntent(IID, MATCH, NOP, PS1, P2);
    }

    @Override
    protected MultiPointToSinglePointIntent createAnother() {
        return new MultiPointToSinglePointIntent(IID, MATCH, NOP, PS2, P1);
    }
}
