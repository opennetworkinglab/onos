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
 * Abstraction of an entity which provides interfaces for xml walk.
 * This interface serve as common tools for anyone who needs to parse the xml
 * node with depth-first algorithm.
 */
interface XmlWalker {
    /**
     * Walks the xml data tree. Protocols implements xml listener service
     * and walks xml tree with input as implemented object. xml walker provides
     * call backs to implemented methods.
     *
     * @param listener    xml listener implemented by the protocol
     * @param walkElement root node(element) of the xml data tree
     * @param rootElement logical root node(element) of the xml data tree
     */
    void walk(XmlListener listener, Element walkElement, Element rootElement);
}
