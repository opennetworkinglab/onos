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

package org.onosproject.net.driver;

import java.util.Optional;

import com.google.common.annotations.Beta;

/**
 * Abstraction of an entity capable of being projected as another entity.
 */
@Beta
public interface Projectable {

    /**
     * Returns the specified projection of this entity if such projection
     * is supported.
     *
     * @param projectionClass requested projection class
     * @param <B> type of behaviour
     * @return projection instance
     * @throws IllegalStateException if a driver cannot be found
     * @throws IllegalArgumentException if the projection is not supported
     */
    <B extends Behaviour> B as(Class<B> projectionClass);

    /**
     * Returns true if this entity is capable of being projected as the
     * specified class.
     *
     * @param projectionClass requested projection class
     * @param <B> type of behaviour
     * @return true if the requested projection is supported
     */
    <B extends Behaviour> boolean is(Class<B> projectionClass);

    /**
     * Returns the specified projection of this entity if such projection
     * is supported.
     *
     * @param projectionClass requested projection class
     * @param <B> type of behaviour
     * @return projection instance
     */
    default <B extends Behaviour> Optional<B> project(Class<B> projectionClass) {
        if (is(projectionClass)) {
            return Optional.of(as(projectionClass));
        } else {
            return Optional.empty();
        }
    }

}
