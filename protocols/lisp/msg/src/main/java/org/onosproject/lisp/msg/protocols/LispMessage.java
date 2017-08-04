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
package org.onosproject.lisp.msg.protocols;

import io.netty.buffer.ByteBuf;
import org.onosproject.lisp.msg.exceptions.LispWriterException;

import java.net.InetSocketAddress;

/**
 * LISP message interface.
 */
public interface LispMessage {

    /**
     * Obtains LISP message type.
     *
     * @return LISP message type
     */
    LispType getType();

    /**
     * Configures the sender's IP address with port number.
     * Note that this information is used to make the UDP datagram packet.
     *
     * @param sender LISP message sender
     */
    void configSender(InetSocketAddress sender);

    /**
     * Obtains the sender's IP address with port number.
     * Note that this information is used to make the UDP datagram packet.
     *
     * @return send's IP address with port number
     */
    InetSocketAddress getSender();

    /**
     * Writes LISP message object into communication channel.
     *
     * @param byteBuf byte buffer
     * @throws LispWriterException if the writing request is failed due to
     * the lisp object cannot be written to the buffer.
     */
    void writeTo(ByteBuf byteBuf) throws LispWriterException;

    /**
     * Generates LISP message builder.
     *
     * @return builder object
     */
    Builder createBuilder();

    /**
     * LISP message builder interface.
     */
    interface Builder {

        /**
         * Obtains LISP message type.
         *
         * @return LISP message type
         */
        LispType getType();
    }
}
