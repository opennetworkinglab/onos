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

/**
 * Factory class for discrete-type resource related instances.
 */
@Beta
public final class DiscreteFactory {
    private final DiscreteResourceId id;
    private final DiscreteResource resource;

    /**
     * Create an instance with the specified resource ID.
     *
     * @param id resource ID that is associated with the resource related instances
     *           which will be created from this instance
     */
    DiscreteFactory(DiscreteResourceId id) {
        this.id = id;
        this.resource = new DiscreteResource(id);
    }

    /**
     * Returns the resource ID for discrete-type.
     *
     * @return discrete-type resource ID
     */
    public DiscreteResourceId id() {
        return id;
    }

    /**
     * Returns the resource for discrete-type.
     *
     * @return discrete-type resource
     */
    public DiscreteResource resource() {
        return resource;
    }
}
