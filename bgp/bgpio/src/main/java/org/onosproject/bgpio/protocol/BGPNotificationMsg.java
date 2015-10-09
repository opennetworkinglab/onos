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

import org.onosproject.bgpio.exceptions.BGPParseException;
import org.onosproject.bgpio.types.BGPHeader;

/**
 * Abstraction of an entity providing BGP Notification Message.
 */
public interface BGPNotificationMsg extends BGPMessage {
    /**
     * Returns errorCode in Notification message.
     *
     * @return errorCode in Notification message
     */
    byte getErrorCode();

    /**
     * Returns error SubCode in Notification message.
     *
     * @return error SubCode in Notification message
     */
    byte getErrorSubCode();

    /**
     * Returns error data in Notification message.
     *
     * @return error data in Notification message
     */
    byte[] getData();

    /**
     * Builder interface with get and set functions to build Notification
     * message.
     */
    public interface Builder extends BGPMessage.Builder {

        @Override
        BGPNotificationMsg build() throws BGPParseException;

        /**
         * Sets notification message header and returns its builder.
         *
         * @param header of notification message
         * @return Builder by setting notification message header
         */
        Builder setNotificationMsgHeader(BGPHeader header);

        /**
         * Sets errorCode in notification message and return its builder.
         *
         * @param errorCode in notification message
         * @return builder by setting ErrorCode in notification message
         */
        Builder setErrorCode(byte errorCode);

        /**
         * Sets error SubCode in notification message and return its builder.
         *
         * @param errorSubCode in notification Message
         * @return builder by setting ErrorSubCode in notification Message
         */
        Builder setErrorSubCode(byte errorSubCode);

        /**
         * Sets error data in notification message and return its builder.
         *
         * @param data in notification message
         * @return builder by setting Data in notification message
         */
        Builder setData(byte[] data);
    }
}