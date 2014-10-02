package org.onlab.onos.net.intent;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Suite of tests of the single-to-multi point intent descriptor.
 */
public class SinglePointToMultiPointIntentTest extends ConnectivityIntentTest {

    @Test
    public void basics() {
        SinglePointToMultiPointIntent intent = createOne();
        assertEquals("incorrect id", IID, intent.getId());
        assertEquals("incorrect match", MATCH, intent.getTrafficSelector());
        assertEquals("incorrect ingress", P1, intent.getIngressPort());
        assertEquals("incorrect egress", PS2, intent.getEgressPorts());
    }

    @Override
    protected SinglePointToMultiPointIntent createOne() {
        return new SinglePointToMultiPointIntent(IID, MATCH, NOP, P1, PS2);
    }

    @Override
    protected SinglePointToMultiPointIntent createAnother() {
        return new SinglePointToMultiPointIntent(IID, MATCH, NOP, P2, PS1);
    }
}
