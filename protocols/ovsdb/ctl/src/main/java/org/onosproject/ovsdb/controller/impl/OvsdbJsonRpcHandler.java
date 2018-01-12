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
package org.onosproject.ovsdb.controller.impl;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import org.onosproject.ovsdb.controller.OvsdbNodeId;
import org.onosproject.ovsdb.controller.driver.OvsdbProviderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Strings;

/**
 * Channel handler deals with the node connection and dispatches
 * ovsdb messages to the appropriate locations.
 */
public final class OvsdbJsonRpcHandler extends ChannelInboundHandlerAdapter {
    private static final Logger log = LoggerFactory
            .getLogger(OvsdbJsonRpcHandler.class);
    private OvsdbNodeId ovsdbNodeId;
    private OvsdbProviderService ovsdbProviderService;

    /**
     * Constructor from a OvsdbNodeId ovsdbNodeId.
     *
     * @param ovsdbNodeId the ovsdbNodeId to use
     */
    public OvsdbJsonRpcHandler(OvsdbNodeId ovsdbNodeId) {
        super();
        this.ovsdbNodeId = ovsdbNodeId;
    }

    /**
     * Gets the ovsdbProviderService instance.
     *
     * @return the instance of the ovsdbProviderService
     */
    public OvsdbProviderService getOvsdbProviderService() {
        return ovsdbProviderService;
    }

    /**
     * Sets the ovsdbProviderService instance.
     *
     * @param ovsdbNodeDriver the ovsdbNodeDriver to use
     */
    public void setOvsdbProviderService(OvsdbProviderService ovsdbNodeDriver) {
        this.ovsdbProviderService = ovsdbNodeDriver;
    }

    /**
     * Gets the OvsdbNodeId instance.
     *
     * @return the instance of the OvsdbNodeId
     */
    public OvsdbNodeId getNodeId() {
        return ovsdbNodeId;
    }

    /**
     * Sets the ovsdb node id.
     *
     * @param ovsdbNodeId the ovsdbNodeId to use
     */
    public void setNodeId(OvsdbNodeId ovsdbNodeId) {
        this.ovsdbNodeId = ovsdbNodeId;
    }

    /**
     * Processes an JsonNode message received on the channel.
     *
     * @param jsonNode The OvsdbJsonRpcHandler that received the message
     */
    private void processOvsdbMessage(JsonNode jsonNode) {

        log.debug("Handle ovsdb message");

        if (jsonNode.has("result")) {

            log.debug("Handle ovsdb result");
            ovsdbProviderService.processResult(jsonNode);

        } else if (jsonNode.hasNonNull("method")) {

            log.debug("Handle ovsdb request");
            if (jsonNode.has("id")
                    && !Strings.isNullOrEmpty(jsonNode.get("id").asText())) {
                ovsdbProviderService.processRequest(jsonNode);
            }

        }
        return;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg)
            throws Exception {
        log.debug("Receive message from ovsdb");
        if (msg instanceof JsonNode) {
            JsonNode jsonNode = (JsonNode) msg;
            processOvsdbMessage(jsonNode);
        }
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        ctx.flush();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext context, Throwable cause) {
        log.error("Exception inside channel handling pipeline.", cause);
        context.close();
    }
}
