/*
 * Copyright 2015 Open Networking Laboratory
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

package org.onosproject.bgp.controller.impl;

import org.jboss.netty.handler.timeout.IdleStateAwareChannelHandler;

/**
 * Channel handler deals with the bgp peer connection and dispatches messages from peer to the appropriate locations.
 */
class BGPChannelHandler extends IdleStateAwareChannelHandler {

    // TODO: implement FSM and session handling mechanism
    /**
     * Create a new unconnected BGPChannelHandler.
     *
     * @param bgpCtrlImpl bgp controller implementation object
     */
    BGPChannelHandler(BGPControllerImpl bgpCtrlImpl) {
    }
}