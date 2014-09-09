package org.onlab.onos.event;

/**
 * Test event listener fixture.
 */
public class BrokenListener extends TestListener {

    public void event(TestEvent event) {
        throw new IllegalStateException("boom");
    }

}

