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
package org.onosproject.drivers.optical;

import com.google.common.annotations.Beta;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.link.LinkDescription;

/**
 * Interface to allow for optical device drivers to add/remove links to
 * the store. Acts as a proxy to LinkProviderService.
 */
@Beta
public interface OpticalAdjacencyLinkService {
    /**
     * Signals that an infrastructure link has been detected.
     *
     * @param linkDescription link information
     */
    void linkDetected(LinkDescription linkDescription);

    /**
     * Signals that an infrastructure link has disappeared.
     *
     * @param linkDescription link information
     */
    void linkVanished(LinkDescription linkDescription);

    /**
     * Signals that infrastructure links associated with the specified
     * connect point have vanished.
     *
     * @param connectPoint connect point
     */
    void linksVanished(ConnectPoint connectPoint);

    /**
     * Signals that infrastructure links associated with the specified
     * device have vanished.
     *
     * @param deviceId device identifier
     */
    void linksVanished(DeviceId deviceId);
}
