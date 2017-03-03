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
 * Abstraction for RPC output.
 */
@Beta
public class RpcOutput {
    public enum Status {
        /**
         * RPC execution was successful.
         */
        RPC_SUCCESS,
        /**
         * RPC execution failed.
         */
        RPC_FAILURE,
        /**
         * RPC execution don't have any output data.
         */
        RPC_NODATA,
        /**
         * Failed to receive a response from the receiver, within the broker specified timeout.
         */
        RPC_TIMEOUT,
    }

    /**
     * Status of RPC execution.
     */
    Status status;
    /**
     * Output data from the RPC execution.
     */
    DataNode output;

    /**
     * Creates an instance of RpcOutput.
     *
     * @param status of RPC execution
     * @param output of RPC execution
     */
    public RpcOutput(Status status, DataNode output) {
        this.status = status;
        this.output = output;
    }

    /**
     * Returns RPC status.
     *
     * @return Status
     */
    public RpcOutput.Status status() {
        return this.status;
    }

    /**
     * Returns RPC output.
     *
     * @return DataNode
     */
    public DataNode output() {
        return this.output;
    }
}