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

package org.onosproject.segmentrouting.pwaas;

import org.onlab.packet.MplsLabel;
import org.onlab.packet.VlanId;
import org.onosproject.net.Link;

import java.util.List;

public interface L2Tunnel {

    /**
     * Return the mode of the l2 tunnel.
     *
     * @return The pw mode.
     */
    L2Mode pwMode();

    /**
     * Returns the service delimiting tag.
     *
     * @return the sd tag
     */
    VlanId sdTag();

    /**
     * Returns the id of the tunnel.
     *
     * @return the tunnel id
     */
    long tunnelId();

    /**
     * Return the label of the pseudowire.
     *
     * @return the pw label.
     */
    MplsLabel pwLabel();

    /**
     * Returns the path used by the pseudowire.
     *
     * @return The path that is used
     */
    List<Link> pathUsed();

    /**
     * Returns the transport vlan used by the pseudowire.
     *
     * @return The transport vlan
     */
    VlanId transportVlan();

    /**
     * Returns the inter-co label used by the pseudowire.
     *
     * @return The inter CO label.
     */
    MplsLabel interCoLabel();

    /**
     * Sets the path that this pw uses.
     *
     * @param path The apth to use
     */
    void setPath(List<Link> path);

    /**
     * Set the transport vlan that this pw will use.
     *
     * @param vlan The vlan to use.
     */
    void setTransportVlan(VlanId vlan);
}
