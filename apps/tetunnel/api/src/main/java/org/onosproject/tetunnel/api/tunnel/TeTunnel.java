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

import org.onosproject.tetopology.management.api.node.TeNodeKey;
import org.onosproject.tetopology.management.api.node.TtpKey;
import org.onosproject.tetunnel.api.tunnel.path.TePath;

import java.util.List;

/**
 * Representation of a TE tunnel attributes.
 */
public interface TeTunnel {

    /**
     * TE tunnel types.
     */
    enum Type {
        /**
         * Designates TE point-to-point tunnel.
         */
        P2P,
        /**
         * Designates TE point-to-multipoint tunnel.
         */
        P2MP,
        /**
         * Designates RSVP-TE path signaling tunnel.
         */
        PATH_SIGNALING_RSVPTE,
        /**
         * Designates Segment-routing path signaling tunnel.
         */
        PATH_SIGNALING_SR
    }

    /**
     * LSP protection types.
     */
    enum LspProtectionType {
        /**
         * Designates LSP protection "Unprotected".
         */
        LSP_PROT_UNPROTECTED,
        /**
         * Designates LSP protection "Rerouting without Extra-Traffic".
         */
        LSP_PROT_REROUTE,
        /**
         * Designates LSP protection "(Full) Rerouting".
         */
        LSP_PROT_REROUTE_EXTRA,
        /**
         * Designates LSP protection "1+1 Unidirectional Protection".
         */
        LSP_PROT_UNIDIR_1_TO_1,
        /**
         * Designates LSP protection "1+1 Bidirectional Protection".
         */
        LSP_PROT_BIDIR_1_TO_1,
        /**
         * Designates LSP protection "1:N Protection with Extra-Traffic".
         */
        LSP_PROT_1_FOR_N
    }

    /**
     * TE Tunnel state.
     */
    enum State {
        /**
         * Designates the tunnel is down (non-operational).
         */
        DOWN,
        /**
         * Designates the tunnel is up.
         */
        UP
    }

    /**
     * Returns the TE tunnel key.
     *
     * @return TE tunnel key
     */
    TeTunnelKey teTunnelKey();

    /**
     * Returns the name of the TE tunnel.
     *
     * @return name of the TE tunnel
     */
    String name();

    /**
     * Returns the type of the TE tunnel.
     *
     * @return type of the TE tunnel
     */
    Type type();

    /**
     * Returns the key of source TE node of this TE tunnel.
     *
     * @return key of the source TE node
     */
    TeNodeKey srcNode();

    /**
     * Returns key of the source TE termination point of this tunnel.
     *
     * @return key of the source TE termination point
     */
    TtpKey srcTp();

    /**
     * Returns key of the destination TE node of this TE tunnel.
     *
     * @return key of the destination TE node
     */
    TeNodeKey dstNode();

    /**
     * Returns key of the destination TE termination point of this TE tunnel.
     *
     * @return key of the destination TE termination point
     */
    TtpKey dstTp();

    /**
     * Returns the TE LSP protection type of the TE tunnel.
     *
     * @return TE LSP protection type of the TE tunnel
     */
    LspProtectionType lspProtectionType();

    /**
     * Returns the TE tunnel administrative state.
     *
     * @return TE tunnel administrative state
     */
    State adminStatus();

    /**
     * Returns primary paths of this TE tunnel.
     *
     * @return a list of TE paths
     */
    List<TePath> primaryPaths();

    /**
     * Returns segment tunnels of this (E2E cross-domain) tunnel.
     *
     * @return a list of keys of the segment tunnels
     */
    List<TeTunnelKey> segmentTunnels();

    /**
     * Sets segment tunnels of this (E2E cross-domain) tunnel.
     *
     * @param segmentTunnels a list of keys of segment tunnels
     */
    void segmentTunnels(List<TeTunnelKey> segmentTunnels);

    /**
     * Returns key of the E2E tunnel of this (segment) tunnel.
     *
     * @return key of the corresponding E2E TE tunnel
     */
    TeTunnelKey e2eTunnelKey();

    /**
     * Sets the E2E tunnel of this (segment) tunnel.
     *
     * @param e2eTunnelKey key of the corresponding E2E tunnel
     */
    void e2eTunnelKey(TeTunnelKey e2eTunnelKey);

    //TODO: add more required TE attributes
}
