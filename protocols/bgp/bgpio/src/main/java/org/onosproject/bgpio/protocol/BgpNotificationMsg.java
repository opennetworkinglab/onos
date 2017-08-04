/*
 * Copyright 2015-present Open Networking Foundation
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

import org.onosproject.bgpio.exceptions.BgpParseException;

/**
 * Abstraction of an entity providing BGP notification message.
 */
public interface BgpNotificationMsg extends BgpMessage {
    /**
     * Returns errorCode in notification message.
     *
     * @return errorCode in notification message
     */
    byte getErrorCode();

    /**
     * Returns error subCode in notification message.
     *
     * @return error subCode in notification message
     */
    byte getErrorSubCode();

    /**
     * Returns error data in notification message.
     *
     * @return error data in notification message
     */
    byte[] getData();

    /**
     * Builder interface with get and set functions to build notification message.
     */
    public interface Builder extends BgpMessage.Builder {

        @Override
        BgpNotificationMsg build() throws BgpParseException;

        /**
         * Sets errorCode in notification message and return its builder.
         *
         * @param errorCode in notification message
         * @return builder by setting errorCode in notification message
         */
        Builder setErrorCode(byte errorCode);

        /**
         * Sets error subCode in notification message and return its builder.
         *
         * @param errorSubCode in notification message
         * @return builder by setting error subCode in notification message
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