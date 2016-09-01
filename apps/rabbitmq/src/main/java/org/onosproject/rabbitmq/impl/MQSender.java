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
package org.onosproject.rabbitmq.impl;

import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeoutException;

import org.onosproject.rabbitmq.api.Manageable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import static org.onosproject.rabbitmq.api.MQConstants.*;

/**
 * Connects client with server using start API, publish the messages received
 * from onos events and disconnect the client from server using stop API.
 */
public class MQSender implements Manageable {

    private static final String E_CREATE_CHAN =
                                  "Error creating the RabbitMQ channel";
    private static final String E_PUBLISH_CHAN =
                                  "Error in publishing to the RabbitMQ channel";
    private static final Logger log = LoggerFactory.getLogger(MQSender.class);
    private static final int RECOVERY_INTERVAL = 15000;

    private final BlockingQueue<MessageContext> outQueue;
    private final String exchangeName;
    private final String routingKey;
    private final String queueName;
    private final String url;

    private ExecutorService executorService;
    private Connection conn;
    private Channel channel;


    /**
     * Creates a MQSender initialized with the specified parameters.
     *
     * @param outQueue     represents message context
     * @param exchangeName represents mq exchange name
     * @param routingKey   represents bound routing key
     * @param queueName    represents mq queue name
     * @param url          represents the mq server url
     */
    public MQSender(BlockingQueue<MessageContext> outQueue, String exchangeName,
            String routingKey, String queueName, String url) {
        this.outQueue = outQueue;
        this.exchangeName = exchangeName;
        this.routingKey = routingKey;
        this.queueName = queueName;
        this.url = url;
    }

    /**
     * Sets the executor service.
     *
     * @param executorService the executor service to use
     */
    public void setExecutorService(ExecutorService executorService) {
        this.executorService = executorService;
    }

    @Override
    public void start() {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setAutomaticRecoveryEnabled(true);
        factory.setNetworkRecoveryInterval(RECOVERY_INTERVAL);
        try {
            factory.setUri(url);
            if (executorService != null) {
                conn = factory.newConnection(executorService);
            } else {
                conn = factory.newConnection();
            }
            channel = conn.createChannel();
            channel.exchangeDeclare(exchangeName, TOPIC, true);
            /*
             * Setting the following parameters to queue
             * durable    - true
             * exclusive  - false
             * autoDelete - false
             * arguments  - null
             */
            channel.queueDeclare(this.queueName, true, false, false, null);
            channel.queueBind(queueName, exchangeName, routingKey);
        } catch (Exception e) {
            log.error(E_CREATE_CHAN, e);
        }
    }

    @Override
    public void publish() {
        try {
                MessageContext input = outQueue.poll();
                channel.basicPublish(exchangeName, routingKey,
                                new AMQP.BasicProperties.Builder()
                                .correlationId(COR_ID).build(),
                                input.getBody());
                String message1 = new String(input.getBody(), "UTF-8");
                log.debug(" [x] Sent: '{}'", message1);
        } catch (Exception e) {
            log.error(E_PUBLISH_CHAN, e);
        }
    }

    @Override
    public void stop() {
        try {
            channel.close();
            conn.close();
        } catch (IOException e) {
            log.error("Error closing the rabbit MQ connection", e);
        } catch (TimeoutException e) {
            log.error("Timeout exception in closing the rabbit MQ connection",
                      e);
        }
    }

}
