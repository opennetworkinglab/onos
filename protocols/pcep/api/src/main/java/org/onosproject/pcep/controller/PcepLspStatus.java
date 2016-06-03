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
package org.onosproject.pcep.controller;

import org.onosproject.incubator.net.tunnel.Tunnel.State;

/**
 * Representation of the PCEP LSP state.
 */
public enum PcepLspStatus {

    /**
     * Signifies that the LSP is not active.
     */
    DOWN,

    /**
     * Signifies that the LSP is signalled.
     */
    UP,

    /**
     * Signifies that the LSP is up and carrying traffic.
     */
    ACTIVE,

    /**
     * Signifies that the LSP is being torn down, resources are being released.
     */
    GOING_DOWN,

    /**
     * Signifies that the LSP is being signalled.
     */
    GOING_UP;

    /**
     * Returns the applicable PCEP LSP status corresponding to ONOS tunnel state.
     *
     * @param tunnelState ONOS tunnel state
     * @return LSP status as per protocol
     */
    public static PcepLspStatus getLspStatusFromTunnelStatus(State tunnelState) {

        switch (tunnelState) {

        case INIT:
            return PcepLspStatus.DOWN;

        case ESTABLISHED:
            return PcepLspStatus.GOING_UP;

        case ACTIVE:
            return PcepLspStatus.UP;

        case FAILED: // fall through
        case INACTIVE: // LSP is administratively down.
        default:
            return PcepLspStatus.DOWN;
        }
    }

    /**
     * Returns the applicable ONOS tunnel state corresponding to PCEP LSP status.
     *
     * @param lspState PCEP LSP status
     * @return tunnel state
     */
    public static State getTunnelStatusFromLspStatus(PcepLspStatus lspState) {

        switch (lspState) {

        case DOWN:
            return State.FAILED;

        case UP: // fall through
        case ACTIVE:
            return State.ACTIVE;

        case GOING_DOWN:
            return State.FAILED;

        case GOING_UP:
            return State.ESTABLISHED;

        default:
            return State.FAILED;
        }
    }
}
