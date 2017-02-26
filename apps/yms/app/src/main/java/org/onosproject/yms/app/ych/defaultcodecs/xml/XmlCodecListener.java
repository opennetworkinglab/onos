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

import org.dom4j.Attribute;
import org.dom4j.Element;
import org.onosproject.yms.app.ydt.YdtExtendedBuilder;
import org.onosproject.yms.ydt.YdtContextOperationType;

import java.util.Iterator;

import static org.onosproject.yms.app.ych.defaultcodecs.netconf.NetconfCodecConstants.OPERATION;
import static org.onosproject.yms.app.ych.defaultcodecs.xml.XmlNodeType.OBJECT_NODE;
import static org.onosproject.yms.app.ych.defaultcodecs.xml.XmlNodeType.TEXT_NODE;

/**
 * Default implementation of codec xml listener.
 */
class XmlCodecListener implements XmlListener {

    /**
     * YANG data tree builder object.
     */
    private YdtExtendedBuilder ydtExtBuilder;

    private String prevNodeNamespace;

    /**
     * Sets the YANG data tree builder object.
     *
     * @param ydtBuilder YANG data tree builder object
     */
    void setYdtExtBuilder(YdtExtendedBuilder ydtBuilder) {
        ydtExtBuilder = ydtBuilder;
    }

    @Override
    public void enterXmlElement(Element element, XmlNodeType nodeType,
                                Element rootElement) {
        if (element.equals(rootElement)) {
            return;
        }

        YdtContextOperationType opType = null;

        for (Iterator iter = element.attributeIterator(); iter.hasNext();) {
            Attribute attr = (Attribute) iter.next();
            if (attr.getName().equals(OPERATION)) {
                opType =
                        YdtContextOperationType.valueOf(attr.getValue()
                                                                .toUpperCase());
            }
        }

        String nameSpace = null;
        if (element.getNamespace() != null) {
            nameSpace = element.getNamespace().getURI();
        }

        /*
         * When new module has to be added, and if curnode has reference of
         * previous module, then we need to traverse back to parent(logical
         * root node).
         */
        if (ydtExtBuilder.getRootNode() == ydtExtBuilder.getCurNode()
                .getParent() && prevNodeNamespace != null &&
                !prevNodeNamespace.equals(nameSpace)) {
            ydtExtBuilder.traverseToParent();
        }

        if (nodeType == OBJECT_NODE &&
                (element.content() == null || element.content().isEmpty())) {
            if (ydtExtBuilder != null) {
                if (ydtExtBuilder.getCurNode() == ydtExtBuilder.getRootNode()) {
                    ydtExtBuilder.addChild(null, nameSpace, opType);
                }
                ydtExtBuilder.addNode(element.getName(), nameSpace);
            }
        } else if (nodeType == OBJECT_NODE) {
            if (ydtExtBuilder != null) {
                if (ydtExtBuilder.getCurNode() == ydtExtBuilder.getRootNode()) {
                    ydtExtBuilder.addChild(null, nameSpace, opType);
                }
                ydtExtBuilder.addChild(element.getName(), nameSpace, opType);
            }
        } else if (nodeType == TEXT_NODE) {
            if (ydtExtBuilder != null) {
                if (ydtExtBuilder.getCurNode() == ydtExtBuilder.getRootNode()) {
                    ydtExtBuilder.addChild(null, nameSpace, opType);
                }
                ydtExtBuilder.addLeaf(element.getName(), nameSpace,
                                      element.getText());
            }
        }

        if (nameSpace != null) {
            prevNodeNamespace = nameSpace;
        }
    }

    @Override
    public void exitXmlElement(Element element, XmlNodeType nodeType,
                               Element rootElement) {
        if (element.equals(rootElement)) {
            return;
        }

        if (ydtExtBuilder != null) {
            ydtExtBuilder.traverseToParent();
        }
    }
}
