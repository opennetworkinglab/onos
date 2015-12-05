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
import com.google.common.base.MoreObjects;

import java.util.Objects;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Represents allocation of resource which is identified by the specifier.
 */
@Beta
public class ResourceAllocation {

    private final ResourcePath resource;
    private final ResourceConsumer consumer;

    /**
     * Creates an instance with the specified subject, resource and consumer.
     *
     * @param resource resource of the subject
     * @param consumer consumer of this resource
     */
    public ResourceAllocation(ResourcePath resource, ResourceConsumer consumer) {
        this.resource = checkNotNull(resource);
        this.consumer = consumer;
    }

    // for serialization
    private ResourceAllocation() {
        this.resource = null;
        this.consumer = null;
    }

    /**
     * Returns the specifier of the resource this allocation uses.
     *
     * @return the specifier of the resource this allocation uses
     */
    public ResourcePath resource() {
        return resource;
    }

    /**
     * Returns the consumer of this resource.
     *
     * @return the consumer of this resource
     */
    public ResourceConsumer consumer() {
        return consumer;
    }

    @Override
    public int hashCode() {
        return Objects.hash(resource, consumer);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof ResourceAllocation)) {
            return false;
        }
        final ResourceAllocation that = (ResourceAllocation) obj;
        return Objects.equals(this.resource, that.resource)
                && Objects.equals(this.consumer, that.consumer);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("resource", resource)
                .add("consumer", consumer)
                .toString();
    }
}
