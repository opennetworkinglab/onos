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
package org.onosproject.net.resource;

import com.google.common.annotations.Beta;
import com.google.common.collect.ImmutableSet;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

/**
 * Represents a set of discrete type resources.
 * This class is intended to be used by ConsistentResourceStore though it is exposed to the public.
 */
@Beta
public final class DiscreteResourceSet {
    private final Set<DiscreteResource> values;
    private final DiscreteResourceCodec codec;

    private static final DiscreteResourceSet EMPTY = new DiscreteResourceSet(ImmutableSet.of(), NoOpCodec.INSTANCE);

    /**
     * Creates an instance with resources and the codec for them.
     *
     * @param values resources to be contained in the instance
     * @param codec codec for the specified resources
     * @return an instance
     */
    public static DiscreteResourceSet of(Set<DiscreteResource> values, DiscreteResourceCodec codec) {
        checkNotNull(values);
        checkNotNull(codec);
        checkArgument(!values.isEmpty());

        return new DiscreteResourceSet(ImmutableSet.copyOf(values), codec);
    }

    /**
     * Creates the instance representing an empty resource set.
     *
     * @return an empty resource set
     */
    public static DiscreteResourceSet empty() {
        return EMPTY;
    }

    private DiscreteResourceSet(Set<DiscreteResource> values, DiscreteResourceCodec codec) {
        this.values = values;
        this.codec = codec;
    }

    private DiscreteResourceSet() {
        this.values = null;
        this.codec = null;
    }

    /**
     * Returns resources contained in this instance.
     *
     * @return resources
     */
    public Set<DiscreteResource> values() {
        return values;
    }

    /**
     * Returns the parent resource of the resources contained in this instance.
     *
     * @return the parent resource of the resources
     */
    public DiscreteResourceId parent() {
        if (values.isEmpty()) {
            // Dummy value avoiding null
            return ResourceId.ROOT;
        }
        Optional<DiscreteResourceId> parent = values.iterator().next().id().parent();
        checkState(parent.isPresent());

        return parent.get();
    }

    /**
     * Returns the codec for the resources contained in this instance.
     *
     * @return the codec for the resources
     */
    public DiscreteResourceCodec codec() {
        return codec;
    }

    @Override
    public int hashCode() {
        return Objects.hash(values, codec);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }

        final DiscreteResourceSet other = (DiscreteResourceSet) obj;
        return Objects.equals(this.values, other.values)
                && Objects.equals(this.codec, other.codec);
    }

}
