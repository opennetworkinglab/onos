/*
 * Copyright 2018-present Open Networking Foundation
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
package org.onosproject.workflow.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.onosproject.core.ApplicationId;
import org.onosproject.net.config.Config;
import org.onosproject.workflow.api.DefaultRpcDescription;
import org.onosproject.workflow.api.RpcDescription;
import org.onosproject.workflow.api.WorkflowException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;


public class WorkflowNetConfig extends Config<ApplicationId> {

    private static final Logger log = LoggerFactory.getLogger(WorkflowNetConfig.class);

    /**
     * Workflow RPC pointer.
     */
    private static final String RPC_PTR = "/rpc";

    public Collection<RpcDescription> getRpcDescriptions() throws WorkflowException {

        JsonNode node = object.at(RPC_PTR);
        if (!(node instanceof ArrayNode)) {
            throw new WorkflowException("invalid rpc for " + object);
        }
        ArrayNode rpcArrayNode = (ArrayNode) node;

        List<RpcDescription> rpcDescriptions = new ArrayList<>();
        for (JsonNode rpcNode : rpcArrayNode) {
            rpcDescriptions.add(DefaultRpcDescription.valueOf(rpcNode));
        }

        return rpcDescriptions;
    }
}
