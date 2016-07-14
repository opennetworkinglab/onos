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
 * Specifies the type of traffic.
 * <p>
 * We classify traffic by the first packet of each traffic.
 * </p>
 */
enum TrafficType {
    /**
     * Traffic from a host located in local SDN network wants to
     * communicate with destination host located in Internet (outside
     * local SDN network).
     */
    HOST_TO_INTERNET,
    /**
     * Traffic from Internet wants to communicate with a host located
     * in local SDN network.
     */
    INTERNET_TO_HOST,
    /**
     * Both the source host and destination host of a traffic are in
     * local SDN network.
     */
    HOST_TO_HOST,
    /**
     * Traffic from Internet wants to traverse local SDN network.
     */
    INTERNET_TO_INTERNET,
    /**
     * Any traffic wants to communicate with a destination which has
     * no route, or traffic from Internet wants to access a local private
     * IP address.
     */
    DROP,
    /**
     * Traffic does not belong to the types above.
     */
    UNKNOWN
}
