/*
 * Copyright 2019-present Open Networking Foundation
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
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.MoreObjects;
import org.slf4j.Logger;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Class for default worklet description.
 */
public final class DefaultWorkletDescription implements WorkletDescription {

    protected static final Logger log = getLogger(DefaultWorkletDescription.class);

    /**
     * worklet Name.
     */
    private String tag;

    /**
     * worklet data model.
     */
    private JsonDataModelTree data;

    /**
     * Constructor of worklet description.
     *
     * @param builder worklet description builder
     */
    public DefaultWorkletDescription(DefaultWorkletDescription.Builder builder) {
        this.tag = builder.tag;
        this.data = builder.data;
    }

    public DefaultWorkletDescription(String tag) {
        this.tag = tag;
    }

    @Override
    public String tag() {
        return this.tag;
    }

    @Override
    public JsonDataModelTree data() {
        return this.data;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("tag", tag())
                .add("data", data())
                .toString();
    }

    /**
     * Gets builder instance.
     *
     * @return builder instance
     */
    public static DefaultWorkletDescription.Builder builder() {
        return new DefaultWorkletDescription.Builder();
    }

    /**
     * Builder for worklet description.
     */
    public static class Builder {

        /**
         * worklet name.
         */
        private String tag;

        /**
         * static data model tree.
         */
        JsonDataModelTree data = new JsonDataModelTree();

        /**
         * Sets worklet name.
         *
         * @param tag worklet name
         * @return builder
         */
        public DefaultWorkletDescription.Builder name(String tag) {
            this.tag = tag;
            return this;
        }


        public DefaultWorkletDescription.Builder staticDataModel(String path, String value) throws WorkflowException {

            data.setAt(path, value);

            return this;
        }

        public DefaultWorkletDescription.Builder staticDataModel(String path, Integer value) throws WorkflowException {

            data.setAt(path, value);

            return this;
        }

        public DefaultWorkletDescription.Builder staticDataModel(String path, Boolean value) throws WorkflowException {

            data.setAt(path, value);

            return this;
        }

        public DefaultWorkletDescription.Builder staticDataModel(String path, JsonNode value) throws WorkflowException {

            data.setAt(path, value);

            return this;
        }

        public DefaultWorkletDescription.Builder staticDataModel(String path, ArrayNode value)
                throws WorkflowException {

            data.setAt(path, value);

            return this;
        }

        public DefaultWorkletDescription.Builder staticDataModel(String path, ObjectNode value)
                throws WorkflowException {

            data.setAt(path, value);

            return this;
        }


        /**
         * Builds worklet description from builder.
         *
         * @return instance of worklet description
         * @throws WorkflowException workflow exception
         */
        public DefaultWorkletDescription build() {

            return new DefaultWorkletDescription(this);
        }


    }
}
