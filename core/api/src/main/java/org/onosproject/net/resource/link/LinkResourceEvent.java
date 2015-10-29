/*
 * Copyright 2014 Open Networking Laboratory
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
package org.onosproject.net.resource.link;

import java.util.Collection;

import org.onosproject.event.AbstractEvent;

import com.google.common.collect.ImmutableList;

/**
 * Describes an event related to a Link Resource.
 *
 * @deprecated in Emu Release
 */
@Deprecated
public final class LinkResourceEvent
       extends AbstractEvent<LinkResourceEvent.Type, Collection<LinkResourceAllocations>> {

    /**
     * Type of resource this event is for.
     */
    public enum Type {
        /** Additional resources are now available. */
        ADDITIONAL_RESOURCES_AVAILABLE
    }

    /**
     * Constructs a link resource event.
     *
     * @param type type of resource event to create
     * @param linkResourceAllocations allocations that are now available
     */
    public LinkResourceEvent(Type type,
                             Collection<LinkResourceAllocations> linkResourceAllocations) {
        super(type, ImmutableList.copyOf(linkResourceAllocations));
    }
}
