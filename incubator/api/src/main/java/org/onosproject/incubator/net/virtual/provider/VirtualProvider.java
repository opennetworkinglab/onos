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

/**
 * Abstraction of a provider of information about virtual network environment.
 * The role of virtual providers is to translate virtual objects into physical
 * objects, vice versa.
 */
public interface VirtualProvider {

    /**
     * Returns the provider identifier.
     *
     * @return provider identification
     */
    ProviderId id();
}
