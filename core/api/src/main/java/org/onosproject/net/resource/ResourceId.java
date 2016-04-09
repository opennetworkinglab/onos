/*
 * Copyright 2014-present Open Networking Laboratory
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

import java.util.Optional;

/**
 * Represents identifier of resource.
 */
@Beta
public abstract class ResourceId {
    static final DiscreteResourceId ROOT = new DiscreteResourceId();

    abstract ImmutableList<Object> components();

    abstract String simpleTypeName();

    // caller must pass a non-null value
    abstract boolean isTypeOf(Class<?> type);

    // caller must pass a non-null value
    abstract boolean isSubTypeOf(Class<?> ancestor);

    /**
     * Returns the parent resource ID of this instance.
     *
     * @return the parent resource ID of this instance.
     * If there is no parent, empty instance will be returned.
     */
    public abstract Optional<DiscreteResourceId> parent();

    /**
     * Returns a resource ID of a child of this resource based on the specified object.
     * If the given object is a {@link Class} instance, {@link IllegalArgumentException} is thrown.
     *
     * @param child the last component of the child
     * @return a child resource ID
     */
    public abstract DiscreteResourceId child(Object child);

    /**
     * Returns a resource ID of a child of this resource based on the specified object.
     *
     * @param child the last component of the child
     * @return a child resource ID
     */
    public abstract ContinuousResourceId child(Class<?> child);
}
