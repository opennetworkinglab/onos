/*
 * Copyright 2015-present Open Networking Laboratory
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

import org.onosproject.incubator.net.tunnel.Tunnel;
import org.onosproject.pcepio.protocol.PcepMessage;
/**
 * Notifies providers about PCEP message events.
 */
public interface PcepEventListener {

    /**
     * Handles the message event.
     *
     * @param pccId id of the pcc
     * @param msg the message
     */
    void handleMessage(PccId pccId, PcepMessage msg);

    /**
     * Handles end of LSPDB sync actions.
     *
     * @param tunnel the tunnel on which action needs to be taken
     * @param endOfSyncAction the action that needs to be taken for the tunnel
     */
    void handleEndOfSyncAction(Tunnel tunnel, PcepLspSyncAction endOfSyncAction);
}
