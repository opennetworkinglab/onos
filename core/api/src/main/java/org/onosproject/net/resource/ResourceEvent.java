/*
 * Copyright 2016-present Open Networking Foundation
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
package org.onosproject.net.resource;

import com.google.common.annotations.Beta;
import org.onosproject.event.AbstractEvent;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Describes an event related to a resource.
 */
@Beta
public final class ResourceEvent extends AbstractEvent<ResourceEvent.Type, Resource> {

    /**
     * Type of resource events.
     */
    @Beta
    public enum Type {
        /**
         * Signifies that a new resource has been detected.
         */
        RESOURCE_ADDED,

        /**
         * Signifies that a resource has been removed.
         */
        RESOURCE_REMOVED
    }

    /**
     * Create a resource event.
     *
     * @param type type of resource event
     * @param subject subject of resource event
     */
    public ResourceEvent(Type type, Resource subject) {
        super(checkNotNull(type), checkNotNull(subject));
    }
}
