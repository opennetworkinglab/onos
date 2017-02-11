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
 * Service for entities that would invoke RPCs and receive RPC responses,
 * through the Dynamic Config brokerage.
 */
@Beta
public interface RpcCaller {
    /*
     * Receives an RPC response.
     *
     * @param msgId of a previously invoked RPC
     * @param output from the RPC execution
     */
    void receiveResponse(Integer msgId, RpcOutput output);
}