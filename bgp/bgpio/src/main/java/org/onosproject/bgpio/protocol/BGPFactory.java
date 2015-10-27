/*
 * Copyright 2015 Open Networking Laboratory
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

package org.onosproject.bgpio.protocol;

/**
 * Abstraction of an message factory providing builder functions to BGP messages
 * and objects.
 *
 */
public interface BGPFactory {

    /**
     * Gets the builder object for a open message.
     *
     * @return builder object for open message
     */
    BGPOpenMsg.Builder openMessageBuilder();

    /**
     * Gets the builder object for a keepalive message.
     *
     * @return builder object for keepalive message
     */
    BGPKeepaliveMsg.Builder keepaliveMessageBuilder();

    /**
     * Gets the builder object for a notification message.
     *
     * @return builder object for notification message.
     */
    BGPNotificationMsg.Builder notificationMessageBuilder();

    /**
     * Gets the BGP message reader.
     *
     * @return BGP message reader
     */
    BGPMessageReader<BGPMessage> getReader();

    /**
     * Returns BGP version.
     *
     * @return BGP version
     */
    BGPVersion getVersion();
}
