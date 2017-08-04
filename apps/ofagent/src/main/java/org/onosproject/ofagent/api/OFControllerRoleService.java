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
package org.onosproject.ofagent.api;

import io.netty.channel.Channel;
import org.projectfloodlight.openflow.protocol.OFControllerRole;

import java.util.Set;

/**
 * Service for administering the OF role of multiple controllers.
 */
public interface OFControllerRoleService {

    /**
     * Adds a new controller channel.
     * EQUAL role is set by default.
     *
     * @param channel openflow channel
     */
    void addControllerChannel(Channel channel);

    /**
     * Deletes the controller channel.
     *
     * @param channel openflow channel
     */
    void deleteControllerChannel(Channel channel);

    /**
     * Returns controller channels.
     *
     * @return set of controller channels
     */
    Set<Channel> controllerChannels();

    /**
     * Sets a role of the controller with a given channel.
     *
     * @param channel openflow channel
     * @param role    role of the controller
     */
    void setRole(Channel channel, OFControllerRole role);

    /**
     * Returns a role of the controller with a given channel.
     *
     * @param channel openflow channel
     * @return role of the controller
     */
    OFControllerRole role(Channel channel);
}
