/*
 * Copyright 2016-present Open Networking Laboratory
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
package org.onosproject.event;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.concurrent.NotThreadSafe;

import org.apache.commons.lang3.tuple.Pair;
import com.google.common.annotations.Beta;
import com.google.common.collect.Lists;

/**
 * Utility to keeps track of registered Listeners.
 * <p>
 * Usage Example:
 * <pre>
    private ListenerTracker listeners;

    {@code @Activate}
    protected void activate() {
        listeners = new ListenerTracker();
        listeners.addListener(mastershipService, new InternalMastershipListener())
                 .addListener(deviceService, new InternalDeviceListener())
                 .addListener(linkService, new InternalLinkListener())
                 .addListener(topologyService, new InternalTopologyListener())
                 .addListener(hostService, new InternalHostListener());
    }

    {@code @Deactivate}
    protected void deactivate() {
        listeners.removeListeners();
    }
 * </pre>
 */
@Beta
@NotThreadSafe
public class ListenerTracker {

    @SuppressWarnings("rawtypes")
    private List<Pair<ListenerService, EventListener>> listeners = new ArrayList<>();

    /**
     * Adds {@link EventListener} to specified {@link ListenerService}.
     *
     * @param <E> event
     * @param <L> listener
     * @param service {@link ListenerService}
     * @param listener {@link EventListener}
     * @return self
     */
    public <E extends Event<?, ?>, L extends EventListener<E>>
    ListenerTracker addListener(ListenerService<E, L> service, L listener) {

        checkNotNull(service);
        checkNotNull(listener);
        service.addListener(listener);
        listeners.add(Pair.of(service, listener));
        return this;
    }

    /**
     * Removes all listeners in reverse order they have been registered.
     */
    public void removeListeners() {
        Lists.reverse(listeners)
            .forEach(r -> r.getLeft().removeListener(r.getRight()));
        listeners.clear();
    }
}
