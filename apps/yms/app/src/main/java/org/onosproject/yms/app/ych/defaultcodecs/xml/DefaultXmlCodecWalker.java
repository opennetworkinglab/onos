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

package org.onosproject.yms.app.ych.defaultcodecs.xml;

import org.dom4j.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;

import static org.onosproject.yms.app.ych.defaultcodecs.xml.XmlNodeType.OBJECT_NODE;
import static org.onosproject.yms.app.ych.defaultcodecs.xml.XmlNodeType.TEXT_NODE;

/**
 * Represents implementation of codec xml walker.
 */
class DefaultXmlCodecWalker implements XmlWalker {
    private final Logger log = LoggerFactory.getLogger(getClass());

    @Override
    public void walk(XmlListener listener, Element element,
                     Element rootElement) {
        try {
            Element newElement = element.createCopy();
            newElement.remove(element.getNamespace());

            listener.enterXmlElement(element, getElementType(newElement),
                                     rootElement);

            if (element.hasContent() && !element.isTextOnly()) {
                for (Iterator i = element.elementIterator(); i.hasNext();) {
                    Element childElement = (Element) i.next();
                    walk(listener, childElement, rootElement);
                }
            }

            listener.exitXmlElement(element, getElementType(element),
                                    rootElement);
        } catch (Exception e) {
            log.error("Exception occurred when walk xml element: {}", element);
        }
    }

    /**
     * Determine the type of an element.
     *
     * @param element to be analysed
     * @return type of the element
     */
    private XmlNodeType getElementType(Element element) {
        return element.hasContent() && element.isTextOnly() ?
                TEXT_NODE : OBJECT_NODE;
    }
}
