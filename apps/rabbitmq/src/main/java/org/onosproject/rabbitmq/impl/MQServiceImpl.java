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

import java.io.UnsupportedEncodingException;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import org.apache.commons.lang.exception.ExceptionUtils;

import static org.onosproject.rabbitmq.api.MQConstants.*;

import org.onosproject.event.Event;
import org.onosproject.net.device.DeviceEvent;
import org.onosproject.net.link.LinkEvent;
import org.onosproject.net.packet.PacketContext;
import org.onosproject.net.topology.TopologyEvent;
import org.onosproject.rabbitmq.api.MQService;
import org.onosproject.rabbitmq.api.Manageable;
import org.onosproject.rabbitmq.util.MQUtil;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.gson.JsonObject;


import com.google.common.collect.Maps;

/**
 * Default implementation of {@link MQService}.
 */
public class MQServiceImpl implements MQService {
    private static final Logger log = LoggerFactory.getLogger(
                                                       MQServiceImpl.class);

    private final BlockingQueue<MessageContext> msgOutQueue =
                                                  new LinkedBlockingQueue<>(10);

    private Manageable manageSender;
    private String correlationId;

    /**
     * Initializes using ComponentContext.
     *
     * @param context ComponentContext from OSGI
     */
    public MQServiceImpl(ComponentContext context) {
        initializeProducers(context);
    }

    /**
     * Initializes MQ sender and receiver with RMQ server.
     *
     * @param context ComponentContext from OSGI
     */
    private void initializeProducers(ComponentContext context) {
        BrokerHost rfHost;
        Properties prop = MQUtil.getProp(context);
        if (prop == null) {
            log.error("RabbitMQ configuration file not found...");
            return;
        }
        try {
            correlationId = prop.getProperty(SENDER_COR_ID);
            rfHost = new BrokerHost(MQUtil.getMqUrl(
                                   prop.getProperty(SERVER_PROTO),
                                   prop.getProperty(SERVER_UNAME),
                                   prop.getProperty(SERVER_PWD),
                                   prop.getProperty(SERVER_ADDR),
                                   prop.getProperty(SERVER_PORT),
                                   prop.getProperty(SERVER_VHOST)));

            manageSender = registerProducer(rfHost,
                                            MQUtil.rfProducerChannelConf(
                                            prop.getProperty(SENDER_EXCHG),
                                            prop.getProperty(ROUTE_KEY),
                                            prop.getProperty(SENDER_QUEUE)),
                    msgOutQueue);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
        manageSender.start();
    }

    /**
     * Returns the handle to call an api for publishing messages to RMQ server.
     */
    private Manageable registerProducer(BrokerHost host, Map<String, String> channelConf,
            BlockingQueue<MessageContext> msgOutQueue) {
        return new MQTransportImpl().registerProducer(host, channelConf, msgOutQueue);
    }

    private byte[] bytesOf(JsonObject jo) {
        return jo.toString().getBytes();
    }

    /**
     * Publishes Device, Topology &amp; Link event message to MQ server.
     *
     * @param event Event received from the corresponding service like topology, device etc
     */
    @Override
    public void publish(Event<? extends Enum, ?> event) {
        byte[] body = null;
        if (null == event) {
                log.error("Captured event is null...");
                return;
        }
        if (event instanceof DeviceEvent) {
            body = bytesOf(MQUtil.json((DeviceEvent) event));
        } else if (event instanceof TopologyEvent) {
            body = bytesOf(MQUtil.json((TopologyEvent) event));
        } else if (event instanceof LinkEvent) {
            body = bytesOf(MQUtil.json((LinkEvent) event));
        } else {
            log.error("Invalid event: '{}'", event);
            return;
        }
        processAndPublishMessage(body);
    }

    /**
     * Publishes packet message to MQ server.
     *
     * @param context Context of the packet received including details like mac, length etc
     */
    @Override
    public void publish(PacketContext context) {
        byte[] body = bytesOf(MQUtil.json(context));
        processAndPublishMessage(body);
    }

    /*
     * Constructs message context and publish it to rabbit mq server.
     *
     * @param body Byte stream of the event's JSON data
     */
    private void processAndPublishMessage(byte[] body) {
        Map<String, Object> props = Maps.newHashMap();
        props.put(CORRELATION_ID, correlationId);
        MessageContext mc = new MessageContext(body, props);
        try {
            msgOutQueue.put(mc);
            String message = new String(body, "UTF-8");
            log.debug(" [x] Sent '{}'", message);
        } catch (InterruptedException | UnsupportedEncodingException e) {
            log.error(ExceptionUtils.getFullStackTrace(e));
        }
        manageSender.publish();
    }
}
