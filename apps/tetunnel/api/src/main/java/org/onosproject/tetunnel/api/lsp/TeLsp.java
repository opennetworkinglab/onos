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

import org.onosproject.tetopology.management.api.node.TeNodeKey;
import org.onosproject.tetopology.management.api.node.TtpKey;
import org.onosproject.tetunnel.api.tunnel.TeTunnel;
import org.onosproject.tetunnel.api.tunnel.TeTunnelKey;
import org.onosproject.tetunnel.api.tunnel.path.TeRouteSubobject;

import java.util.List;

/**
 * Representation of a TE LSP.
 */
public interface TeLsp {

    /**
     * Protection roles of TE LSP.
     */
    enum LspProtectionRole {
        /**
         * Designates a working LSP.
         */
        WORKING,
        /**
         * Designates a protection LSP.
         */
        PROTECTION
    }

    /**
     * Origin type of LSP relative to the location of the local switch in the
     * path.
     */
    enum OriginType {
        INGRESS,
        EGRESS,
        TRANSIT
    }

    /**
     * Returns key of this TE LSP.
     *
     * @return key of this TE LSP
     */
    TeLspKey teLspKey();

    /**
     * Returns source TE node of this tunnel.
     *
     * @return source TE node key
     */
    TeNodeKey srcNode();

    /**
     * Returns source TE termination point of this tunnel.
     *
     * @return source TE termination point key
     */
    TtpKey srcTp();

    /**
     * Returns destination TE node of this tunnel.
     *
     * @return destination TE node key
     */
    TeNodeKey dstNode();

    /**
     * Returns destination TE termination point of this tunnel.
     *
     * @return destination TE termination point key
     */
    TtpKey dstTp();

    /**
     * Returns the TE tunnel used in the SESSION that remains constant over
     * the life of the tunnel.
     *
     * @return TE tunnel key
     */
    TeTunnelKey teTunnelKey();

    /**
     * Returns corresponding tunnel type.
     *
     * @return TE tunnel type
     */
    TeTunnel.Type tunnelType();

    /**
     * Returns operational status of the LSP.
     *
     * @return operational status
     */
    TeTunnel.State operStatus();

    /**
     * Returns protection role of the LSP.
     *
     * @return protection role
     */
    LspProtectionRole lspProtectionRole();

    /**
     * Return origin type of the LSP.
     *
     * @return origin type
     */
    OriginType originType();

    /**
     * Returns route of this LSP.
     *
     * @return list of TeRouteSubobject
     */
    List<TeRouteSubobject> lspRecordRoutes();

    //TODO add more attributes here.
}
