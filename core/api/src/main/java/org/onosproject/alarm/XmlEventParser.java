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
package org.onosproject.alarm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStream;
import java.time.DateTimeException;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Parser for Netconf notifications.
 */
public final class XmlEventParser {
    public static final Logger log = LoggerFactory
            .getLogger(XmlEventParser.class);

    private static final String DISALLOW_DTD_FEATURE = "http://apache.org/xml/features/disallow-doctype-decl";
    private static final String DISALLOW_EXTERNAL_DTD =
            "http://apache.org/xml/features/nonvalidating/load-external-dtd";
    private static final String EVENTTIME_TAGNAME = "eventTime";

    private XmlEventParser() {
    }

    /**
     * Creates a document from the input stream message and returns the result.
     *
     * @param message input stream message
     * @return the document result
     * @throws SAXException Throws SAX Exception
     * @throws IOException Throws IO Exception
     * @throws ParserConfigurationException Throws ParserConfigurationException
     */
    public static Document createDocFromMessage(InputStream message)
            throws SAXException, IOException, ParserConfigurationException {
        DocumentBuilderFactory dbfactory = DocumentBuilderFactory.newInstance();
        //Disabling DTDs in order to avoid XXE xml-based attacks.
        disableFeature(dbfactory, DISALLOW_DTD_FEATURE);
        disableFeature(dbfactory, DISALLOW_EXTERNAL_DTD);
        dbfactory.setXIncludeAware(false);
        dbfactory.setExpandEntityReferences(false);
        DocumentBuilder builder = dbfactory.newDocumentBuilder();
        return builder.parse(new InputSource(message));
    }

    private static void disableFeature(DocumentBuilderFactory dbfactory, String feature) {
        try {
            dbfactory.setFeature(feature, true);
        } catch (ParserConfigurationException e) {
            // This should catch a failed setFeature feature
            log.info("ParserConfigurationException was thrown. The feature '" +
                    feature + "' is probably not supported by your XML processor.");
        }
    }

    public static long getEventTime(String dateTime)
        throws UnsupportedOperationException, IllegalArgumentException {
        try {
            OffsetDateTime date = OffsetDateTime.parse(dateTime, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
            return date.toInstant().toEpochMilli();
        } catch (DateTimeException e) {
            log.error("Cannot parse exception {} {}", dateTime, e);
        }
        return System.currentTimeMillis();
    }

    public static long getEventTime(Document doc)
        throws UnsupportedOperationException, IllegalArgumentException {
        String dateTime = getEventTimeNode(doc).getTextContent();
        return getEventTime(dateTime);
    }

    public static Node getDescriptionNode(Document doc) {
        return getEventTimeNode(doc).getNextSibling();
    }

    private static Node getEventTimeNode(Document doc) {
        return doc.getElementsByTagName(EVENTTIME_TAGNAME).item(0);
    }
}
