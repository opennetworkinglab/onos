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
import com.google.common.base.MoreObjects;
import org.onlab.packet.VlanId;
import org.onosproject.net.AbstractDescription;
import org.onosproject.net.SparseAnnotations;

import java.util.List;
import java.util.Optional;

/**
 * Default implementation of mirroring description entity.
 */
@Beta
public final class DefaultMirroringDescription extends AbstractDescription
    implements MirroringDescription {

    private final MirroringName mirroringName;
    private final List<String> monitorSrcPorts;
    private final List<String> monitorDstPorts;
    private final List<VlanId> monitorVlans;
    private final Optional<String> mirrorPort;
    private final Optional<VlanId> mirrorVlan;

    /**
     * Creates a mirroring description using the supplied information.
     *
     * @param name the name of the mirroring
     * @param monitorsrcports the monitored src ports
     * @param monitordstports the monitored dst ports
     * @param monitorvlans the monitored vlans
     * @param mirrorport the mirror port
     * @param mirrorvlan the mirror vlan
     * @param annotations optional key/value annotations
     */
    public DefaultMirroringDescription(MirroringName name,
                                       List<String> monitorsrcports,
                                       List<String> monitordstports,
                                       List<VlanId> monitorvlans,
                                       Optional<String> mirrorport,
                                       Optional<VlanId> mirrorvlan,
                                       SparseAnnotations... annotations) {
        super(annotations);
        this.mirroringName = name;
        this.monitorSrcPorts = monitorsrcports;
        this.monitorDstPorts = monitordstports;
        this.monitorVlans = monitorvlans;
        this.mirrorPort = mirrorport;
        this.mirrorVlan = mirrorvlan;
    }


    /**
     * Returns mirroring name.
     *
     * @return mirroring name
     */
    @Override
    public MirroringName name() {
        return mirroringName;
    }

    /**
     * Returns src ports to monitor.
     * If it is empty, then no src port has
     * to be monitored.
     *
     * @return set of src ports to monitor
     */
    @Override
    public List<String> monitorSrcPorts() {
        return monitorSrcPorts;
    }

    /**
     * Returns dst ports to monitor.
     * If it is empty, then no dst port has
     * to be monitored.
     *
     * @return set of dst ports to monitor
     */
    @Override
    public List<String> monitorDstPorts() {
        return monitorDstPorts;
    }

    /**
     * Returns vlans to monitor.
     * If it is empty, then no vlan has
     * to be monitored.
     *
     * @return monitored vlan
     */
    @Override
    public List<VlanId> monitorVlans() {
        return monitorVlans;
    }

    /**
     * Returns mirror port.
     * If it is not set, then no destination
     * port for mirrored packets.
     *
     * @return mirror port
     */
    @Override
    public Optional<String> mirrorPort() {
        return mirrorPort;
    }

    /**
     * Returns mirror vlan.
     * If it is not set the no destination
     * vlan for mirrored packets.
     *
     * @return mirror vlan
     */
    @Override
    public Optional<VlanId> mirrorVlan() {
        return mirrorVlan;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("name", name())
                .add("monitorsrcports", monitorSrcPorts())
                .add("monitordstports", monitorDstPorts())
                .add("monitorvlans", monitorVlans())
                .add("mirrorport", mirrorPort())
                .add("mirrorvlan", mirrorVlan())
                .toString();
    }
}
