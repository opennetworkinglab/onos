/*
 * Copyright 2015 Open Networking Laboratory
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
 *
 */

package org.onosproject.ui.impl.topo;

import org.onosproject.event.ListenerRegistry;
import org.slf4j.Logger;

import java.util.HashSet;
import java.util.Set;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * A listener registry that automatically prunes listeners that have not
 * been sending heartbeat messages to assure us they are really listening.
 */
// package private
class ModelListenerRegistry
        extends ListenerRegistry<TopoUiEvent, TopoUiListener> {

    private final Logger log = getLogger(getClass());

    private final Set<TopoUiListener> zombies = new HashSet<>();

    @Override
    public void process(TopoUiEvent event) {
        zombies.clear();
        for (TopoUiListener listener : listeners) {
            try {
                if (listener.isAwake()) {
                    listener.event(event);
                } else {
                    zombies.add(listener);
                }
            } catch (Exception error) {
                reportProblem(event, error);
            }
        }

        // clean up zombie listeners
        for (TopoUiListener z : zombies) {
            log.debug("Removing zombie model listener: {}", z);
            removeListener(z);
        }
    }

}
