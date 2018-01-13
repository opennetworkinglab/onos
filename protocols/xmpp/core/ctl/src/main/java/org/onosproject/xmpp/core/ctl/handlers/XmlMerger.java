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

import com.fasterxml.aalto.stax.OutputFactoryImpl;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import org.dom4j.io.DOMReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;


import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;
import javax.xml.transform.dom.DOMResult;
import java.util.List;

/**
 * Merges incoming XML events (elements) into XML document.
 */
public class XmlMerger extends MessageToMessageDecoder {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    protected final XMLOutputFactory xmlOutputFactory = new OutputFactoryImpl();
    protected DocumentBuilder docBuilder;
    protected Document document;
    protected DOMResult result;
    protected XMLEventWriter writer;
    protected int depth;

    public XmlMerger() throws ParserConfigurationException {
        initDocBuilder();
        this.resetWriter();
    }

    private void initDocBuilder() throws ParserConfigurationException {
        try {
            final DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
            docBuilderFactory.setIgnoringElementContentWhitespace(true);
            docBuilderFactory.setIgnoringComments(true);
            this.docBuilder = docBuilderFactory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            throw e;
        }
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, Object object, List out) throws Exception {
        try {
            if (object instanceof  XMLEvent) {
                final XMLEvent event = (XMLEvent) object;

                if (event.isStartDocument() || event.isEndDocument()) {
                    return;
                }

                if (event.isCharacters() && depth <= 1) {
                    return;
                }

                if (depth < 1 && event.isStartElement()) {
                    out.add(object);
                    depth++;
                    return;
                }

                if (depth <= 1 && event.isEndElement()) {
                    out.add(object);
                    depth--;
                    return;
                }

                writer.add(event);

                if (event.isStartElement()) {
                    depth++;
                } else if (event.isEndElement()) {
                    depth--;

                    if (depth == 1) {
                        writer.flush();
                        org.dom4j.Element xmlElement = transform().getRootElement();
                        out.add(xmlElement);
                        writer.close();
                        resetWriter();
                    }
                }
            }
        } catch (Exception e) {
            logger.info(e.getCause().getMessage());
            throw e;
        }
    }

    private org.dom4j.Document transform() {
        org.dom4j.io.DOMReader reader = new DOMReader();
        return reader.read(document);
    }

    private void resetWriter() {
        try {
            document = newDocument();
            result = new DOMResult(document);
            writer = xmlOutputFactory.createXMLEventWriter(result);
        } catch (XMLStreamException e) {
            throw new InternalError("Error creating writer");
        }
    }

    private Document newDocument() {
        return docBuilder.newDocument();
    }


}
