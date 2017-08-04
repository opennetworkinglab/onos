/*
 * Copyright 2016-present Open Networking Foundation
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
package org.onosproject.rabbitmq.api;

import java.util.Map;
import java.util.concurrent.BlockingQueue;

import org.onosproject.rabbitmq.impl.BrokerHost;
import org.onosproject.rabbitmq.impl.MessageContext;

/**
 * API for registering producer with server.
 */
public interface MQTransport {
    /**
     * Registers MQ client with the server.
     *
     * @param host the broker host
     * @param channelConf the mq channel configurations
     * @param queue the message context
     * @return the sender handle
     */
    Manageable registerProducer(BrokerHost host, Map<String, String> channelConf,
            BlockingQueue<MessageContext> queue);

}
