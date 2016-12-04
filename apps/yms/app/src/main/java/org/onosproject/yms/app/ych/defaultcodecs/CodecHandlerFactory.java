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

package org.onosproject.yms.app.ych.defaultcodecs;


import org.onosproject.yms.app.ych.YchException;
import org.onosproject.yms.app.ych.defaultcodecs.xml.XmlCodecHandler;
import org.onosproject.yms.app.ych.defaultcodecs.xml.XmlCodecMultiInstanceHandler;
import org.onosproject.yms.app.ych.defaultcodecs.xml.XmlCodecMultiInstanceLeafHandler;
import org.onosproject.yms.app.ych.defaultcodecs.xml.XmlCodecSingleInstanceHandler;
import org.onosproject.yms.app.ych.defaultcodecs.xml.XmlCodecSingleInstanceLeafHandler;
import org.onosproject.yms.ych.YangProtocolEncodingFormat;
import org.onosproject.yms.ydt.YdtContext;
import org.onosproject.yms.ydt.YdtType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

import static org.onosproject.yms.ych.YangProtocolEncodingFormat.XML;
import static org.onosproject.yms.ydt.YdtType.MULTI_INSTANCE_LEAF_VALUE_NODE;
import static org.onosproject.yms.ydt.YdtType.MULTI_INSTANCE_NODE;
import static org.onosproject.yms.ydt.YdtType.SINGLE_INSTANCE_LEAF_VALUE_NODE;
import static org.onosproject.yms.ydt.YdtType.SINGLE_INSTANCE_NODE;

/**
 * Represents an YCH handle factory to create different types of YANG data tree
 * node.
 */
public final class CodecHandlerFactory {

    private static final Logger log =
            LoggerFactory.getLogger(CodecHandlerFactory.class);
    private static final String YDT_TYPE_ERROR = "YDT type is not supported.";

    /**
     * Map of xml codec handler.
     */
    private final Map<YdtType, XmlCodecHandler> handlerMap;

    /**
     * Creates a new codec handler factory.
     */
    private CodecHandlerFactory() {
        handlerMap = new HashMap<>();
        handlerMap.put(SINGLE_INSTANCE_NODE,
                       new XmlCodecSingleInstanceHandler());
        handlerMap.put(MULTI_INSTANCE_NODE,
                       new XmlCodecMultiInstanceHandler());
        handlerMap.put(SINGLE_INSTANCE_LEAF_VALUE_NODE,
                       new XmlCodecSingleInstanceLeafHandler());
        handlerMap.put(MULTI_INSTANCE_LEAF_VALUE_NODE,
                       new XmlCodecMultiInstanceLeafHandler());
    }

    /**
     * Returns YCH instance handler node instance.
     *
     * @param node   YDT context node
     * @param format data format type expected from driver
     * @return returns YCH handler node instance
     */
    public XmlCodecHandler getCodecHandlerForContext(
            YdtContext node,
            YangProtocolEncodingFormat format) {
        if (format == XML) {
            XmlCodecHandler handler = handlerMap.get(node.getYdtType());
            if (handler == null) {
                throw new YchException(YDT_TYPE_ERROR + node.getYdtType());
            }
            return handler;
        }
        log.error("{} data format is not supported.", format);
        return null;
    }

    /*
     * Bill Pugh Singleton pattern. INSTANCE won't be instantiated until the
     * LazyHolder class is loaded via a call to the instance() method below.
     */
    private static class LazyHolder {
        private static final CodecHandlerFactory INSTANCE =
                new CodecHandlerFactory();
    }

    /**
     * Returns a reference to the Singleton Codec Handler factory.
     *
     * @return the singleton codec handler factory
     */
    public static CodecHandlerFactory instance() {
        return LazyHolder.INSTANCE;
    }
}
