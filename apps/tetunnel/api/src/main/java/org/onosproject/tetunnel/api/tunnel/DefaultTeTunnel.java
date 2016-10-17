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

package org.onosproject.tetunnel.api.tunnel;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import org.onosproject.tetopology.management.api.node.TeNodeKey;
import org.onosproject.tetopology.management.api.node.TtpKey;
import org.onosproject.tetunnel.api.tunnel.path.TePath;

import java.util.List;

/**
 * Default TE tunnel implementation.
 */
public class DefaultTeTunnel implements TeTunnel {

    private final TeTunnelKey teTunnelKey;
    private final String name;
    private final Type type;
    private final LspProtectionType lspProtectionType;
    private final State adminState;
    private final TeNodeKey srcNode;
    private final TeNodeKey dstNode;
    private final TtpKey srcTp;
    private final TtpKey dstTp;
    private final List<TePath> primaryPaths;

    private List<TeTunnelKey> segmentTunnels = null;
    private TeTunnelKey e2eTunnel = null;

    /**
     * Creates a TE tunnel with supplied information.
     *
     * @param teTunnelKey TE tunnel key
     * @param name TE tunnel name
     * @param type TE tunnel type
     * @param lspProtectionType LSP protection type of the TE tunnel
     * @param adminState TE tunnel administrative state
     * @param srcNode source TE node
     * @param dstNode destination TE node
     * @param srcTp source termination point
     * @param dstTp destination termination point
     * @param primaryPaths primary paths
     */
    protected DefaultTeTunnel(TeTunnelKey teTunnelKey, String name, Type type,
                           LspProtectionType lspProtectionType,
                              State adminState, TeNodeKey srcNode,
                           TeNodeKey dstNode, TtpKey srcTp, TtpKey dstTp,
                           List<TePath> primaryPaths) {
        this.teTunnelKey = teTunnelKey;
        this.name = name;
        this.type = type;
        this.lspProtectionType = lspProtectionType;
        this.adminState = adminState;
        this.srcNode = srcNode;
        this.dstNode = dstNode;
        this.srcTp = srcTp;
        this.dstTp = dstTp;
        this.primaryPaths = Lists.newArrayList(primaryPaths);
    }

    @Override
    public TeTunnelKey teTunnelKey() {
        return teTunnelKey;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public Type type() {
        return type;
    }

    @Override
    public LspProtectionType lspProtectionType() {
        return lspProtectionType;
    }

    @Override
    public State adminStatus() {
        return adminState;
    }

    @Override
    public TeNodeKey srcNode() {
        return srcNode;
    }

    @Override
    public TeNodeKey dstNode() {
        return dstNode;
    }

    @Override
    public List<TePath> primaryPaths() {
        return ImmutableList.copyOf(primaryPaths);
    }

    @Override
    public List<TeTunnelKey> segmentTunnels() {
        return ImmutableList.copyOf(segmentTunnels);
    }

    @Override
    public void segmentTunnels(List<TeTunnelKey> segmentTunnels) {
        this.segmentTunnels = Lists.newArrayList(segmentTunnels);
    }

    @Override
    public TeTunnelKey e2eTunnelKey() {
        return e2eTunnel;
    }

    @Override
    public void e2eTunnelKey(TeTunnelKey e2eTunnelKey) {
        this.e2eTunnel = e2eTunnelKey;
    }

    @Override
    public TtpKey srcTp() {
        return srcTp;
    }

    @Override
    public TtpKey dstTp() {
        return dstTp;
    }


    /**
     * Creates a new default TE tunnel builder.
     *
     * @return default builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for default TE tunnel objects.
     */
    public static class Builder {

        private TeTunnelKey teTunnelKey = null;
        private String name = "";
        private Type type = null;
        private LspProtectionType lspProtectionType = null;
        private State adminState = State.UP;
        private TeNodeKey srcNode = null;
        private TeNodeKey dstNode = null;
        private TtpKey srcTp = null;
        private TtpKey dstTp = null;
        private List<TePath> primaryPaths = Lists.newArrayList();

        /**
         * Builds a default TE tunnel object from the accumulated parameters.
         *
         * @return default TE tunnel object
         */
        public DefaultTeTunnel build() {
            return new DefaultTeTunnel(teTunnelKey, name, type,
                                       lspProtectionType,
                                       adminState, srcNode, dstNode,
                                       srcTp, dstTp, primaryPaths);
        }

        /**
         * Sets TE tunnel key to be used by this builder.
         *
         * @param teTunnelKey TE tunnel key
         * @return self
         */
        public Builder teTunnelKey(TeTunnelKey teTunnelKey) {
            this.teTunnelKey = teTunnelKey;
            return this;
        }

        /**
         * Sets TE tunnel name to be used by this builder.
         *
         * @param name TE tunnel name
         * @return self
         */
        public Builder name(String name) {
            this.name = name;
            return this;
        }

        /**
         * Sets TE tunnel type to be used by this builder.
         *
         * @param type TE tunnel type
         * @return self
         */
        public Builder type(Type type) {
            this.type = type;
            return this;
        }

        /**
         * Sets tunnel LSP protection type to be used by this builder.
         *
         * @param lspProtectionType protection type
         * @return self
         */
        public Builder lspProtectionType(LspProtectionType lspProtectionType) {
            this.lspProtectionType = lspProtectionType;
            return this;
        }

        /**
         * Sets administrative state to be used by this builder.
         *
         * @param adminState administrative state
         * @return self
         */
        public Builder adminState(State adminState) {
            this.adminState = adminState;
            return this;
        }

        /**
         * Sets source node key to be used by this builder.
         *
         * @param srcNode source node key
         * @return self
         */
        public Builder srcNode(TeNodeKey srcNode) {
            this.srcNode = srcNode;
            return this;
        }

        /**
         * Sets destination node key to be used by this builder.
         *
         * @param dstNode destination node key
         * @return self
         */
        public Builder dstNode(TeNodeKey dstNode) {
            this.dstNode = dstNode;
            return this;
        }

        /**
         * Sets source termination point key to be used by this builder.
         *
         * @param srcTp source termination point key
         * @return self
         */
        public Builder srcTp(TtpKey srcTp) {
            this.srcTp = srcTp;
            return this;
        }

        /**
         * Sets destination point key to be used by this builder.
         *
         * @param dstTp destination point key
         * @return self
         */
        public Builder dstTp(TtpKey dstTp) {
            this.dstTp = dstTp;
            return this;
        }

        /**
         * Sets primary paths to be used by this builder.
         *
         * @param primaryPaths list of TePath
         * @return self
         */
        public Builder primaryPaths(List<TePath> primaryPaths) {
            if (primaryPaths != null) {
            this.primaryPaths = primaryPaths;
            }
            return this;
        }
    }
}
