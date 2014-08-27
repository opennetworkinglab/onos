package org.onlab.onos.event;

import java.util.ArrayList;
import java.util.List;

/**
 * Test event listener manager fixture.
 */
public class TestListenerManager
        extends AbstractListenerManager<TestEvent, TestListener> {

    public final List<Throwable> errors = new ArrayList<>();

    @Override
    protected void reportProblem(TestEvent event, Throwable error) {
        super.reportProblem(event, error);
        errors.add(error);
    }

}

