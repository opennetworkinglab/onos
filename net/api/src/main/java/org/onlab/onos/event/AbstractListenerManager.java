package org.onlab.onos.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Base implementation of a manager capable of tracking listeners and
 * dispatching events to them.
 */
public class AbstractListenerManager<E extends Event, L extends EventListener<E>>
        implements EventSink<E> {

    protected Logger log = LoggerFactory.getLogger(AbstractListenerManager.class);

    private final Set<L> listeners = new CopyOnWriteArraySet<>();

    /**
     * Adds the specified listener.
     *
     * @param listener listener to be added
     */
    public void addListener(L listener) {
        checkNotNull(listener, "Listener cannot be null");
        listeners.add(listener);
    }

    /**
     * Removes the specified listener.
     *
     * @param listener listener to be removed
     */
    public void removeListener(L listener) {
        checkNotNull(listener, "Listener cannot be null");
        checkArgument(listeners.remove(listener), "Listener not registered");
    }

    @Override
    public void process(E event) {
        for (L listener : listeners) {
            try {
                listener.event(event);
            } catch (Throwable error) {
                reportProblem(event, error);
            }
        }
    }

    /**
     * Reports a problem encountered while processing an event.
     *
     * @param event event being processed
     * @param error error encountered while processing
     */
    protected void reportProblem(E event, Throwable error) {
        log.warn("Exception encountered while processing event " + event, error);
    }

}
