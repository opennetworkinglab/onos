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
package org.onosproject.incubator.net.resource.label;

import com.google.common.annotations.Beta;
import org.onosproject.event.AbstractEvent;

/**
 * Describes label resource event.
 */
@Beta
public final class LabelResourceEvent
        extends AbstractEvent<LabelResourceEvent.Type, LabelResourcePool> {

    /**
     * Type of label resource event.
     */
    public enum Type {
        /**
         * Signifies that a new pool has been administratively created.
         */
        POOL_CREATED,
        /**
         * Signifies that a new pool has been administratively destroyed.
         */
        POOL_DESTROYED,
        /**
         * Signifies that a new pool has been administratively changed.
         */
        POOL_CAPACITY_CHANGED
    }

    /**
     * Creates an event of a given type and the given LabelResourcePool.
     *
     * @param type event type
     * @param subject pool
     */
    public LabelResourceEvent(Type type, LabelResourcePool subject) {
        super(type, subject);
    }
}
