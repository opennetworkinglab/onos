/*
 * Copyright 2019-present Open Networking Foundation
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
package org.onosproject.store.atomix.primitives.impl;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import com.google.common.collect.Maps;
import io.atomix.primitive.PrimitiveState;
import org.onosproject.store.service.DistributedPrimitive;

/**
 * Atomix distributed primitive.
 */
public abstract class AtomixPrimitive implements DistributedPrimitive {
    private final io.atomix.primitive.AsyncPrimitive atomixPrimitive;
    private final Map<Consumer<Status>, Consumer<PrimitiveState>> listenerMap = Maps.newIdentityHashMap();

    protected AtomixPrimitive(io.atomix.primitive.AsyncPrimitive atomixPrimitive) {
        this.atomixPrimitive = atomixPrimitive;
    }

    @Override
    public String name() {
        return atomixPrimitive.name();
    }

    private Status toStatus(PrimitiveState state) {
        switch (state) {
            case CONNECTED:
                return Status.ACTIVE;
            case SUSPENDED:
            case EXPIRED:
                return Status.SUSPENDED;
            case CLOSED:
                return Status.INACTIVE;
            default:
                throw new AssertionError();
        }
    }

    @Override
    public synchronized void addStatusChangeListener(Consumer<Status> listener) {
        Consumer<PrimitiveState> atomixListener = state -> listener.accept(toStatus(state));
        listenerMap.put(listener, atomixListener);
        atomixPrimitive.addStateChangeListener(atomixListener);
    }

    @Override
    public synchronized void removeStatusChangeListener(Consumer<Status> listener) {
        Consumer<PrimitiveState> atomixListener = listenerMap.remove(listener);
        if (atomixListener != null) {
            atomixPrimitive.removeStateChangeListener(atomixListener);
        }
    }

    @Override
    public Collection<Consumer<Status>> statusChangeListeners() {
        return listenerMap.keySet();
    }

    @Override
    public CompletableFuture<Void> destroy() {
        return atomixPrimitive.close();
    }
}
