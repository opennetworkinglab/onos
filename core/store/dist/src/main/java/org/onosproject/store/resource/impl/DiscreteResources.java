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
package org.onosproject.store.resource.impl;

import org.onosproject.net.resource.DiscreteResource;
import org.onosproject.net.resource.DiscreteResourceId;

import java.util.List;
import java.util.Optional;
import java.util.Set;

interface DiscreteResources {
    static DiscreteResources empty() {
        return NonEncodableDiscreteResources.empty();
    }

    Optional<DiscreteResource> lookup(DiscreteResourceId id);

    DiscreteResources difference(DiscreteResources other);

    boolean isEmpty();

    boolean containsAny(List<DiscreteResource> other);

    // returns a new instance, not mutate the current instance
    DiscreteResources add(DiscreteResources other);

    // returns a new instance, not mutate the current instance
    DiscreteResources remove(List<DiscreteResource> removed);

    Set<DiscreteResource> values();
}
