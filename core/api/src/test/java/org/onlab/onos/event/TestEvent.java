package org.onlab.onos.event;

/**
 * Test event fixture.
 */
public class TestEvent extends AbstractEvent<TestEvent.Type, String> {

    public enum Type { FOO, BAR };

    public TestEvent(Type type, String subject) {
        super(type, subject);
    }

    public TestEvent(Type type, String subject, long timestamp) {
        super(type, subject, timestamp);
    }

}

