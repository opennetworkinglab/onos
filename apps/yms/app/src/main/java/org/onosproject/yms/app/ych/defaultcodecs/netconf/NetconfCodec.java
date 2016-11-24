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

package org.onosproject.yms.app.ych.defaultcodecs.netconf;

import com.google.common.collect.ImmutableSet;
import org.dom4j.Element;
import org.onosproject.yms.app.ych.YchException;
import org.onosproject.yms.ydt.YmsOperationType;

import java.util.Iterator;
import java.util.Set;

import static org.onosproject.yms.app.ych.defaultcodecs.netconf.NetconfCodecConstants.CONFIG;
import static org.onosproject.yms.app.ych.defaultcodecs.netconf.NetconfCodecConstants.DATA;
import static org.onosproject.yms.app.ych.defaultcodecs.netconf.NetconfCodecConstants.EDIT_CONFIG;
import static org.onosproject.yms.app.ych.defaultcodecs.netconf.NetconfCodecConstants.FILTER;
import static org.onosproject.yms.app.ych.defaultcodecs.netconf.NetconfCodecConstants.GET;
import static org.onosproject.yms.app.ych.defaultcodecs.netconf.NetconfCodecConstants.GET_CONFIG;
import static org.onosproject.yms.ydt.YmsOperationType.EDIT_CONFIG_REQUEST;
import static org.onosproject.yms.ydt.YmsOperationType.QUERY_CONFIG_REQUEST;
import static org.onosproject.yms.ydt.YmsOperationType.QUERY_REQUEST;

/**
 * Represents an YCH netconf codec to find the root element in the xml string.
 */
public class NetconfCodec {

    private static final String PROTO_OPER_ERROR = "Received protocol " +
            "operation is not same as in the XML string: ";
    private static final Set<String> ALLOWABLE_NAMES =
            ImmutableSet.of(CONFIG, DATA, FILTER);

    /**
     * Validate the operation type.
     *
     * @param elementName tag name in the xml string
     * @param opType      operation type
     */
    private void validateOpType(String elementName, YmsOperationType opType) {
        switch (elementName) {
            // edit-config tag name is found in xml then check the
            // interaction type.
            case EDIT_CONFIG: {
                if (opType != EDIT_CONFIG_REQUEST) {
                    throw new YchException(PROTO_OPER_ERROR + opType);
                }
                break;
            }

            // get-config tag name is found in xml then check the
            // interaction type.
            case GET_CONFIG: {
                if (opType != QUERY_CONFIG_REQUEST) {
                    throw new YchException(PROTO_OPER_ERROR + opType);
                }
                break;
            }

            // get tag name is found in xml then check the interaction type.
            case GET: {
                if (opType != QUERY_REQUEST) {
                    throw new YchException(PROTO_OPER_ERROR + opType);
                }
                break;
            }

            default: {
                //TODO
            }
        }
    }

    /**
     * Returns the data root element based on the NETCONF operation parameter.
     *
     * @param rootElement root element of document tree to find the root node
     * @param opType      protocol operation being performed
     * @return the data root node element
     */
    public Element getDataRootElement(Element rootElement,
                                      YmsOperationType opType) {

        Element retElement = null;
        String elementName = rootElement.getName();
        try {
            validateOpType(elementName, opType);
            // If config tag name is found then set the root element node.
            if (DATA.equals(elementName)
                    || CONFIG.equals(elementName)
                    || FILTER.equals(elementName)) {
                return rootElement;
            }

            // If element has child node then traverse through the child node
            // by recursively calling getDataRootElement method.
            if (rootElement.hasContent() && !rootElement.isTextOnly()) {
                for (Iterator i = rootElement.elementIterator();
                     i.hasNext();) {
                    Element childElement = (Element) i.next();
                    retElement = getDataRootElement(childElement, opType);
                }
            }
        } catch (Exception e) {
            // TODO
        }

        return retElement;
    }
}
