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

package org.onosproject.pce.pceservice;

import org.onosproject.incubator.net.tunnel.Tunnel;
import org.onosproject.incubator.net.tunnel.TunnelId;
import org.onosproject.net.intent.Constraint;

import java.util.Collection;
import java.util.List;

/**
 * Abstraction of an entity which provides functionalities of pce path.
 */
public interface PcePath {

    /**
     * Returns the attribute path id.
     *
     * @return path id
     */
    TunnelId id();

    /**
     * Sets the attribute path id.
     *
     * @param id path id
     */
    void id(TunnelId id);

    /**
     * Returns the attribute ingress.
     *
     * @return source
     */
    String source();

    /**
     * Sets the attribute ingress.
     *
     * @param src pce source
     */
    void source(String src);

    /**
     * Returns the attribute egress.
     *
     * @return destination
     */
    String destination();

    /**
     * Sets the attribute egress.
     *
     * @param dst pce destination.
     */
    void destination(String dst);

    /**
     * Returns the attribute lspType.
     *
     * @return lspType
     */
    LspType lspType();

    /**
     * Returns the attribute symbolic-path-name.
     *
     * @return symbolic-path-name
     */
    String name();

    /**
     * Returns the attribute cost constraint.
     *
     * @return cost constraint
     */
    Constraint costConstraint();

    /**
     * Returns the attribute bandwidth constraint.
     *
     * @return bandwidth constraint
     */
    Constraint bandwidthConstraint();

    /**
     * Returns the list of explicit path objects.
     *
     * @return list of explicit path objects
     */
    Collection<ExplicitPathInfo> explicitPathInfo();

    /**
     * Copies only non-null or non-zero member variables.
     *
     * @param id path-id
     * @return pce-path
     */
    PcePath copy(PcePath id);

    /**
     * Builder for pce path.
     */
    interface Builder {

        /**
         * Returns the builder object of path id.
         *
         * @param id path id
         * @return builder object of path id
         */
        Builder id(String id);

        /**
         * Returns the builder object of ingress.
         *
         * @param source ingress
         * @return builder object of ingress
         */
        Builder source(String source);

        /**
         * Returns the builder object of egress.
         *
         * @param destination egress
         * @return builder object of egress
         */
        Builder destination(String destination);

        /**
         * Returns the builder object of lspType.
         *
         * @param lspType lsp type
         * @return builder object of lsp type
         */
        Builder lspType(String lspType);

        /**
         * Returns the builder object of symbolic-path-name.
         *
         * @param n symbolic-path-name
         * @return builder object of symbolic-path-name
         */
        Builder name(String n);

        /**
         * Returns the builder object of cost constraint.
         *
         * @param cost constraint
         * @return builder object of cost constraint
         */
        Builder costConstraint(String cost);

        /**
         * Returns the builder object of bandwidth constraint.
         *
         * @param bandwidth constraint
         * @return builder object of bandwidth constraint
         */
        Builder bandwidthConstraint(String bandwidth);

        /**
         * Copies tunnel information to local.
         *
         * @param tunnel pcc tunnel
         * @param explicitPathInfoList list of explicit path objects info
         * @return object of pce-path
         */
        Builder of(Tunnel tunnel, List<ExplicitPathInfo> explicitPathInfoList);

        /**
         * Returns the builder object of ExplicitPathInfo.
         *
         * @param explicitPathInfo list of explicit path obj
         * @return builder object of ExplicitPathInfo
         */
        Builder explicitPathInfo(Collection<ExplicitPathInfo> explicitPathInfo);

        /**
         * Builds object of pce path.
         *
         * @return object of pce path.
         */
        PcePath build();
    }
}
