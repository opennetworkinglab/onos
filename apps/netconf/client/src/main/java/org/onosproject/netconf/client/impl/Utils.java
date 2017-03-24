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

package org.onosproject.netconf.client.impl;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.StringReader;
import java.io.StringWriter;

/**
 * Represents utilities for huawei driver.
 */
public final class Utils {

    /**
     * Prevents creation of utils instance.
     */
    private Utils() {
    }

    // Default namespace given in yang files
    private static final String XMLNS_STRING = "xmlns=\"ne-l3vpn-api\"";
    private static final String XMLNS_HUA_STRING = "xmlns=\"http://www.huawei" +
            ".com/netconf/vrp\" format-version=\"1.0\" content-version=\"1.0\"";

    /**
     * YMS encode the java object into a xml string with xml namespace equals to
     * the namespace defined in YANG file. Huawei driver overwriting this
     * default xml namespace in generated xml string with xml string for Huawei.
     *
     * @param request xml string as an output of YMS encode operation
     * @return formatted string
     */
    private static String formatMessage(String request) {
        if (request.contains(XMLNS_STRING)) {
            request = request.replaceFirst(XMLNS_STRING, XMLNS_HUA_STRING);
        }
        return request;
    }

    /**
     * Returns the appended provided xml string with device specific rpc
     * request tags.
     *
     * @param encodedString xml string need to be updated
     * @return appended new tags xml string
     */
    static String editConfig(String encodedString) {

        // Add opening protocol edit config tags.
        StringBuilder rpc =
                new StringBuilder(
                        "<rpc xmlns=\"urn:ietf:params:xml:ns:netconf:base:1.0" +
                                "\" " +
                                "message-id=\"1\">");
        rpc.append("<edit-config>");
        rpc.append("<target>");
        rpc.append("<running/>");
        rpc.append("</target>");

        // Get the formatted XML namespace string.
        encodedString = formatMessage(encodedString);

        // Add the closing protocol edit config tags.
        rpc.append(encodedString);
        rpc.append("</edit-config>");
        rpc.append("</rpc>");

        return rpc.toString();
    }

    /**
     * Converts xml string to pretty format.
     *
     * @param input xml string to be converted to pretty format
     * @return pretty format xml string
     */
    static String prettyFormat(String input) {
        // Prepare input and output stream
        Source xmlInput = new StreamSource(new StringReader(input));
        StringWriter stringWriter = new StringWriter();
        StreamResult xmlOutput = new StreamResult(stringWriter);

        // Create transformer
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = null;

        try {
            transformer = transformerFactory.newTransformer();
        } catch (TransformerConfigurationException e) {
            e.printStackTrace();
        }

        // Need to omit the xml header and set indent to 4
        if (transformer != null) {
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache" +
                                                  ".org/xslt}indent-amount", "4");

            // Covert input string to xml pretty format and return
            try {
                transformer.transform(xmlInput, xmlOutput);
            } catch (TransformerException e) {
                e.printStackTrace();
            }
        }
        return xmlOutput.getWriter().toString();
    }
}
