package org.onlab.onos.event;

/**
 * Base abstraction of an event.
 */
public class AbstractEvent<T extends Enum, S extends Object> implements Event<T, S> {

    private final long time;
    private final T type;
    private S subject;

    /**
     * Creates an event of a given type and for the specified subject and the
     * current time.
     *
     * @param type    event type
     * @param subject event subject
     */
    protected AbstractEvent(T type, S subject) {
        this(type, subject, System.currentTimeMillis());
    }

    /**
     * Creates an event of a given type and for the specified subject and time.
     *
     * @param type    event type
     * @param subject event subject
     * @param time    occurrence time
     */
    protected AbstractEvent(T type, S subject, long time) {
        this.type = type;
        this.subject = subject;
        this.time = time;
    }

    @Override
    public long time() {
        return time;
    }

    @Override
    public T type() {
        return type;
    }

    @Override
    public S subject() {
        return subject;
    }

}
