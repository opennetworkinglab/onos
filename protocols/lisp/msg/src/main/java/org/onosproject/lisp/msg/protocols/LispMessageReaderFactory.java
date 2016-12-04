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
import org.onlab.util.ByteOperator;
import org.onosproject.lisp.msg.protocols.DefaultLispEncapsulatedControl.EcmReader;
import org.onosproject.lisp.msg.protocols.DefaultLispInfoReply.InfoReplyReader;
import org.onosproject.lisp.msg.protocols.DefaultLispInfoRequest.InfoRequestReader;
import org.onosproject.lisp.msg.protocols.DefaultLispMapNotify.NotifyReader;
import org.onosproject.lisp.msg.protocols.DefaultLispMapRegister.RegisterReader;
import org.onosproject.lisp.msg.protocols.DefaultLispMapReply.ReplyReader;
import org.onosproject.lisp.msg.protocols.DefaultLispMapRequest.RequestReader;

/**
 * A factory class which helps to instantiate LISP reader class.
 */
public final class LispMessageReaderFactory {
    private static final int TYPE_SHIFT_BIT = 4;
    private static final int INFO_REPLY_INDEX = 3;

    private LispMessageReaderFactory() {}

    /**
     * Obtains corresponding LISP message reader.
     *
     * @param buffer netty byte buffer
     * @return LISP message reader
     */
    public static LispMessageReader getReader(ByteBuf buffer) {
        LispMessageReader reader;

        LispType type = LispType.valueOf(
                (short) (buffer.getUnsignedByte(0) >> TYPE_SHIFT_BIT));

        switch (type) {
            case LISP_MAP_REQUEST:
                reader = new RequestReader();
                break;
            case LISP_MAP_REPLY:
                reader = new ReplyReader();
                break;
            case LISP_MAP_REGISTER:
                reader = new RegisterReader();
                break;
            case LISP_MAP_NOTIFY:
                reader = new NotifyReader();
                break;
            case LISP_INFO:
                boolean isInfoReply = ByteOperator.getBit(
                        (byte) buffer.getUnsignedByte(0), INFO_REPLY_INDEX);
                if (isInfoReply) {
                    reader = new InfoReplyReader();
                } else {
                    reader = new InfoRequestReader();
                }
                break;
            case LISP_ENCAPSULATED_CONTROL:
                reader = new EcmReader();
                break;
            case UNKNOWN:
                throw new IllegalArgumentException("Unknown message type: "
                                                           + type);
            default:
                throw new IllegalArgumentException("Undefined message type: "
                                                           + type);
        }
        return reader;
    }
}
