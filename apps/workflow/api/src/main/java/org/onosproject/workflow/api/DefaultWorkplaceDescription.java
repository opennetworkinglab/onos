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
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.MissingNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.common.base.MoreObjects;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.onosproject.workflow.api.CheckCondition.check;

/**
 * Class for default workplace description.
 */
public final class DefaultWorkplaceDescription implements WorkplaceDescription {

    /**
     * Name of workplace.
     */
    private final String name;

    /**
     * Data model of workplace(Optional).
     */
    private final Optional<JsonNode> optData;

    /**
     * Constructor of workplace description.
     * @param builder workplace builder
     */
    private DefaultWorkplaceDescription(Builder builder) {
        this.name = builder.name;
        this.optData = builder.optData;
    }

    @Override
    public String name() {
        return this.name;
    }

    @Override
    public Optional<JsonNode> data() {
        return this.optData;
    }

    /**
     * Creating workplace description from json tree.
     * @param root root node for workplace description
     * @return workplace description
     * @throws WorkflowException workflow exception
     */
    public static DefaultWorkplaceDescription valueOf(JsonNode root) throws WorkflowException {

        JsonNode node = root.at(ptr(WP_NAME));
        if (!(node instanceof TextNode)) {
            throw new WorkflowException("invalid workplace name for " + root);
        }

        Builder builder = builder()
            .name(node.asText());

        node = root.at(ptr(WP_DATA));
        if (node != null && !(node instanceof MissingNode)) {
            if (!(node instanceof ObjectNode) && !(node instanceof ArrayNode)) {
                throw new WorkflowException("invalid workplace data for " + root);
            }
            builder.data(node);
        }

        return builder.build();
    }

    private static String ptr(String field) {
        return "/" + field;
    }

    @Override
    public JsonNode toJson() {
        ObjectNode root = JsonNodeFactory.instance.objectNode();
        root.put(WP_NAME, name());
        if (data().isPresent()) {
            root.put(WP_DATA, data().get());
        }
        return root;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("name", name())
                .add("optData", data())
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
     * Builder for workplace description.
     */
    public static class Builder {

        /**
         * Workplace name.
         */
        private String name;

        /**
         * Workplace optData model.
         */
        private Optional<JsonNode> optData = Optional.empty();

        /**
         * List of workflow.
         */
        private List<DefaultWorkflowDescription> workflowDescs = new ArrayList<DefaultWorkflowDescription>();

        /**
         * Sets workplace name.
         * @param name workplace name
         * @return builder
         */
        public Builder name(String name) {
            this.name = name;
            return this;
        }

        /**
         * Sets optData model.
         * @param data workplace optData model
         * @return builder
         */
        public Builder data(JsonNode data) {
            this.optData = Optional.of(data);
            return this;
        }

        /**
         * Builds workplace description from builder.
         * @return instance of workflow description
         * @throws WorkflowException workflow exception
         */
        public DefaultWorkplaceDescription build() throws WorkflowException {
            check(name != null, "name is invalid");
            return new DefaultWorkplaceDescription(this);
        }
    }
}
