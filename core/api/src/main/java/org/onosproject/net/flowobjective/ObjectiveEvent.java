/*
 * Copyright 2015-present Open Networking Laboratory
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
package org.onosproject.net.flowobjective;

import com.google.common.annotations.Beta;
import org.onosproject.event.AbstractEvent;

/**
 * Describes a objective event.
 */
@Beta
public class ObjectiveEvent extends AbstractEvent<ObjectiveEvent.Type, Integer> {

    /**
     * Type of objective events.
     */
    public enum Type {
        /**
         * Signifies that the objective has been added to the store.
         */
        ADD,

        /**
         * Signifies that the objective has been removed.
         */
        REMOVE
    }

    /**
     * Creates an event of the given type for the specified objective id.
     *
     * @param type the type of the event
     * @param objective the objective id the event is about
     */
    public ObjectiveEvent(Type type, Integer objective) {
        super(type, objective);
    }

    /**
     * Creates an event of the given type for the specified objective id at the given
     * time.
     *
     * @param type the type of the event
     * @param objective the objective id the event is about
     * @param time the time of the event
     */
    public ObjectiveEvent(Type type, Integer objective, long time) {
        super(type, objective, time);
    }
}

