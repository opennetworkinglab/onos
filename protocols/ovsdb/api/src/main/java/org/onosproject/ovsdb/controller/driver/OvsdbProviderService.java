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
package org.onosproject.ovsdb.controller.driver;

import io.netty.channel.Channel;

import org.onosproject.ovsdb.rfc.jsonrpc.Callback;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Represents the driver side of an ovsdb node. This interface should never be
 * exposed to consumers.
 */
public interface OvsdbProviderService {
    /**
     * Sets the ovsdb agent to be used. This method can only be called once.
     *
     * @param agent the agent to set.
     */
    void setAgent(OvsdbAgent agent);

    /**
     * Sets the associated Netty channel for this node.
     *
     * @param channel the Netty channel
     */
    void setChannel(Channel channel);

    /**
     * Announces to the ovsdb agent that this node has added.
     */
    void nodeAdded();

    /**
     * Announces to the ovsdb agent that this node has removed.
     */
    void nodeRemoved();

    /**
     * Sets whether the node is connected.
     *
     * @param connected whether the node is connected
     */
    void setConnection(boolean connected);

    /**
     * Processes result from ovsdb.
     *
     * @param response JsonNode response from ovsdb
     */
    void processResult(JsonNode response);

    /**
     * processes request from ovsdb.
     *
     * @param request JsonNode request from ovsdb
     */
    void processRequest(JsonNode request);

    /**
     * Sets call back.
     *
     * @param monitorCallback the callback to set
     */
    void setCallback(Callback monitorCallback);

}
