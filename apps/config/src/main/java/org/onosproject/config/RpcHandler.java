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

/**
 *  Service for entities that would execute RPC methods invoked through
 *  Dynamic Config RPC brokerage.
 */
@Beta
public interface RpcHandler {
    /*
     * Executes the RPC.
     *
     * @param msgId of the RPC message to be executed
     * @param cmd to be executed
     * @param input data to the RPC command
     */
    void executeRpc(Integer msgId, RpcCommand cmd, RpcInput input);
}