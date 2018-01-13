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

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import org.dom4j.DocumentFactory;
import org.dom4j.Element;
import org.dom4j.QName;
import org.onosproject.xmpp.core.XmppConstants;
import org.onosproject.xmpp.core.ctl.XmppValidator;
import org.onosproject.xmpp.core.ctl.exception.UnsupportedStanzaTypeException;
import org.onosproject.xmpp.core.ctl.exception.XmppValidationException;
import org.onosproject.xmpp.core.stream.XmppStreamClose;
import org.onosproject.xmpp.core.stream.XmppStreamError;
import org.onosproject.xmpp.core.stream.XmppStreamOpen;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmpp.packet.IQ;
import org.xmpp.packet.Message;
import org.xmpp.packet.Packet;
import org.xmpp.packet.Presence;

import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.Namespace;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import java.util.Iterator;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Translates XML Element to XMPP Packet.
 */
public class XmppDecoder extends MessageToMessageDecoder {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private XmppValidator validator = new XmppValidator();

    @Override
    protected void decode(ChannelHandlerContext channelHandlerContext, Object object, List out) throws Exception {
            if (object instanceof Element) {
                Element root = (Element) object;

                try {
                    Packet packet = recognizeAndReturnXmppPacket(root);
                    validate(packet);
                    out.add(packet);
                } catch (UnsupportedStanzaTypeException e) {
                    throw e;
                } catch (Exception e) {
                    throw new XmppValidationException(false);
                }

            } else if (object instanceof XMLEvent) {

                XMLEvent event = (XMLEvent) object;
                if (event.isStartElement()) {
                    final StartElement element = event.asStartElement();

                    if (element.getName().getLocalPart().equals(XmppConstants.STREAM_QNAME)) {
                        DocumentFactory df = DocumentFactory.getInstance();
                        QName qname = (element.getName().getPrefix() == null) ?
                                df.createQName(element.getName().getLocalPart(),
                                               element.getName().getNamespaceURI()) :
                                df.createQName(element.getName().getLocalPart(),
                                               element.getName().getPrefix(), element.getName().getNamespaceURI());

                        Element newElement = df.createElement(qname);

                        Iterator nsIt = element.getNamespaces();
                        // add all relevant XML namespaces to Element
                        while (nsIt.hasNext()) {
                            Namespace ns = (Namespace) nsIt.next();
                            newElement.addNamespace(ns.getPrefix(), ns.getNamespaceURI());
                        }

                        Iterator attrIt = element.getAttributes();
                        // add all attributes to Element
                        while (attrIt.hasNext()) {
                            Attribute attr = (Attribute) attrIt.next();
                            newElement.addAttribute(attr.getName().getLocalPart(), attr.getValue());
                        }
                        XmppStreamOpen xmppStreamOpen = new XmppStreamOpen(newElement);
                        validator.validateStream(xmppStreamOpen);
                        out.add(xmppStreamOpen);
                    }
                } else if (event.isEndElement()) {
                    out.add(new XmppStreamClose());
                }
            }

    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        logger.info("Exception caught: {}", cause.getMessage());
        if (cause.getCause() instanceof XmppValidationException) {
            if (((XmppValidationException) cause.getCause()).isStreamValidationException()) {
                XmppStreamError.Condition condition = XmppStreamError.Condition.bad_format;
                XmppStreamError error = new XmppStreamError(condition);
                ctx.channel().writeAndFlush(error);
                ctx.channel().writeAndFlush(new XmppStreamClose());
                return;
            }
        }
        logger.info("Not a StreamValidationException. Sending exception upstream.");
        ctx.fireExceptionCaught(cause);
    }


    private void validate(Packet packet) throws UnsupportedStanzaTypeException, XmppValidationException {
        validator.validate(packet);
    }

    protected Packet recognizeAndReturnXmppPacket(Element root)
            throws UnsupportedStanzaTypeException, IllegalArgumentException {
        checkNotNull(root);

        Packet packet = null;
        if (root.getName().equals(XmppConstants.IQ_QNAME)) {
            packet = new IQ(root);
        } else if (root.getName().equals(XmppConstants.MESSAGE_QNAME)) {
            packet = new Message(root);
        } else if (root.getName().equals(XmppConstants.PRESENCE_QNAME)) {
            packet = new Presence(root);
        } else {
            throw new UnsupportedStanzaTypeException("Unrecognized XMPP Packet");
        }
        logger.info("XMPP Packet received\n" + root.asXML());
        return packet;
    }

}
