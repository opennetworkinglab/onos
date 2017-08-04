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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * ResourceId for {@link DiscreteResource}.
 */
@Beta
public final class DiscreteResourceId extends ResourceId {
    private final ImmutableList<Object> components;

    DiscreteResourceId(ImmutableList<Object> components) {
        this.components = components;
    }

    DiscreteResourceId() {
        this.components = ImmutableList.of();
    }

    @Override
    ImmutableList<Object> components() {
        return components;
    }

    @Override
    String simpleTypeName() {
        if (isRoot()) {
            return "Root";
        }

        return lastComponent().getClass().getSimpleName();
    }

    @Override
    boolean isTypeOf(Class<?> type) {
        if (isRoot()) {
            return false;
        }

        return type.isAssignableFrom(lastComponent().getClass());
    }

    @Override
    boolean isSubTypeOf(Class<?> ancestor) {
        return components.stream()
                .filter(x -> ancestor.isAssignableFrom(x.getClass()))
                .findAny()
                .isPresent();
    }

    @Override
    public DiscreteResourceId child(Object child) {
        checkArgument(!(child instanceof Class<?>));

        return Resources.discrete(this, child).id();
    }

    @Override
    public ContinuousResourceId child(Class<?> child) {
        checkNotNull(child);

        return Resources.continuous(this, child).id();
    }

    @Override
    public Optional<DiscreteResourceId> parent() {
        if (isRoot()) {
            return Optional.empty();
        }
        if (components.size() == 1) {
            return Optional.of(ROOT);
        } else {
            return Optional.of(new DiscreteResourceId(components.subList(0, components.size() - 1)));
        }
    }

    <T> Optional<T> lastComponentAs(Class<T> type) {
        if (!isTypeOf(type)) {
            return Optional.empty();
        }

        @SuppressWarnings("unchecked")
        T value = (T) lastComponent();
        return Optional.of(value);
    }


    private boolean isRoot() {
        return components.isEmpty();
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
        final DiscreteResourceId other = (DiscreteResourceId) obj;
        return Objects.equals(this.components, other.components);
    }

    @Override
    public String toString() {
        return components.toString();
    }
}
