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
package org.onosproject.netconf;

import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stax.StAXSource;
import javax.xml.transform.stream.StreamResult;

import org.onosproject.netconf.rpc.RpcErrorType;
import org.slf4j.Logger;
import org.w3c.dom.Node;

import com.google.common.annotations.Beta;
import com.google.common.io.CharSource;
import com.google.common.io.CharStreams;

@Beta
public final class NetconfRpcParserUtil {

    private static final Logger log = getLogger(NetconfRpcParserUtil.class);

    /**
     * Parse first rpc-reply contained in the input.
     *
     * @param xml input
     * @return {@link NetconfRpcReply} or null on error
     */
    public static NetconfRpcReply parseRpcReply(CharSequence xml) {
        XMLInputFactory xif = XMLInputFactory.newFactory();
        try {
            XMLStreamReader xsr = xif.createXMLStreamReader(CharSource.wrap(xml).openStream());
            return parseRpcReply(xsr);
        } catch (XMLStreamException | IOException e) {
            log.error("Exception thrown creating XMLStreamReader", e);
            return null;
        }
    }

    /**
      * Parse first rpc-reply contained in the input.
      *
      * @param xsr input
      * @return {@link NetconfRpcReply} or null on error
      */
    public static NetconfRpcReply parseRpcReply(XMLStreamReader xsr) {
        try {
            for ( ; xsr.hasNext(); xsr.next()) {
                if (xsr.isStartElement() &&
                    xsr.getName().getLocalPart().equals("rpc-reply")) {

                    NetconfRpcReply.Builder builder = NetconfRpcReply.builder();
                    String msgId = xsr.getAttributeValue(null, "message-id");
                    builder.withMessageId(msgId);
                    xsr.nextTag();

                    return parseRpcReplyBody(builder, xsr);
                }
            }
        } catch (XMLStreamException e) {
            log.error("Exception thrown parsing rpc-reply", e);
        }
        return null;
    }

    private static NetconfRpcReply parseRpcReplyBody(NetconfRpcReply.Builder builder,
                                                     XMLStreamReader xsr) {

        try {
            for ( ; xsr.hasNext(); xsr.next()) {
                if (xsr.isStartElement()) {
                    switch (xsr.getName().getLocalPart()) {
                    case "ok":
                        try {
                            // skip to end of tag event
                            xsr.getElementText();
                        } catch (XMLStreamException e) {
                            log.warn("Failed parsing ok", e);
                        }
                        // ok should be the only element
                        return builder.buildOk();

                    case "rpc-error":
                        try {
                            JAXBContext context = JAXBContext.newInstance(RpcErrorType.class);
                            Unmarshaller unmarshaller = context.createUnmarshaller();
                            JAXBElement<RpcErrorType> error = unmarshaller.unmarshal(xsr, RpcErrorType.class);
                            builder.addError(NetconfRpcError.wrap(error.getValue()));
                        } catch (JAXBException e) {
                            log.warn("Failed parsing rpc-error", e);
                        }
                        break;

                    default: // =rpc-response
                        QName qName = xsr.getName();
                        try {
                            TransformerFactory tf = TransformerFactory.newInstance();
                            Transformer t = tf.newTransformer();

                            t.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
                            StringBuilder sb = new StringBuilder();
                            t.transform(new StAXSource(xsr),
                                        new StreamResult(CharStreams.asWriter(sb)));
                            builder.addResponses(sb.toString());
                        } catch (TransformerException e) {
                            log.warn("Failed parsing {}", qName, e);
                        }
                        break;
                    }
                }
            }
        } catch (XMLStreamException e) {
            log.error("Exception thrown parsing rpc-reply body", e);
        }

        return builder.build();

    }

    /**
     * Converts XML object into a String.
     *
     * @param xml Object (e.g., DOM {@link Node})
     * @return String representation of {@code xml} or empty on error.
     */
    @Beta
    public static String toString(Object xml) {
        TransformerFactory tf = TransformerFactory.newInstance();
        try {
            Transformer t = tf.newTransformer();
            t.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            Source xmlSource = null;
            if (xml instanceof Node) {
                xmlSource = new DOMSource((Node) xml);
            } else if (xml instanceof XMLEventReader) {
                xmlSource = new StAXSource((XMLEventReader) xml);
            } else if (xml instanceof XMLStreamReader) {
                xmlSource = new StAXSource((XMLStreamReader) xml);
            } else {
                log.warn("Unknown XML object type: {}, {}", xml.getClass(), xml);
                return "";
            }

            StringBuilder sb = new StringBuilder();
            t.transform(xmlSource, new StreamResult(CharStreams.asWriter(sb)));
            return sb.toString();
        } catch (TransformerException | XMLStreamException e) {
            log.error("Exception thrown", e);
            return "";
        }
    }

    private NetconfRpcParserUtil() {}

}
