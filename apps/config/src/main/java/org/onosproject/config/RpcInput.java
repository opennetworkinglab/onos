/*
 * Copyright 2016-present Open Networking Laboratory
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
package org.onosproject.config;

import com.google.common.annotations.Beta;
import org.onosproject.yang.model.DataNode;

/**
 * Abstraction for RPC input.
 */
@Beta
public class RpcInput {
    /**
     * Input data to the RPC execution.
     */
    DataNode input;

    /**
     * TODO
     * Any other meta data or contextual information
     * to help the RPC execution can be here.
     * Examples: List<DataNodes> to provide multiple inputs
     * Additional info for the broker, to choose a suitable executor
     */

    /**
     * Creates an instance of RpcInput.
     *
     * @param input to RPC execution
     */
    public RpcInput(DataNode input) {
        this.input = input;
    }

    /**
     * Returns RPC input.
     *
     * @return DataNode
     */
    public DataNode input() {
        return this.input;
    }
}