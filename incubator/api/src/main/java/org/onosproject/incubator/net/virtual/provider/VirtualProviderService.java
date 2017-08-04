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

/**
 * Abstraction of a service through which virtual providers can inject information
 * about the network environment into the virtual core.
 *
 * @param <P> type of the information virtual provider
 */

public interface VirtualProviderService<P extends VirtualProvider> {
    /**
     * Returns the virtual provider to which this service has been issued.
     *
     * @return virtual provider to which this service has been assigned
     */
    P provider();
}
