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

/**
 * Representation of actions to be taken for LSPs on end of LSP-DB sync.
 */
public enum PcepLspSyncAction {

    /**
     * Specifies that delete message for PCE intiiated tunnel should be sent.
     */
    SEND_DELETE(0),

    /**
     * Specifies that update message should be sent.
     */
    SEND_UPDATE(1),

    /**
     * Specifies that the tunnel should be removed from PCE.
     */
    REMOVE(2),

    /**
     * Specifies that the status of the tunnel should be set as unstable.
     */
    UNSTABLE(3);

    int value;

    /**
     * Assigns val with the value for actions to be taken for LSPs on end of LSP-DB sync.
     *
     * @param val sync status
     */
    PcepLspSyncAction(int val) {
        value = val;
    }
}
