package org.onlab.onos.event;

import java.util.ArrayList;
import java.util.List;

/**
 * Test event listener fixture.
 */
public class TestListener implements EventListener<TestEvent> {

    public final List<TestEvent> events = new ArrayList<>();

    @Override
    public void event(TestEvent event) {
        events.add(event);
    }

}

