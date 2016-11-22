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
import org.onosproject.yms.ydt.YdtContext;

import java.util.Iterator;
import java.util.Stack;

/**
 * Represents a multi instance leaf node handler in YCH.
 */
public class XmlCodecMultiInstanceLeafHandler extends XmlCodecHandler {

    @Override
    public void setXmlValue(YdtContext ydtContext,
                            Stack<Element> elementStack) {

        if (ydtContext.getValueSet().isEmpty()) {
            return;
        }

        Iterator<String> iterator = ydtContext.getValueSet().iterator();
        elementStack.peek().setText(iterator.next());
        Element topOfStack = elementStack.pop();
        Element parent = elementStack.peek();

        while (iterator.hasNext()) {
            Element newElement = updateNameAndNamespace(ydtContext, parent);
            newElement.setText(iterator.next());
        }
        elementStack.push(topOfStack);
    }
}
