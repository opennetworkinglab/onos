/*
 * Copyright 2014-present Open Networking Foundation
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
import com.google.common.base.MoreObjects;

import java.util.Objects;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Represents allocation of resource which is identified by the specifier.
 */
@Beta
public class ResourceAllocation {

    private final Resource resource;
    private final ResourceConsumerId consumerId;

    /**
     * Creates an instance with the specified subject, resource and consumerId.
     *
     * @param resource resource of the subject
     * @param consumerId consumer ID of this resource
     */
    public ResourceAllocation(Resource resource, ResourceConsumerId consumerId) {
        this.resource = checkNotNull(resource);
        this.consumerId = checkNotNull(consumerId);
    }

    /**
     * Creates an instance with the specified subject, resource and consumer.
     *
     * @param resource resource of the subject
     * @param consumer consumer of this resource
     */
    public ResourceAllocation(Resource resource, ResourceConsumer consumer) {
        this(resource, checkNotNull(consumer).consumerId());
    }

    // for serialization
    private ResourceAllocation() {
        this.resource = null;
        this.consumerId = null;
    }

    /**
     * Returns the specifier of the resource this allocation uses.
     *
     * @return the specifier of the resource this allocation uses
     */
    public Resource resource() {
        return resource;
    }

    /**
     * Returns ID of the consumer of this resource.
     *
     * @return ID of the consumer of this resource
     */
    public ResourceConsumerId consumerId() {
        return consumerId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(resource, consumerId);
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
                && Objects.equals(this.consumerId, that.consumerId);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("resource", resource)
                .add("consumerId", consumerId)
                .toString();
    }
}
