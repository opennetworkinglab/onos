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
package org.onosproject.rabbitmq.impl;

import java.util.Map;
import java.util.concurrent.BlockingQueue;

import org.onosproject.rabbitmq.api.MQTransport;
import org.onosproject.rabbitmq.api.Manageable;

import static org.onosproject.rabbitmq.api.MQConstants.*;

/**
 * Provides handle to call MQSender for message delivery.
 */
public class MQTransportImpl implements MQTransport {

    @Override
    public Manageable registerProducer(BrokerHost host,
                                       Map<String, String> channelConf,
                                       BlockingQueue<MessageContext> queue) {
        String exchangeName = channelConf.get(EXCHANGE_NAME_PROPERTY);
        String routingKey = channelConf.get(ROUTING_KEY_PROPERTY);
        String queueName =  channelConf.get(QUEUE_NAME_PROPERTY);
        return new MQSender(queue, exchangeName, routingKey, queueName,
                            host.getUrl());
    }

}
