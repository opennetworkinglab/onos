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
package org.onosproject.incubator.net.tunnel;

import com.google.common.annotations.Beta;
import org.onosproject.core.GroupId;
import org.onosproject.incubator.net.tunnel.Tunnel.Type;
import org.onosproject.net.Annotated;
import org.onosproject.net.Description;
import org.onosproject.net.NetworkResource;
import org.onosproject.net.Path;
import org.onosproject.net.provider.ProviderId;

/**
 * Describes a tunnel.
 */
@Beta
public interface TunnelDescription extends Description, Annotated {

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
    TunnelEndPoint src();

    /**
     * Returns the connection point destination.
     *
     * @return tunnel destination
     */
    TunnelEndPoint dst();

    /**
     * Returns the tunnel type.
     *
     * @return tunnel type
     */
    Type type();

    /**
     * Returns group flow table id which a tunnel match up.
     *
     * @return OpenFlowGroupId
     */
    GroupId groupId();

    /**
     * Returns tunnel producer name.
     *
     * @return producer name
     */
    ProviderId producerName();

    /**
     * Return the name of a tunnel.
     *
     * @return Tunnel Name
     */
    TunnelName tunnelName();

    /**
     * Returns the path of the tunnel.
     *
     * @return the path of the tunnel
     */
    Path path();

    /**
     * Returns the network resource backing the tunnel, e.g. lambda, VLAN id, MPLS tag, label stack.
     *
     * @return backing resource
     */
    NetworkResource resource();
}
