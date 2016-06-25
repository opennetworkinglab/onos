/*
 * Copyright 2015-present Open Networking Laboratory
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

package org.onosproject.reactive.routing;

/**
 * Specifies the type of an IP address or an IP prefix location.
 */
enum LocationType {
    /**
     * The location of an IP address or an IP prefix is in local SDN network.
     */
    LOCAL,
    /**
     * The location of an IP address or an IP prefix is outside local SDN network.
     */
    INTERNET,
    /**
     * There is no route for this IP address or IP prefix.
     */
    NO_ROUTE
}
