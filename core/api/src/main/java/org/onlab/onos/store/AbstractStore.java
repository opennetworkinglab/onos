package org.onlab.onos.store;

import org.onlab.onos.event.Event;

/**
 * Base implementation of a store.
 */
public class AbstractStore<E extends Event, D extends StoreDelegate<E>>
        implements Store<E, D> {

    protected D delegate;

    @Override
    public void setDelegate(D delegate) {
        this.delegate = delegate;
    }

    @Override
    public D getDelegate() {
        return delegate;
    }

    /**
     * Notifies the delegate with the specified event.
     *
     * @param event event to delegate
     */
    protected void notifyDelegate(E event) {
        if (delegate != null) {
            delegate.notify(event);
        }
    }
}
