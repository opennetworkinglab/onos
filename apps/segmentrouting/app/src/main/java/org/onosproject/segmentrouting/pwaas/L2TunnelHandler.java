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

import org.onosproject.net.Link;
import org.onosproject.net.config.NetworkConfigEvent;

import java.util.List;
import java.util.Set;

public interface L2TunnelHandler {
    void init();

    /**
     * Returns a copy of the l2 policies that exist in the store.
     *
     * @return The l2 policies
     */
    List<L2TunnelPolicy> getL2Policies();

    /**
     * Returns a copy of the l2 tunnels that exist in the store.
     *
     * @return The l2 tunnels.
     */
    List<L2Tunnel> getL2Tunnels();

    /**
     * Processes a link removal. Finds affected pseudowires and rewires them.
     * TODO: Make it also take into account failures of links that are used for pw
     * traffic in the spine.
     * @param link The link that failed
     */
    void processLinkDown(Link link);

    /**
     * Processes Pwaas Config added event.
     *
     * @param event network config add event
     */
    void processPwaasConfigAdded(NetworkConfigEvent event);

    /**
     * Processes PWaaS Config updated event.
     *
     * @param event network config updated event
     */
    void processPwaasConfigUpdated(NetworkConfigEvent event);

    /**
     * Processes Pwaas Config removed event.
     *
     * @param event network config removed event
     */
    void processPwaasConfigRemoved(NetworkConfigEvent event);

    /**
     * Helper function to handle the pw removal.
     * <p>
     * This method should for the mastership of the device because it is
     * used only from network configuration updates, thus we only want
     * one instance only to program each pseudowire.
     *
     * @param pwToRemove the pseudo wires to remove
     */
    void tearDown(Set<L2TunnelDescription> pwToRemove);

    /**
     * Pwaas pipelines.
     */
    enum Pipeline {
        /**
         * The initiation pipeline.
         */
        INITIATION, /**
         * The termination pipeline.
         */
        TERMINATION
    }

    /**
     * Enum helper to carry results of various operations.
     */
    enum Result {
        /**
         * Happy ending scenario.
         */
        SUCCESS(0, "No error occurred"),

        /**
         * We have problems with the supplied parameters.
         */
        WRONG_PARAMETERS(1, "Wrong parameters"),

        /**
         * We have an internal error during the deployment
         * or removal phase.
         */
        INTERNAL_ERROR(3, "Internal error"),

        /**
         *
         */
        REMOVAL_ERROR(5, "Can not remove pseudowire from network configuration"),

        /**
         *
         */
        ADDITION_ERROR(6, "Can not add pseudowire in network configuration"),

        /**
         *
         */
        CONFIG_NOT_FOUND(7, "Can not find configuration class for pseudowires");

        private final int code;
        private final String description;
        protected int nextId;

        Result(int code, String description) {
            this.code = code;
            this.description = description;
        }

        public String getDescription() {
            return description;
        }

        @Override
        public String toString() {
            return code + ": " + description;
        }
    }

    /**
     * Enum helper for handling the direction of the pw.
     */
    enum Direction {
        /**
         * The forward direction of the pseudo wire.
         */
        FWD, /**
         * The reverse direction of the pseudo wire.
         */
        REV
    }
}
