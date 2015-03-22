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
package org.onosproject.net.tunnel;

import org.onosproject.net.ConnectPoint;
import org.onosproject.net.Description;

/**
 * Describes the tunnel.
 */
public interface TunnelDescription extends Description {

    /**
     * Returns the tunnel id.
     *
     * @return tunnelId
     */
    TunnelId id();

    /**
     * Returns the connection point source.
     *
     * @return tunnel source ConnectionPoint
     */
    ConnectPoint src();

    /**
     * Returns the connection point destination.
     *
     * @return tunnel destination
     */
    ConnectPoint dst();

    /**
     * Returns the tunnel type.
     *
     * @return tunnel type
     */
    Tunnel.Type type();

    /**
     * Returns if the tunnel is bidirectional.
     *
     * @return true if bidirectional, otherwise false
     */
    boolean isBidirectional();

}
