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

package org.onosproject.incubator.net.virtual.provider;

import org.onosproject.net.provider.ProviderId;

public abstract class AbstractVirtualProvider implements VirtualProvider {
    private final ProviderId providerId;

    /**
     * Creates a virtual provider with the supplied identifier.
     *
     * @param id a virtual provider id
     */
    protected AbstractVirtualProvider(ProviderId id) {
        this.providerId = id;
    }

    @Override
    public ProviderId id() {
        return providerId;
    }
}
