/*
 * Copyright 2017-present Open Networking Foundation
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
package org.onlab.util;

import java.io.IOException;
import java.io.StringWriter;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.io.CharSource;

/**
 * PrettyPrinted XML String.
 */
public class XmlString implements CharSequence {

    private static final Logger log = LoggerFactory.getLogger(XmlString.class);

    private final Supplier<String> prettyString;


    /**
     * Prettifies given XML String.
     *
     * @param xml input XML
     * @return prettified input or input itself is input is not well-formed
     */
    public static CharSequence prettifyXml(CharSequence xml) {
        return new XmlString(CharSource.wrap(xml));
    }

    XmlString(CharSource inputXml) {
        prettyString = Suppliers.memoize(() -> prettyPrintXml(inputXml));
    }

    private String prettyPrintXml(CharSource inputXml) {
        try {
            Document document = DocumentBuilderFactory.newInstance()
                    .newDocumentBuilder()
                    .parse(new InputSource(inputXml.openStream()));

            document.normalize();

            XPath xPath = XPathFactory.newInstance().newXPath();
            NodeList nodeList = (NodeList) xPath.evaluate("//text()[normalize-space()='']",
                                                          document,
                                                          XPathConstants.NODESET);

            for (int i = 0; i < nodeList.getLength(); ++i) {
                Node node = nodeList.item(i);
                node.getParentNode().removeChild(node);
            }

            // Setup pretty print options
            Transformer t = TransformerFactory.newInstance().newTransformer();
            t.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            t.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            t.setOutputProperty(OutputKeys.INDENT, "yes");
            t.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");

            // Return pretty print xml string
            StringWriter strWriter = new StringWriter();
            t.transform(new DOMSource(document), new StreamResult(strWriter));
            return strWriter.toString();
        } catch (Exception e) {
            log.warn("Pretty printing failed", e);
            try {
                String rawInput = inputXml.read();
                log.debug("  failed input: \n{}", rawInput);
                return rawInput;
            } catch (IOException e1) {
                log.error("Failed to read from input", e1);
                return inputXml.toString();
            }
        }
    }

    @Override
    public int length() {
        return toString().length();
    }

    @Override
    public char charAt(int index) {
        return toString().charAt(index);
    }

    @Override
    public CharSequence subSequence(int start, int end) {
        return toString().subSequence(start, end);
    }

    @Override
    public String toString() {
        return prettyString.get();
    }

}
