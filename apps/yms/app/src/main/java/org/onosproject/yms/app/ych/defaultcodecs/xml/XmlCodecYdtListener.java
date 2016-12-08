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
import org.onosproject.yms.app.ych.defaultcodecs.CodecHandlerFactory;
import org.onosproject.yms.app.ydt.YdtExtendedContext;
import org.onosproject.yms.app.ydt.YdtExtendedListener;
import org.onosproject.yms.ych.YangProtocolEncodingFormat;
import org.onosproject.yms.ydt.YdtContext;

import java.util.Objects;
import java.util.Stack;

import static org.onosproject.yms.ych.YangProtocolEncodingFormat.XML;

/**
 * Represents implementation of codec YANG data object listener.
 */
class XmlCodecYdtListener implements YdtExtendedListener {

    /**
     * Data format type requested from driver.
     */
    private YangProtocolEncodingFormat dataFormat;

    /**
     * Stack for element is maintained for hierarchical references, this is
     * used during YDT walker and preparation of xml/json.
     */
    private final Stack<Element> elementStack = new Stack<>();

    /**
     * Root name received from driver.
     */
    private YdtExtendedContext rootYdtNode;

    /**
     * Module YDT node.
     */
    private YdtExtendedContext currentModule;

    /**
     * Creates a new codec listener.
     *
     * @param format   protocol data format
     * @param rootNode extended YDT root node
     */
    XmlCodecYdtListener(YangProtocolEncodingFormat format,
                        YdtExtendedContext rootNode) {
        dataFormat = format;
        rootYdtNode = rootNode;
        currentModule = ((YdtExtendedContext) rootNode.getFirstChild());
    }

    /**
     * Returns the stack for the element.
     *
     * @return the stack for the element
     */
    Stack<Element> getElementStack() {
        return elementStack;
    }

    /**
     * Returns true, if YDT node is module node; false otherwise.
     *
     * @param ydtContext YDT node
     * @return true if YDT node is module; false otherwise
     */
    private boolean isModuleNode(YdtExtendedContext ydtContext,
                                 boolean isExit) {
        if (Objects.equals(currentModule, ydtContext)) {
            if (isExit) {
                currentModule = (YdtExtendedContext) currentModule
                        .getNextSibling();
            }
            return true;
        }
        return false;
    }

    @Override
    public void enterYdtNode(YdtExtendedContext ydtContext) {

        if (!Objects.equals(rootYdtNode, ydtContext) &&
                !isModuleNode(ydtContext, false)) {

            CodecHandlerFactory factory = CodecHandlerFactory.instance();
            XmlCodecHandler handler =
                    factory.getCodecHandlerForContext(ydtContext, dataFormat);
            try {
                if (dataFormat == XML && handler != null) {
                    handler.processXmlContext(ydtContext, elementStack);
                }
            } catch (Exception e) {
                // TODO
            }

            if (dataFormat == XML && handler != null) {
                handler.setXmlValue(ydtContext, elementStack);
            }
        }
    }

    @Override
    public void exitYdtNode(YdtExtendedContext ydtExtendedContext) {
        if (!Objects.equals(rootYdtNode, ydtExtendedContext) &&
                !isModuleNode(ydtExtendedContext, true)) {
            elementStack.pop();
        }
    }

    @Override
    public void enterYdtNode(YdtContext ydtContext) {
    }

    @Override
    public void exitYdtNode(YdtContext ydtContext) {
    }

}
