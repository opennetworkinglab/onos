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

/**
 * Abstraction of an entity which provide call back methods which are called
 * by xml walker while walking the xml data tree. This interface needs to be
 * implemented by protocol implementing listener's based call backs while
 * xml walk.
 */
interface XmlListener {

    /**
     * Callback invoked during a node entry. All the related information
     * about the node can be obtained from the element.
     *
     * @param element     current xml node(element)
     * @param nodeType    xml node type
     * @param rootElement root element
     */
    void enterXmlElement(Element element, XmlNodeType nodeType,
                         Element rootElement);

    /**
     * Callback invoked during a node exit. All the related information
     * about the node can be obtained from the element.
     *
     * @param element     current xml node(element)
     * @param nodeType    xml node type
     * @param rootElement root element
     */
    void exitXmlElement(Element element, XmlNodeType nodeType,
                        Element rootElement);
}
