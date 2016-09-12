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

import static org.onosproject.lisp.msg.protocols.DefaultLispMapReply.ReplyReader;
import static org.onosproject.lisp.msg.protocols.DefaultLispMapNotify.NotifyReader;
import static org.onosproject.lisp.msg.protocols.DefaultLispMapRegister.RegisterReader;
import static org.onosproject.lisp.msg.protocols.DefaultLispMapRequest.RequestReader;

/**
 * A factory class which helps to instantiate LISP reader class.
 */
public final class LispMessageReaderFactory {
    private static final int TYPE_SHIFT_BIT = 4;

    private LispMessageReaderFactory() {}

    /**
     * Obtains corresponding LISP message reader.
     *
     * @param buffer netty byte buffer
     * @return LISP message reader
     */
    public static LispMessageReader getReader(ByteBuf buffer) {
        LispMessageReader reader;

        int type = buffer.getByte(0) >> TYPE_SHIFT_BIT;
        switch (type) {
            case 1:
                reader = new RequestReader();
                break;
            case 2:
                reader = new ReplyReader();
                break;
            case 3:
                reader = new RegisterReader();
                break;
            case 4:
                reader = new NotifyReader();
                break;
            default:
                throw new IllegalArgumentException("Unknown LISP message type: " + type);
        }
        return reader;
    }
}
