/*
 * Copyright 2017-present Open Networking Foundation
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

import org.onosproject.yang.model.RpcInput;
import org.onosproject.yang.model.RpcOutput;
import org.onosproject.yang.model.RpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.util.function.Supplier;

/**
 * Rpc Executor.
 */
public final class RpcExecutor implements Supplier<RpcOutput> {
    private final Logger log = LoggerFactory.getLogger(getClass());
    private RpcService handler;
    private String msgId;
    private RpcInput input;
    private String rpcName;
    int svcId;

    /**
     * Creates an RpcExecutor.
     *
     * @param handler rpc handler
     * @param svcId svcId
     * @param rpcName  rpcName
     * @param msgId rpc msgId
     * @param input rpc input
     */
    public RpcExecutor(RpcService handler, int svcId, String rpcName, String msgId, RpcInput input) {
        this.handler  = handler;
        this.rpcName = rpcName;
        this.svcId = svcId;
        this.msgId = msgId;
        this.input = input;
    }

    @Override
    public RpcOutput get() {
        RpcOutput ret;
        try {
            ret = (RpcOutput) handler.getClass().getInterfaces()[svcId].
                    getMethod(rpcName, RpcInput.class).invoke(handler, input);
        } catch (NoSuchMethodException | IllegalAccessException |
                InvocationTargetException | IllegalArgumentException e) {
            throw new FailedException(e.getMessage() + ", request:" + msgId);
        }
        ret.messageId(msgId);
        return ret;
    }
}