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

import org.onosproject.event.Event;
import org.onosproject.net.packet.PacketContext;

/**
 * Service apis for publishing device and packet events.
 */
public interface MQService {

    /**
     * Publishes device/link/topology events to MQ server.
     *
     * @param event the event type
     */
    void publish(Event<? extends Enum, ?> event);

    /**
     * Publishes packet context message to MQ server.
     *
     * @param context for processing an inbound packet
     */
    void publish(PacketContext context);

}
