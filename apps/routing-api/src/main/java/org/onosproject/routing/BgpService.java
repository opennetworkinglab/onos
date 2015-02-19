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
package org.onosproject.routing;

/**
 * Provides a way of interacting with the BGP protocol component.
 */
public interface BgpService {

    /**
     * Starts the BGP service.
     *
     * @param routeListener listener to send route updates to
     */
    void start(RouteListener routeListener);

    /**
     * Stops the BGP service.
     */
    void stop();
}
