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

package org.onosproject.net.behaviour;

import com.google.common.annotations.Beta;
import org.onlab.packet.VlanId;
import org.onosproject.net.Annotated;
import org.onosproject.net.Description;

import java.util.List;
import java.util.Optional;

/**
 * The abstraction of a mirroring. Port mirroring is a method of monitoring
 * network traffic that forwards a copy of each incoming or outgoing packet from
 * one port (Monitor port) on a network switch to another port (Mirror port)
 * where the packet can be analyzed.
 */
@Beta
public interface MirroringDescription extends Description, Annotated {

    /**
     * Returns mirroring name.
     *
     * @return mirroring name
     */
    MirroringName name();

    /**
     * Returns src ports to monitor.
     * If it is empty, then no src port has
     * to be monitored.
     *
     * @return set of src ports to monitor
     */
    List<String> monitorSrcPorts();

    /**
     * Returns dst ports to monitor.
     * If it is empty, then no dst port has
     * to be monitored.
     *
     * @return set of dst ports to monitor
     */
    List<String> monitorDstPorts();

    /**
     * Returns vlans to monitor.
     * If it is empty, then no vlan has
     * to be monitored.
     *
     * @return monitored vlan
     */
    List<VlanId> monitorVlans();

    /**
     * Returns mirror port.
     * If it is not set, then no destination
     * port for mirrored packets.
     *
     * @return mirror port
     */
    Optional<String> mirrorPort();

    /**
     * Returns mirror vlan.
     * If it is not set then no destination
     * vlan for mirrored packets.
     *
     * @return mirror vlan
     */
    Optional<VlanId> mirrorVlan();

}
