/*
 * Copyright 2016 Open Networking Laboratory
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
import java.util.Optional;

/**
 * Represents a resource path which specifies a resource which can be measured
 * as continuous value. Bandwidth of a link is an example of the resource.
 * <p>
 * Note: This class is exposed to the public, but intended to be used in the resource API
 * implementation only. It is not for resource API user.
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

    /**
     * The user of this methods must receive the return value as Double or double.
     * Otherwise, this methods throws an exception.
     *
     * @param <T> type of the return value
     * @return the volume of this resource
     */
    @SuppressWarnings("unchecked")
    @Override
    public <T> T volume() {
        return (T) Double.valueOf(value);
    }

    /**
     * Returns the value of the resource amount.
     *
     * @return the value of the resource amount
     */
    // FIXME: overlapping a purpose with volume()
    public double value() {
        return value;
    }

    @Override
    public boolean isTypeOf(Class<?> ancestorType) {
        String typeName = (String) id.components().get(id.components().size() - 1);
        boolean foundInLeaf = typeName.equals(ancestorType.getCanonicalName());
        boolean foundInAncestor = id.components().subList(0, id.components().size()).stream()
                .map(Object::getClass)
                .filter(x -> x.equals(ancestorType))
                .findAny()
                .isPresent();
        return foundInAncestor || foundInLeaf;
    }

    @Override
    public Object last() {
        if (id.components().isEmpty()) {
            return null;
        }
        return id.components().get(id.components().size() - 1);
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
                .add("volume", value)
                .toString();
    }
}
