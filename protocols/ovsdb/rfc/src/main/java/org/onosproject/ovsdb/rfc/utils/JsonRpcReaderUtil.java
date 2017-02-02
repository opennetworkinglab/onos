/*
 * Copyright 2015-present Open Networking Laboratory
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
package org.onosproject.ovsdb.rfc.utils;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;

import java.io.DataInput;
import java.io.IOException;
import java.util.List;
import java.util.Stack;

import org.onosproject.ovsdb.rfc.exception.UnsupportedException;
import org.onosproject.ovsdb.rfc.jsonrpc.JsonReadContext;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.io.IOContext;
import com.fasterxml.jackson.core.json.ByteSourceJsonBootstrapper;
import com.fasterxml.jackson.core.util.BufferRecycler;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MappingJsonFactory;

/**
 * Decoder utility class.
 */
public final class JsonRpcReaderUtil {

    /**
     * Constructs a JsonRpcReaderUtil object. Utility classes should not have a
     * public or default constructor, otherwise IDE will compile unsuccessfully.
     * This class should not be instantiated.
     */
    private JsonRpcReaderUtil() {
    }

    /**
     * Decode the bytes to Json object.
     * @param in input of bytes
     * @param out ouput of Json object list
     * @param jrContext context for the last decoding process
     * @throws IOException IOException
     * @throws JsonParseException JsonParseException
     */
    public static void readToJsonNode(ByteBuf in, List<Object> out, JsonReadContext jrContext)
            throws JsonParseException, IOException {
        int lastReadBytes = jrContext.getLastReadBytes();
        if (lastReadBytes == 0) {
            if (in.readableBytes() < 4) {
                return;
            }
            checkEncoding(in);
        }

        int i = lastReadBytes + in.readerIndex();
        Stack<Byte> bufStack = jrContext.getBufStack();
        for (; i < in.writerIndex(); i++) {
            byte b = in.getByte(i);
            switch (b) {
            case '{':
                if (!isDoubleQuote(bufStack)) {
                    bufStack.push(b);
                    jrContext.setStartMatch(true);
                }
                break;
            case '}':
                if (!isDoubleQuote(bufStack)) {
                    bufStack.pop();
                }
                break;
            case '"':
                if (in.getByte(i - 1) != '\\') {
                    if (!bufStack.isEmpty() && bufStack.peek() != '"') {
                        bufStack.push(b);
                    } else {
                        bufStack.pop();
                    }
                }
                break;
            default:
                break;
            }

            if (jrContext.isStartMatch() && bufStack.isEmpty()) {
                ByteBuf buf = in.readSlice(i - in.readerIndex() + 1);
                JsonParser jf = new MappingJsonFactory().createParser((DataInput) new ByteBufInputStream(buf));
                JsonNode jsonNode = jf.readValueAsTree();
                out.add(jsonNode);
                lastReadBytes = 0;
                jrContext.setLastReadBytes(lastReadBytes);
                break;
            }
        }

        if (i >= in.writerIndex()) {
            lastReadBytes = in.readableBytes();
            jrContext.setLastReadBytes(lastReadBytes);
        }
    }

    /**
     * Filter the invalid characters before decoding.
     * @param in input of bytes
     * @param lastReadBytes the bytes for last decoding incomplete record
     */
    private static void fliterCharaters(ByteBuf in) {
        while (in.isReadable()) {
            int ch = in.getByte(in.readerIndex());
            if ((ch != ' ') && (ch != '\n') && (ch != '\t') && (ch != '\r')) {
                break;
            } else {
                in.readByte();
            }
        }
    }

    /**
     * Check whether the peek of the stack element is double quote.
     * @param jrContext context for the last decoding process
     * @return boolean
     */
    private static boolean isDoubleQuote(Stack<Byte> bufStack) {
        if (!bufStack.isEmpty() && bufStack.peek() == '"') {
            return true;
        }
        return false;
    }

    /**
     * Check whether the encoding is valid.
     * @param in input of bytes
     * @throws IOException this is an IO exception
     * @throws UnsupportedException this is an unsupported exception
     */
    private static void checkEncoding(ByteBuf in) throws IOException {
        int inputStart = 0;
        int inputLength = 4;
        fliterCharaters(in);
        byte[] buff = new byte[4];
        in.getBytes(in.readerIndex(), buff);
        ByteSourceJsonBootstrapper strapper = new ByteSourceJsonBootstrapper(new IOContext(new BufferRecycler(),
                                                                                           null,
                                                                                           false),
                                                                             buff, inputStart,
                                                                             inputLength);
        JsonEncoding jsonEncoding = strapper.detectEncoding();
        if (!JsonEncoding.UTF8.equals(jsonEncoding)) {
            throw new UnsupportedException("Only UTF-8 encoding is supported.");
        }
    }

}
