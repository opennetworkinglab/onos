/*
 * Copyright 2015-present Open Networking Foundation
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
package org.onosproject.fpm.web;


import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onosproject.codec.CodecContext;
import org.onosproject.codec.JsonCodec;
import org.onosproject.routing.fpm.FpmPeer;

import org.onlab.packet.IpAddress;
import org.onosproject.routing.fpm.FpmPeerAcceptRoutes;

/**
 * Codec of FpmPeerInfo class.
 */
public final class FpmAcceptRoutesCodec extends JsonCodec<FpmPeerAcceptRoutes> {

    // JSON field names
    private static final String PEER_ADDRESS = "peerAddress";
    private static final String PEER_PORT = "peerPort";
    private static final String ACCEPT_ROUTES = "acceptRoutes";

    @Override
    public FpmPeerAcceptRoutes decode(ObjectNode json, CodecContext context) {
        if (json == null || !json.isObject()) {
            return null;
        }

        IpAddress address = IpAddress.valueOf(json.path(PEER_ADDRESS).asText());
        int port = Integer.parseInt(json.path(PEER_PORT).asText());
        boolean isAcceptRoutes = Boolean.valueOf(json.path(ACCEPT_ROUTES).asText());
        FpmPeer peer = new FpmPeer(address, port);
        FpmPeerAcceptRoutes updatedPeer = new FpmPeerAcceptRoutes(peer, isAcceptRoutes);
        return updatedPeer;
    }
}
