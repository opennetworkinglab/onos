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
package org.onosproject.lisp.msg.protocols;

import io.netty.buffer.ByteBuf;

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
     * Writes LISP message object into communication channel.
     *
     * @param byteBuf byte buffer
     */
    void writeTo(ByteBuf byteBuf);

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
