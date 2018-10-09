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
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.MissingNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.common.base.MoreObjects;

import java.net.URI;

import static org.onosproject.workflow.api.CheckCondition.check;


/**
 * Class for default workflow description.
 */
public final class DefaultWorkflowDescription implements WorkflowDescription {

    /**
     * Workplace Name.
     */
    private String workplaceName;

    /**
     * Workflow ID.
     */
    private URI id;

    /**
     * Workflow data model.
     */
    private JsonNode data;

    /**
     * Constructor of workflow description.
     * @param builder workflow description builder
     */
    private DefaultWorkflowDescription(Builder builder) {
        this.workplaceName = builder.workplaceName;
        this.id = builder.id;
        this.data = builder.data;
    }

    @Override
    public String workplaceName() {
        return this.workplaceName;
    }

    @Override
    public URI id() {
        return this.id;
    }

    @Override
    public String workflowContextName() {
        return DefaultWorkflowContext.nameBuilder(id(), workplaceName());
    }

    @Override
    public JsonNode data() {
        return this.data;
    }

    /**
     * Creating workflow description from json tree.
     * @param root root node for workflow description
     * @return workflow description
     * @throws WorkflowException workflow exception
     */
    public static DefaultWorkflowDescription valueOf(JsonNode root) throws WorkflowException {

        JsonNode node = root.at(ptr(WF_WORKPLACE));
        if (!(node instanceof TextNode)) {
            throw new WorkflowException("invalid workflow workplace for " + root);
        }
        String wfWorkplaceName = node.asText();

        node = root.at(ptr(WF_ID));
        if (!(node instanceof TextNode)) {
            throw new WorkflowException("invalid workflow id for " + root);
        }
        URI wfId = URI.create(node.asText());

        node = root.at(ptr(WF_DATA));
        if (node instanceof MissingNode) {
            throw new WorkflowException("invalid workflow data for " + root);
        }
        JsonNode wfData = node;

        return builder()
                .workplaceName(wfWorkplaceName)
                .id(wfId)
                .data(wfData)
                .build();
    }

    private static String ptr(String field) {
        return "/" + field;
    }

    @Override
    public JsonNode toJson() {
        ObjectNode root = JsonNodeFactory.instance.objectNode();
        root.put(WF_WORKPLACE, workplaceName());
        root.put(WF_ID, id().toString());
        root.put(WF_DATA, data());
        return root;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("workplace", workplaceName())
                .add("id", id())
                .add("data", data())
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
     * Builder for workflow description.
     */
    public static class Builder {

        /**
         * Workplace name.
         */
        private String workplaceName;

        /**
         * Workflow ID.
         */
        private URI id;

        /**
         * Workflow data model.
         */
        private JsonNode data;

        /**
         * Sets workplace name.
         * @param workplaceName workplace name
         * @return builder
         */
        public Builder workplaceName(String workplaceName) {
            this.workplaceName = workplaceName;
            return this;
        }

        /**
         * Sets workflow id.
         * @param id workflow ID
         * @return builder
         */
        public Builder id(URI id) {
            this.id = id;
            return this;
        }

        /**
         * Sets workflow id.
         * @param id workflow ID string
         * @return builder
         */
        public Builder id(String id) {
            this.id = URI.create(id);
            return this;
        }

        /**
         * Sets workflow data model.
         * @param data workflow data model
         * @return builder
         */
        public Builder data(JsonNode data) {
            this.data = data;
            return this;
        }

        /**
         * Builds workflow description from builder.
         * @return instance of workflow description
         * @throws WorkflowException workflow exception
         */
        public DefaultWorkflowDescription build() throws WorkflowException {
            check(workplaceName != null, "workplaceName is invalid");
            check(id != null, "id is invalid");
            check(data != null, "data is invalid");
            return new DefaultWorkflowDescription(this);
        }
    }
}
