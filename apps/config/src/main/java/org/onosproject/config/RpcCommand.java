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
import org.onosproject.yang.model.ResourceId;
/**
 * Abstract implementation of an RPC command.
 */
@Beta
public abstract class RpcCommand {
    /**
     * Identifier of an RPC command.
     */
    ResourceId cmdId;

    /**
     * Creates an instance of RpcCommand.
     *
     * @param cmdId of RPC command
     */
    public RpcCommand(ResourceId cmdId) {
        this.cmdId = cmdId;
    }

    /**
     * Returns the RPC command id.
     *
     * @return cmdId
     */
    public ResourceId cmdId() {
        return this.cmdId;
    }
    /**
     * Executes the RPC command.
     *
     * @param input input data to the RPC command.
     */
    public abstract void execute(RpcInput input);
}