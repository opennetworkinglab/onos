package org.onlab.onos.net.intent;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Objects;

import com.google.common.base.MoreObjects;

/**
 * A class to represent an intent related event.
 */
public class IntentEvent {

    // TODO: determine a suitable parent class; if one does not exist, consider introducing one

    private final long time;
    private final Intent intent;
    private final IntentState state;
    private final IntentState previous;

    /**
     * Creates an event describing a state change of an intent.
     *
     * @param intent subject intent
     * @param state new intent state
     * @param previous previous intent state
     * @param time time the event created in milliseconds since start of epoch
     * @throws NullPointerException if the intent or state is null
     */
    public IntentEvent(Intent intent, IntentState state, IntentState previous, long time) {
        this.intent = checkNotNull(intent);
        this.state = checkNotNull(state);
        this.previous = previous;
        this.time = time;
    }

    /**
     * Constructor for serializer.
     */
    protected IntentEvent() {
        this.intent = null;
        this.state = null;
        this.previous = null;
        this.time = 0;
    }

    /**
     * Returns the state of the intent which caused the event.
     *
     * @return the state of the intent
     */
    public IntentState getState() {
        return state;
    }

    /**
     * Returns the previous state of the intent which caused the event.
     *
     * @return the previous state of the intent
     */
    public IntentState getPreviousState() {
        return previous;
    }

    /**
     * Returns the intent associated with the event.
     *
     * @return the intent
     */
    public Intent getIntent() {
        return intent;
    }

    /**
     * Returns the time at which the event was created.
     *
     * @return the time in milliseconds since start of epoch
     */
    public long getTime() {
        return time;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        IntentEvent that = (IntentEvent) o;
        return Objects.equals(this.intent, that.intent)
                && Objects.equals(this.state, that.state)
                && Objects.equals(this.previous, that.previous)
                && Objects.equals(this.time, that.time);
    }

    @Override
    public int hashCode() {
        return Objects.hash(intent, state, previous, time);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("intent", intent)
                .add("state", state)
                .add("previous", previous)
                .add("time", time)
                .toString();
    }
}
