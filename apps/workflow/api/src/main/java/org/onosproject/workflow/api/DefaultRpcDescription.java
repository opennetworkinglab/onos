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
package org.onosproject.workflow.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.MissingNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.common.base.MoreObjects;

import static org.onosproject.workflow.api.CheckCondition.check;

/**
 * Class of workflow RPC description.
 */
public final class DefaultRpcDescription implements RpcDescription {

    /**
     * Workflow RPC operation.
     */
    private final String op;

    /**
     * Parameters.
     */
    private final JsonNode params;

    /**
     * Invocation ID.
     */
    private final String id;

    /**
     * Constructor of workplace description.
     * @param builder workplace builder
     */
    private DefaultRpcDescription(Builder builder) {
        this.op = builder.op;
        this.params = builder.params;
        this.id = builder.id;
    }

    @Override
    public String op() {
        return this.op;
    }

    @Override
    public JsonNode params() {
        return this.params;
    }

    @Override
    public String id() {
        return this.id;
    }

    /**
     * Creating workflow RPC description from json tree.
     * @param root root node for workflow RPC description
     * @return workflow RPC description
     * @throws WorkflowException workflow exception
     */
    public static DefaultRpcDescription valueOf(JsonNode root) throws WorkflowException {

        JsonNode node = root.at(RPC_OP_PTR);
        if (!(node instanceof TextNode)) {
            throw new WorkflowException("invalid RPC operation for " + root);
        }
        String rpcOp = node.asText();

        node = root.at(RPC_PARAMS_PTR);
        if (node instanceof MissingNode) {
            throw new WorkflowException("invalid RPC parameters for " + root);
        }
        JsonNode rpcParams = node;

        node = root.at(RPC_ID_PTR);
        if (!(node instanceof TextNode)) {
            throw new WorkflowException("invalid RPC invocation ID for " + root);
        }
        String rpcId = node.asText();


        return builder()
                .setOp(rpcOp)
                .setParams(rpcParams)
                .setId(rpcId)
                .build();
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("op", op())
                .add("params", params())
                .add("id", id())
                .toString();
    }

    /**
     * Gets builder instance.
     * @return builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for workplace RPC description.
     */
    public static class Builder {

        /**
         * Workflow RPC operation.
         */
        private String op;

        /**
         * Parameters.
         */
        private JsonNode params;

        /**
         * Invocation ID.
         */
        private String id;

        /**
         * Sets workflow RPC operation.
         * @param op workflow RPC operation
         * @return builder
         */
        public Builder setOp(String op) {
            this.op = op;
            return this;
        }

        /**
         * Sets workflow RPC parameters.
         * @param params workflow RPC parameters
         * @return builder
         */
        public Builder setParams(JsonNode params) {
            this.params = params;
            return this;
        }

        /**
         * Sets workflow RPC invocation ID.
         * @param id workflow invocation ID
         * @return builder
         */
        public Builder setId(String id) {
            this.id = id;
            return this;
        }

        /**
         * Builds workplace RPC description from builder.
         * @return instance of workflow RPC description
         * @throws WorkflowException workflow exception
         */
        public DefaultRpcDescription build() throws WorkflowException {
            check(op != null, "op is invalid");
            check(params != null, "params is invalid");
            check(id != null, "id is invalid");
            return new DefaultRpcDescription(this);
        }
    }
}
