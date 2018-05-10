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

package org.onosproject.ovsdb.controller;

import com.google.common.collect.Maps;
import org.onlab.packet.VlanId;
import org.onosproject.net.DefaultAnnotations;
import org.onosproject.net.behaviour.MirroringDescription;
import org.onosproject.ovsdb.rfc.notation.Uuid;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static com.google.common.base.MoreObjects.toStringHelper;

/**
 * The class representing an OVSDB mirror.
 * This class is immutable.
 */
public final class OvsdbMirror {

    private final String mirroringName;
    private boolean selectAll;
    private final Set<Uuid> monitorSrcPorts;
    private final Set<Uuid> monitorDstPorts;
    private final Set<Short> monitorVlans;
    private final Optional<Uuid> mirrorPort;
    private final Optional<Short> mirrorVlan;
    private Map<String, String> externalIds;

    /**
     * Creates an OvsdbMirror using the given inputs.
     *
     * @param mirroringName the name of the mirroring
     * @param selectAll mirrors all ports
     * @param monitorSrcPorts the monitored src ports
     * @param monitorDstPorts the monitored dst ports
     * @param monitorVlans the monitored vlans
     * @param mirrorPort the mirror port
     * @param mirrorVlan the mirror vlan
     * @param externalIds optional key/value options
     */
    private OvsdbMirror(String mirroringName, boolean selectAll, Set<Uuid> monitorSrcPorts, Set<Uuid> monitorDstPorts,
                        Set<Short> monitorVlans, Optional<Uuid> mirrorPort, Optional<Short> mirrorVlan,
                        Map<String, String> externalIds) {

        this.mirroringName = mirroringName;
        this.selectAll = selectAll;
        this.monitorSrcPorts = monitorSrcPorts;
        this.monitorDstPorts = monitorDstPorts;
        this.monitorVlans = monitorVlans;
        this.mirrorPort = mirrorPort;
        this.mirrorVlan = mirrorVlan;
        this.externalIds = externalIds;

    }

    /**
     * Returns the name of the mirroring.
     *
     * @return the string representing the name
     */
    public String mirroringName() {
        return mirroringName;
    }

    /**
     * Returns selectAll value.
     *
     * @return mirrors all ports if true
     */
    public boolean selectAll() {
        return selectAll;
    }

    /**
     * Returns the monitored src ports.
     *
     * @return the uuids set of the ports
     */
    public Set<Uuid> monitorSrcPorts() {
        return monitorSrcPorts;
    }

    /**
     * Returns the monitored dst ports.
     *
     * @return the uuids set of the ports
     */
    public Set<Uuid> monitorDstPorts() {
        return monitorDstPorts;
    }

    /**
     * Returns the monitored vlans.
     *
     * @return the vlans set
     */
    public Set<Short> monitorVlans() {
        return monitorVlans;
    }

    /**
     * Returns the mirror port.
     *
     * @return the uuid port if present, otherwise null
     */
    public Uuid mirrorPort() {
        return mirrorPort.orElse(null);
    }

    /**
     * Returns the mirror vlan.
     *
     * @return the vlan id if present, otherwise null
     */
    public Short mirrorVlan() {
        return mirrorVlan.orElse(null);
    }

    /**
     * Returns the optional external ids.
     *
     * @return the external ids.
     */
    public Map<String, String> externalIds() {
        return externalIds;
    }

    @Override
    public int hashCode() {
        return Objects.hash(mirroringName, selectAll, monitorSrcPorts, monitorDstPorts,
                            monitorVlans, mirrorPort, mirrorVlan, externalIds);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof OvsdbMirror) {
            final OvsdbMirror other = (OvsdbMirror) obj;
            return Objects.equals(this.mirroringName, other.mirroringName) &&
                    Objects.equals(this.selectAll, other.selectAll) &&
                    Objects.equals(this.monitorSrcPorts, other.monitorSrcPorts) &&
                    Objects.equals(this.monitorDstPorts, other.monitorDstPorts) &&
                    Objects.equals(this.monitorVlans, other.monitorVlans) &&
                    Objects.equals(this.mirrorPort, other.mirrorPort) &&
                    Objects.equals(this.mirrorVlan, other.mirrorVlan) &&
                    Objects.equals(this.externalIds, other.externalIds);
        }
        return false;
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("mirroringName", mirroringName())
                .add("selectAll", selectAll())
                .add("monitorSrcPorts", monitorSrcPorts())
                .add("monitorDstPorts", monitorDstPorts())
                .add("monitorVlans", monitorVlans())
                .add("mirrorPort", mirrorPort())
                .add("mirrorVlan", mirrorVlan())
                .add("externalIds", externalIds())
                .toString();
    }

    /**
     * Returns new OVSDB mirror builder.
     *
     * @return ovsdb mirror builder
     */
    public static OvsdbMirror.Builder builder() {
        return new OvsdbMirror.Builder();
    }

    /**
     * Returns new OVSDB mirror builder with mirror description.
     *
     * @param mirrorDesc mirror description
     * @return ovsdb mirror builder
     */
    public static OvsdbMirror.Builder builder(MirroringDescription mirrorDesc) {
        return new OvsdbMirror.Builder(mirrorDesc);
    }

    /**
     * Builder of OVSDB mirror entities.
     */
    public static final class Builder {

        private String mirroringName;
        private boolean selectAll;
        private Set<Uuid> monitorSrcPorts;
        private Set<Uuid> monitorDstPorts;
        private Set<Short> monitorVlans;
        private Optional<Uuid> mirrorPort;
        private Optional<Short> mirrorVlan;
        private Map<String, String> externalIds = Maps.newHashMap();

        /**
         * Constructs an empty builder.
         */
        private Builder() {

        }

        /**
         * Constructs a builder with a given mirror description.
         *
         * @param mirrorDesc mirror description
         */
        private Builder(MirroringDescription mirrorDesc) {

            mirroringName = mirrorDesc.name().name();
            selectAll = false;
            monitorSrcPorts = mirrorDesc.monitorSrcPorts().parallelStream()
                    .map(Uuid::uuid)
                    .collect(Collectors.toSet());
            monitorDstPorts = mirrorDesc.monitorDstPorts().parallelStream()
                    .map(Uuid::uuid)
                    .collect(Collectors.toSet());
            monitorVlans = mirrorDesc.monitorVlans().parallelStream()
                    .map(VlanId::toShort)
                    .collect(Collectors.toSet());

            if (mirrorDesc.mirrorPort().isPresent()) {
                mirrorPort = Optional.of(Uuid.uuid(mirrorDesc.mirrorPort().get()));
            } else {
                mirrorPort = Optional.empty();
            }

            if (mirrorDesc.mirrorVlan().isPresent()) {
                mirrorVlan =  Optional.of(mirrorDesc.mirrorVlan().get().toShort());
            } else {
                mirrorVlan = Optional.empty();
            }

            externalIds.putAll(((DefaultAnnotations) mirrorDesc.annotations()).asMap());

        }

        /**
         * Returns new OVSDB mirror.
         *
         * @return ovsdb mirror
         */
        public OvsdbMirror build() {
            return new OvsdbMirror(mirroringName, selectAll, monitorSrcPorts, monitorDstPorts, monitorVlans,
                                   mirrorPort, mirrorVlan, externalIds);
        }

        /**
         * Returns OVSDB mirror builder with a given name.
         *
         * @param name name of the mirror
         * @return ovsdb interface builder
         */
        public OvsdbMirror.Builder mirroringName(String name) {
            this.mirroringName = name;
            return this;
        }

        /**
         * Returns OVSDB mirror builder with select all.
         *
         * @param all mirrors all ports
         * @return ovsdb interface builder
         */
        public OvsdbMirror.Builder selectAll(boolean all) {
            this.selectAll = all;
            return this;
        }

        /**
         * Returns OVSDB mirror builder with a given set
         * of monitored src ports.
         *
         * @param monitorPorts ports to be monitored
         * @return ovsdb mirror builder
         */
        public OvsdbMirror.Builder monitorSrcPorts(Set<Uuid> monitorPorts) {
            this.monitorSrcPorts = monitorPorts;
            return this;
        }

        /**
         * Returns OVSDB mirror builder with a given set
         * of monitored dst ports.
         *
         * @param monitorPorts ports to be monitored
         * @return ovsdb mirror builder
         */
        public OvsdbMirror.Builder monitorDstPorts(Set<Uuid> monitorPorts) {
            this.monitorDstPorts = monitorPorts;
            return this;
        }

        /**
         * Returns OVSDB mirror builder with a given set
         * of monitored vlans.
         *
         * @param vlans vlans to be monitored
         * @return ovsdb mirror builder
         */
        public OvsdbMirror.Builder monitorVlans(Set<Short> vlans) {
            this.monitorVlans = vlans;
            return this;
        }

        /**
         * Returns OVSDB mirror builder with a given mirror port.
         *
         * @param port the mirror port
         * @return ovsdb mirror builder
         */
        public OvsdbMirror.Builder mirrorPort(Uuid port) {
            this.mirrorPort = Optional.ofNullable(port);
            return this;
        }

        /**
         * Returns OVSDB mirror builder with a given mirror vlan.
         *
         * @param vlan the mirror vlan
         * @return ovsdb mirror builder
         */
        public OvsdbMirror.Builder mirrorVlan(Short vlan) {
            this.mirrorVlan = Optional.ofNullable(vlan);
            return this;
        }

        /**
         * Returns OVSDB mirror builder with given external ids.
         *
         * @param ids the external ids
         * @return ovsdb mirror builder
         */
        public OvsdbMirror.Builder externalIds(Map<String, String> ids) {
            this.externalIds = ids;
            return this;
        }

    }

}
