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

/**
 * Factory class for continuous-type resource related instances.
 */
@Beta
public final class ContinuousFactory {
    private final ContinuousResourceId id;

    /**
     * Creates an instance with the specified resource ID.
     *
     * @param id resource ID that is associated with the resource related instances
     *           which will be created from this instance
     */
    ContinuousFactory(ContinuousResourceId id) {
        this.id = id;
    }

    /**
     * Returns the resource ID for continuous-type.
     *
     * @return continuous-type resource ID
     */
    public ContinuousResourceId id() {
        return id;
    }

    /**
     * Returns the resource for continuous-type specified by the given value.
     *
     * @param volume volume of the returned resource
     * @return continuous-type resource
     */
    public ContinuousResource resource(double volume) {
        return new ContinuousResource(id(), volume);
    }
}
