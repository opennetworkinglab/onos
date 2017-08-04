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
import com.google.common.collect.ImmutableList;

import java.util.Objects;
import java.util.Optional;

/**
 * ResourceId for {@link ContinuousResource}.
 */
@Beta
public final class ContinuousResourceId extends ResourceId {
    private final ImmutableList<Object> components;

    // for printing purpose only (used in toString() implementation)
    private final String name;

    ContinuousResourceId(ImmutableList.Builder<Object> parentComponents, Class<?> last) {
        this.components = parentComponents.add(last.getCanonicalName()).build();
        this.name = last.getSimpleName();
    }

    // for serializer
    ContinuousResourceId() {
        this.components = ImmutableList.of();
        this.name = "";
    }

    @Override
    ImmutableList<Object> components() {
        return components;
    }

    @Override
    String simpleTypeName() {
        return name;
    }

    @Override
    boolean isTypeOf(Class<?> type) {
        String typeName = (String) lastComponent();
        return typeName.equals(type.getCanonicalName());
    }

    @Override
    boolean isSubTypeOf(Class<?> ancestor) {
        String typeName = (String) lastComponent();
        boolean foundInLeaf = typeName.equals(ancestor.getCanonicalName());
        boolean foundInAncestor = components.subList(0, components.size()).stream()
                .anyMatch(x -> ancestor.isAssignableFrom(x.getClass()));
        return foundInAncestor || foundInLeaf;
    }

    /**
     * {@inheritDoc}
     *
     * A child of a continuous-type resource is prohibited.
     * {@link UnsupportedOperationException} is always thrown.
     */
    @Override
    public DiscreteResourceId child(Object child) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     *
     * A child of a continuous-type resource is prohibited.
     * {@link UnsupportedOperationException} is always thrown.
     */
    @Override
    public ContinuousResourceId child(Class<?> child) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Optional<DiscreteResourceId> parent() {
        if (components.isEmpty()) {
            return Optional.empty();
        }
        if (components.size() == 1) {
            return Optional.of(ROOT);
        } else {
            return Optional.of(new DiscreteResourceId(components.subList(0, components.size() - 1)));
        }
    }

    private Object lastComponent() {
        return components.get(components.size() - 1);
    }

    @Override
    public int hashCode() {
        return components.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final ContinuousResourceId other = (ContinuousResourceId) obj;
        return Objects.equals(this.components, other.components);
    }

    @Override
    public String toString() {
        // due to performance consideration, the value might need to be stored in a field
        return ImmutableList.builder()
                .addAll(components.subList(0, components.size() - 1))
                .add(name)
                .build().toString();
    }
}
