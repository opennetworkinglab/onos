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
 */
package org.onosproject.net.newresource;

import com.google.common.annotations.Beta;
import org.onosproject.net.LinkKey;

/**
 * Represents resource which belongs to a particular link.
 *
 * @param <T> represents the type of the resource
 */
@Beta
public final class LinkResource<T> extends AbstractResource<LinkKey, T> {

    /**
     * Creates a new link resource from the specified link identifier and resource.
     * (linkKey, resource) signifies a resource identifier which is globally unique.
     *
     * @param linkKey link identifier which this resource belongs to
     * @param resource resource of the link
     */
    public LinkResource(LinkKey linkKey, T resource) {
        super(linkKey, resource);
    }
}
