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

import org.onlab.util.Tools;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onosproject.codec.CodecContext;
import org.onosproject.codec.JsonCodec;
import org.onosproject.routing.fpm.FpmPeerInfo;


/**
 * Codec of FpmPeerInfo class.
 */
public final class FpmCodec extends JsonCodec<FpmPeerInfo> {

    // JSON field names
    private static final String PEER_ADDRESS = "peerAddress";
    private static final String PEER_PORT = "peerPort";
    private static final String CONNECTED_TO = "connectedTo";
    private static final String CONNECTION_TIME = "connectionTime";
    private static final String LOCAL_ROUTES = "localRoutes";
    private static final String ACCEPT_ROUTES = "acceptRoutes";


    @Override
    public ObjectNode encode(FpmPeerInfo fpmPeerInfo, CodecContext context) {
        final ObjectNode fpmConnectionArray = context.mapper().createObjectNode();

        ArrayNode connectionArray = context.mapper().createArrayNode();
        fpmPeerInfo.connections().forEach(connection -> {
            ObjectNode fpmNode = context.mapper().createObjectNode();
            fpmNode.put(PEER_ADDRESS, connection.peer().address().toString());
            fpmNode.put(PEER_PORT, connection.peer().port());
            fpmNode.put(CONNECTED_TO, connection.connectedTo().toString());
            fpmNode.put(CONNECTION_TIME, Tools.timeAgo(connection.connectTime()));
            fpmNode.put(LOCAL_ROUTES, fpmPeerInfo.routes());
            fpmNode.put(ACCEPT_ROUTES, connection.isAcceptRoutes());
            connectionArray.add(fpmNode);
        });

        fpmConnectionArray.put("connection", connectionArray);
        return fpmConnectionArray;
    }



}
