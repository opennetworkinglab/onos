/*
 * Copyright 2018-present Open Networking Foundation
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

package org.onosproject.xmpp.core.ctl.handlers;


import com.fasterxml.aalto.AsyncByteArrayFeeder;
import com.fasterxml.aalto.AsyncXMLInputFactory;
import com.fasterxml.aalto.AsyncXMLStreamReader;
import com.fasterxml.aalto.stax.InputFactoryImpl;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import org.codehaus.stax2.ri.evt.Stax2EventAllocatorImpl;
import org.dom4j.DocumentFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.stream.XMLStreamException;
import java.util.List;

/**
 * Decodes a incoming data from XML stream.
 */
public class XmlStreamDecoder extends ByteToMessageDecoder {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private static final AsyncXMLInputFactory XML_INPUT_FACTORY = new InputFactoryImpl();
    private Stax2EventAllocatorImpl allocator = new Stax2EventAllocatorImpl();
    private AsyncXMLStreamReader<AsyncByteArrayFeeder> streamReader = XML_INPUT_FACTORY.createAsyncForByteArray();
    private DocumentFactory df = DocumentFactory.getInstance();

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {

        AsyncByteArrayFeeder streamFeeder = streamReader.getInputFeeder();
        logger.info("Decoding XMPP data.. ");

        byte[] buffer = new byte[in.readableBytes()];
        in.readBytes(buffer);
        logger.debug("Buffer length: " + buffer.length);
        try {
            streamFeeder.feedInput(buffer, 0, buffer.length);
        } catch (XMLStreamException exception) {
            logger.info(exception.getMessage());
            in.skipBytes(in.readableBytes());
            logger.info("Bytes skipped");
            throw exception;
        }

        while (streamReader.hasNext() && streamReader.next() != AsyncXMLStreamReader.EVENT_INCOMPLETE) {
            out.add(allocator.allocate(streamReader));
        }

    }
}
