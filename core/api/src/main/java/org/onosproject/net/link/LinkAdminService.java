/*
 * Copyright 2014-present Open Networking Laboratory
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
package org.onosproject.net.link;

import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;

/**
 * Service for administering the inventory of infrastructure links.
 */
public interface LinkAdminService extends LinkService {

    /**
     * Removes all infrastructure links leading to and from the
     * specified connection point.
     *
     * @param connectPoint connection point
     */
    void removeLinks(ConnectPoint connectPoint);

    /**
     * Removes all infrastructure links leading to and from the
     * specified device.
     *
     * @param deviceId device identifier
     */
    void removeLinks(DeviceId deviceId);

    /**
     * Removes all links between between the specified src and
     * dst connection points.
     *
     * @param src link source
     * @param dst link destination
     */
    void removeLink(ConnectPoint src, ConnectPoint dst);
}
