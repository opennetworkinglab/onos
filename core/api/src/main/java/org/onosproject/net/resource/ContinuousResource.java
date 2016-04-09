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
import com.google.common.base.MoreObjects;

import java.util.Objects;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Represents a resource path which specifies a resource which can be measured
 * as continuous value. Bandwidth of a link is an example of the resource.
 */
@Beta
public final class ContinuousResource implements Resource {
    private final ContinuousResourceId id;
    private final double value;

    ContinuousResource(ContinuousResourceId id, double value) {
        this.id = id;
        this.value = value;
    }

    // for serializer
    ContinuousResource() {
        this.id = null;
        this.value = 0;
    }

    @Override
    public ContinuousResourceId id() {
        return id;
    }

    @Override
    public String simpleTypeName() {
        return id.simpleTypeName();
    }

    @Override
    public boolean isTypeOf(Class<?> type) {
        checkNotNull(type);

        return id.isTypeOf(type);
    }

    /**
     * Returns the value of the resource amount.
     *
     * @return the value of the resource amount
     */
    public double value() {
        return value;
    }

    @Override
    public boolean isSubTypeOf(Class<?> ancestor) {
        checkNotNull(ancestor);

        return id.isSubTypeOf(ancestor);
    }

    /**
     * {@inheritDoc}
     *
     * A user must specify Double.class or double.class to avoid an empty value.
     */
    @Override
    public <T> Optional<T> valueAs(Class<T> type) {
        checkNotNull(type);

        if (type == Object.class || type == double.class || type == Double.class) {
            @SuppressWarnings("unchecked")
            T value = (T) Double.valueOf(this.value);
            return Optional.of(value);
        }

        return Optional.empty();
    }

    @Override
    public DiscreteResource child(Object child) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ContinuousResource child(Class<?> child, double value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Optional<DiscreteResource> parent() {
        return id.parent().map(x -> Resources.discrete(x).resource());
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, value);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final ContinuousResource other = (ContinuousResource) obj;
        return Objects.equals(this.id, other.id)
                && Objects.equals(this.value, other.value);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("id", id)
                .add("value", value)
                .toString();
    }
}
