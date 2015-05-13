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
package org.onosproject.store.service;

import java.util.Set;

/**
 * Builder for distributed set.
 *
 * @param <E> type set elements.
 */
public interface SetBuilder<E> {

    /**
     * Sets the name of the set.
     * <p>
     * Each set is identified by a unique name.
     * </p>
     * <p>
     * Note: This is a mandatory parameter.
     * </p>
     *
     * @param name name of the set
     * @return this SetBuilder
     */
    SetBuilder<E> withName(String name);

    /**
     * Sets a serializer that can be used to serialize
     * the elements add to the set. The serializer
     * builder should be pre-populated with any classes that will be
     * put into the set.
     * <p>
     * Note: This is a mandatory parameter.
     * </p>
     *
     * @param serializer serializer
     * @return this SetBuilder
     */
    SetBuilder<E> withSerializer(Serializer serializer);

    /**
     * Disables set updates.
     * <p>
     * Attempt to update the built set will throw {@code UnsupportedOperationException}.
     *
     * @return this SetBuilder
     */
    SetBuilder<E> withUpdatesDisabled();

    /**
     * Builds an set based on the configuration options
     * supplied to this builder.
     *
     * @return new set
     * @throws java.lang.RuntimeException if a mandatory parameter is missing
     */
    Set<E> build();
}
