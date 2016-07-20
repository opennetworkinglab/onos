/*
 * Copyright 2016 Open Networking Laboratory
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

package org.onosproject.pce.pceservice;

import static com.google.common.base.MoreObjects.toStringHelper;

import java.util.Objects;

import org.onlab.util.DataRateUnit;
import org.onosproject.incubator.net.tunnel.Tunnel;
import org.onosproject.incubator.net.tunnel.TunnelId;
import org.onosproject.net.intent.constraint.BandwidthConstraint;
import org.onosproject.net.intent.Constraint;
import org.onosproject.pce.pceservice.constraint.CostConstraint;

/**
 * Implementation of an entity which provides functionalities of pce path.
 */
public final class DefaultPcePath implements PcePath {

    private TunnelId id; // path id
    private String source; // Ingress
    private String destination; // Egress
    private LspType lspType; // LSP type
    private String name; // symbolic-path-name
    private Constraint costConstraint; // cost constraint
    private Constraint bandwidthConstraint; // bandwidth constraint

    /**
     * Initializes PCE path attributes.
     *
     * @param id path id
     * @param src ingress
     * @param dst egress
     * @param lspType LSP type
     * @param name symbolic-path-name
     * @param costConstrnt cost constraint
     * @param bandwidthConstrnt bandwidth constraint
     */
    private DefaultPcePath(TunnelId id, String src, String dst, LspType lspType,
                           String name, Constraint costConstrnt, Constraint bandwidthConstrnt) {

        this.id = id;
        this.source = src;
        this.destination = dst;
        this.lspType = lspType;
        this.name = name;
        this.costConstraint = costConstrnt;
        this.bandwidthConstraint = bandwidthConstrnt;
    }

    @Override
    public TunnelId id() {
        return id;
    }

    @Override
    public void id(TunnelId id) {
        this.id = id;
    }

    @Override
    public String source() {
        return source;
    }

    @Override
    public void source(String src) {
        this.source = src;
    }

    @Override
    public String destination() {
        return destination;
    }

    @Override
    public void destination(String dst) {
        this.destination = dst;
    }

    @Override
    public LspType lspType() {
        return lspType;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public Constraint costConstraint() {
        return costConstraint;
    }

    @Override
    public Constraint bandwidthConstraint() {
        return bandwidthConstraint;
    }

    @Override
    public PcePath copy(PcePath path) {
        if (null != path.source()) {
            this.source = path.source();
        }
        if (null != path.destination()) {
            this.destination = path.destination();
        }

        this.lspType = path.lspType();

        if (null != path.name()) {
            this.name = path.name();
        }
        if (null != path.costConstraint()) {
            this.costConstraint = path.costConstraint();
        }
        if (null != path.bandwidthConstraint()) {
            this.bandwidthConstraint = path.bandwidthConstraint();
        }
        return this;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, source, destination, lspType, name, costConstraint, bandwidthConstraint);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof DefaultPcePath) {
            DefaultPcePath that = (DefaultPcePath) obj;
            return Objects.equals(id, that.id)
                    && Objects.equals(source, that.source)
                    && Objects.equals(destination, that.destination)
                    && Objects.equals(lspType, that.lspType)
                    && Objects.equals(name, that.name)
                    && Objects.equals(costConstraint, that.costConstraint)
                    && Objects.equals(bandwidthConstraint, that.bandwidthConstraint);
        }
        return false;
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("id", id())
                .add("source", source)
                .add("destination", destination)
                .add("lsptype", lspType)
                .add("name", name)
                .add("costConstraint", costConstraint.toString())
                .add("bandwidthConstraint", bandwidthConstraint.toString())
                .toString();
    }

    /**
     * Creates an instance of the pce path builder.
     *
     * @return instance of builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder class for pce path.
     */
    public static final class Builder implements PcePath.Builder {
        private TunnelId id;
        private String source;
        private String destination;
        private LspType lspType;
        private String name;
        private Constraint costConstraint;
        private Constraint bandwidthConstraint;

        @Override
        public Builder id(String id) {
            this.id = TunnelId.valueOf(id);
            return this;
        }

        @Override
        public Builder source(String source) {
            this.source = source;
            return this;
        }

        @Override
        public Builder destination(String destination) {
            this.destination = destination;
            return this;
        }

        @Override
        public Builder lspType(String type) {
            if (null != type) {
                this.lspType = LspType.values()[Integer.valueOf(type)];
            }
            return this;
        }

        @Override
        public Builder name(String name) {
            this.name = name;
            return this;
        }

        @Override
        public Builder costConstraint(String cost) {
            this.costConstraint = CostConstraint.of(CostConstraint.Type.values()[Integer.valueOf(cost) - 1]);
            return this;
        }

        @Override
        public Builder bandwidthConstraint(String bandwidth) {
            this.bandwidthConstraint = BandwidthConstraint.of(Double.valueOf(bandwidth), DataRateUnit
                    .valueOf("BPS"));
            return this;
        }

        @Override
        public Builder of(Tunnel tunnel) {
            this.id = TunnelId.valueOf(tunnel.tunnelId().id());
            this.source = tunnel.src().toString();
            this.destination = tunnel.dst().toString();
            this.name = tunnel.tunnelName().toString();
            // LSP type
            String lspType = tunnel.annotations().value(PcepAnnotationKeys.LSP_SIG_TYPE);
            if (lspType != null) {
               this.lspType = LspType.values()[Integer.valueOf(lspType) - 1];
            }
            // Cost type
            String costType = tunnel.annotations().value(PcepAnnotationKeys.COST_TYPE);
            if (costType != null) {
                this.costConstraint = CostConstraint.of(CostConstraint.Type.values()[Integer.valueOf(costType) - 1]);
            }
            // Bandwidth
            String bandwidth = tunnel.annotations().value(PcepAnnotationKeys.BANDWIDTH);
            if (bandwidth != null) {
                this.bandwidthConstraint = BandwidthConstraint.of(Double.parseDouble(bandwidth),
                                                                  DataRateUnit.valueOf("BPS"));
            }

            return this;
        }

        @Override
        public PcePath build() {
            return new DefaultPcePath(id, source, destination, lspType, name,
                                      costConstraint, bandwidthConstraint);
        }
    }
}
