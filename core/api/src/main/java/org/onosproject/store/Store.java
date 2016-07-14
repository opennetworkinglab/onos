/*
 * Copyright 2014-present Open Networking Laboratory
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

import org.onosproject.event.Event;

/**
 * Abstraction of a entity capable of storing and/or distributing information
 * across a cluster.
 */
public interface Store<E extends Event, D extends StoreDelegate<E>> {

    /**
     * Sets the delegate on the store.
     *
     * @param delegate new store delegate
     * @throws java.lang.IllegalStateException if a delegate is already
     *                                         currently set on the store and is a different one that
     */
    void setDelegate(D delegate);

    /**
     * Withdraws the delegate from the store.
     *
     * @param delegate store delegate to withdraw
     * @throws java.lang.IllegalArgumentException if the delegate is not
     *                                            currently set on the store
     */
    void unsetDelegate(D delegate);

    /**
     * Indicates whether the store has a delegate.
     *
     * @return true if delegate is set
     */
    boolean hasDelegate();

}
