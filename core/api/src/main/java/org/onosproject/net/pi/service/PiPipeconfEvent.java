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

package org.onosproject.net.pi.service;

import org.onosproject.event.AbstractEvent;
import org.onosproject.net.pi.model.PiPipeconf;
import org.onosproject.net.pi.model.PiPipeconfId;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Event related to the PiPipeconfService.
 */
public class PiPipeconfEvent extends AbstractEvent<PiPipeconfEvent.Type, PiPipeconfId> {

    private final PiPipeconf pipeconf;

    /**
     * Type of pipeconf event.
     */
    public enum Type {
        REGISTERED,
        UNREGISTERED
    }

    /**
     * Creates anew pipeconf event for the given type and pipeconf.
     *
     * @param type     type of event
     * @param pipeconf pipeconf
     */
    public PiPipeconfEvent(Type type, PiPipeconf pipeconf) {
        super(type, checkNotNull(pipeconf).id());
        this.pipeconf = pipeconf;
    }


    /**
     * Creates anew pipeconf event for the given type and pipeconf ID.
     *
     * @param type       type of event
     * @param pipeconfId pipeconf ID
     */
    public PiPipeconfEvent(Type type, PiPipeconfId pipeconfId) {
        super(type, pipeconfId);
        pipeconf = null;
    }

    /**
     * Returns the pipeconf instance associated to this event, or null if one
     * was not provided. For example, {@link Type#UNREGISTERED} events are not
     * expected to carry the pipeconf instance that was unregistered, but just
     * the ID (via {@link #subject()}).
     *
     * @return pipeconf instance or null
     */
    public PiPipeconf pipeconf() {
        return pipeconf;
    }
}
