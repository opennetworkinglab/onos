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

package org.onosproject.tetunnel.api.lsp;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import org.onosproject.tetopology.management.api.node.TeNodeKey;
import org.onosproject.tetopology.management.api.node.TtpKey;
import org.onosproject.tetunnel.api.tunnel.TeTunnel;
import org.onosproject.tetunnel.api.tunnel.TeTunnelKey;
import org.onosproject.tetunnel.api.tunnel.path.TeRouteSubobject;

import java.util.List;

/**
 * Default implementation of TE LSP.
 */
public class DefaultTeLsp implements TeLsp {

    private final TeLspKey teLspKey;
    private final TeNodeKey srcNode;
    private final TeNodeKey dstNode;
    private final TtpKey srcTp;
    private final TtpKey dstTp;
    private final TeTunnelKey teTunnelKey;
    private final TeTunnel.Type tunnelType;
    private final TeTunnel.State operStatus;
    private final LspProtectionRole lspProtectionRole;
    private final OriginType originType;
    private final List<TeRouteSubobject> lspRecordRoutes;

    /**
     * Creates an instance of default TE LSP with supplied information.
     *
     * @param teLspKey TE LSP key
     * @param srcNode source TE node key
     * @param dstNode destination TE node key
     * @param srcTp source TE termination point key
     * @param dstTp destination TE termination point key
     * @param teTunnelKey TE tunnel key
     * @param tunnelType TE tunnel type
     * @param operStatus operational status
     * @param lspProtectionRole protection type
     * @param originType origin type
     * @param lspRecordRoutes route of the LSP
     */
    protected DefaultTeLsp(TeLspKey teLspKey, TeNodeKey srcNode, TeNodeKey dstNode,
                        TtpKey srcTp, TtpKey dstTp, TeTunnelKey teTunnelKey,
                        TeTunnel.Type tunnelType, TeTunnel.State operStatus,
                        LspProtectionRole lspProtectionRole,
                        OriginType originType,
                        List<TeRouteSubobject> lspRecordRoutes) {
        this.srcNode = srcNode;
        this.dstNode = dstNode;
        this.srcTp = srcTp;
        this.dstTp = dstTp;
        this.teTunnelKey = teTunnelKey;
        this.tunnelType = tunnelType;
        this.operStatus = operStatus;
        this.lspProtectionRole = lspProtectionRole;
        this.originType = originType;
        this.lspRecordRoutes = Lists.newArrayList(lspRecordRoutes);
        this.teLspKey = teLspKey;
    }

    @Override
    public TeLspKey teLspKey() {
        return teLspKey;
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
    public TtpKey srcTp() {
        return srcTp;
    }

    @Override
    public TtpKey dstTp() {
        return dstTp;
    }

    @Override
    public TeTunnelKey teTunnelKey() {
        return teTunnelKey;
    }

    @Override
    public TeTunnel.Type tunnelType() {
        return tunnelType;
    }

    @Override
    public TeTunnel.State operStatus() {
        return operStatus;
    }

    @Override
    public LspProtectionRole lspProtectionRole() {
        return lspProtectionRole;
    }

    @Override
    public OriginType originType() {
        return originType;
    }

    @Override
    public List<TeRouteSubobject> lspRecordRoutes() {
        return ImmutableList.copyOf(lspRecordRoutes);
    }


    /**
     * Creates a new default TE LSP builder.
     *
     * @return default builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for default TE LSP objects.
     */
    public static class Builder {

        private TeLspKey teLspKey = null;
        private TeNodeKey srcNode = null;
        private TeNodeKey dstNode = null;
        private TtpKey srcTp = null;
        private TtpKey dstTp = null;
        private TeTunnelKey teTunnelKey = null;
        private TeTunnel.Type tunnelType = null;
        private TeTunnel.State operStatus = null;
        private LspProtectionRole lspProtectionRole = null;
        private OriginType originType = null;
        private List<TeRouteSubobject> lspRecordRoutes = Lists.newArrayList();

        /**
         * Builds a default TE LSP object from the accumulated parameters.
         *
         * @return default TE LSP object
         */
        public DefaultTeLsp build() {
            return new DefaultTeLsp(teLspKey, srcNode, dstNode, srcTp, dstTp,
                                    teTunnelKey, tunnelType, operStatus,
                                    lspProtectionRole, originType,
                                    lspRecordRoutes);
        }

        /**
         * Sets TE LSP key to be used by this builder.
         *
         * @param teLspKey TE LSP key
         * @return self
         */
        public Builder teLspKey(TeLspKey teLspKey) {
            this.teLspKey = teLspKey;
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
         * Sets destination termination point key to be used by this builder.
         *
         * @param dstTp destination termination point key
         * @return self
         */
        public Builder dstTp(TtpKey dstTp) {
            this.dstTp = dstTp;
            return this;
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
         * Sets TE tunnel type to be used by this builder.
         *
         * @param tunnelType TE tunnel type
         * @return self
         */
        public Builder tunnelType(TeTunnel.Type tunnelType) {
            this.tunnelType = tunnelType;
            return this;
        }

        /**
         * Sets LSP operational status to be used by this builder.
         *
         * @param operStatus LSP operational status
         * @return self
         */
        public Builder operStatus(TeTunnel.State operStatus) {
            this.operStatus = operStatus;
            return this;
        }

        /**
         * Sets LSP protection role to be used by this builder.
         *
         * @param lspProtectionRole LSP protection role
         * @return self
         */
        public Builder lspProtectionRole(LspProtectionRole lspProtectionRole) {
            this.lspProtectionRole = lspProtectionRole;
            return this;
        }

        /**
         * Sets LSP origin type to be used by this builder.
         *
         * @param originType LSP origin type
         * @return self
         */
        public Builder originType(OriginType originType) {
            this.originType = originType;
            return this;
        }

        /**
         * Sets LSP record routes to be used by this builder.
         *
         * @param lspRecordRoutes LSP record routes
         * @return self
         */
        public Builder lspRecordRoutes(List<TeRouteSubobject> lspRecordRoutes) {
            if (lspRecordRoutes != null) {
                this.lspRecordRoutes = lspRecordRoutes;
            }
            return this;
        }
    }
}
