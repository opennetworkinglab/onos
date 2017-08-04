/*
 * Copyright 2014-present Open Networking Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onosproject.store;

import java.util.List;

import org.onosproject.event.Event;

import static com.google.common.base.Preconditions.checkState;

/**
 * Base implementation of a store.
 */
public class AbstractStore<E extends Event, D extends StoreDelegate<E>>
        implements Store<E, D> {

    protected D delegate;

    @Override
    public void setDelegate(D delegate) {
        checkState(this.delegate == null || this.delegate == delegate,
                   "Store delegate already set");
        this.delegate = delegate;
    }

    @Override
    public void unsetDelegate(D delegate) {
        if (this.delegate == delegate) {
            this.delegate = null;
        }
    }

    @Override
    public boolean hasDelegate() {
        return delegate != null;
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

    /**
     * Notifies the delegate with the specified list of events.
     *
     * @param events list of events to delegate
     */
    protected void notifyDelegate(List<E> events) {
        for (E event: events) {
            notifyDelegate(event);
        }
    }
}
