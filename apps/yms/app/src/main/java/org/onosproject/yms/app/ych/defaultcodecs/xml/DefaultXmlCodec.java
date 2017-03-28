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

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.Namespace;
import org.onosproject.yms.app.ych.YchException;
import org.onosproject.yms.app.ych.defaultcodecs.netconf.NetconfCodec;
import org.onosproject.yms.app.ych.defaultcodecs.utils.DefaultCodecUtils;
import org.onosproject.yms.app.ydt.DefaultYdtWalker;
import org.onosproject.yms.app.ydt.YangRequestWorkBench;
import org.onosproject.yms.app.ydt.YdtExtendedBuilder;
import org.onosproject.yms.app.ydt.YdtExtendedContext;
import org.onosproject.yms.app.ydt.YdtExtendedWalker;
import org.onosproject.yms.app.ysr.YangSchemaRegistry;
import org.onosproject.yms.ych.YangCompositeEncoding;
import org.onosproject.yms.ych.YangDataTreeCodec;
import org.onosproject.yms.ydt.YdtBuilder;
import org.onosproject.yms.ydt.YmsOperationType;

import java.util.Map;

import static org.onosproject.yms.ych.YangProtocolEncodingFormat.XML;
import static org.onosproject.yms.ydt.YdtContextOperationType.CREATE;

/**
 * Represents an implementation of YCH data tree codec interface.
 */
public class DefaultXmlCodec implements YangDataTreeCodec {

    private static final String E_RESTCONF_ROOT = "/onos/restconf";
    private static final String E_YDT_ROOT_NODE = "YDT extended root node " +
            "is null.";
    private static final String E_ROOT_ELEMENT = "Root element in XML " +
            "input string is not well-formed.";
    private static final String E_ROOT_KEY_ELEMENT = "Root element " +
            "(filter, config, data) in XML input string is not found.";


    /**
     * Creates a new YANG xml codec.
     */
    public DefaultXmlCodec() {
    }

    /**
     * Returns the xml string from YDT.
     *
     * @param ydtBuilder YDT builder
     * @return the xml string from YDT
     */
    private String buildXmlForYdt(YdtBuilder ydtBuilder) {

        YdtExtendedBuilder extBuilder = (YdtExtendedBuilder) ydtBuilder;
        YdtExtendedContext rootNode = extBuilder.getRootNode();

        if (rootNode == null) {
            throw new YchException(E_YDT_ROOT_NODE);
        }

        // Creating the root element for xml.
        Element rootElement =
                DocumentHelper.createDocument().addElement(rootNode.getName());

        // Adding the name space if exist for root name.
        if (rootNode.getNamespace() != null) {
            rootElement.add(Namespace.get(rootNode.getNamespace()));
        }

        if ("config".equals(rootElement.getName())) {
            rootElement.add(new Namespace("nc", "urn:ietf:params:xml:ns:netconf:base:1.0"));
        }

        // Adding the attribute if exist
        Map<String, String> tagAttrMap = extBuilder.getRootTagAttributeMap();
        if (tagAttrMap != null && !tagAttrMap.isEmpty()) {
            for (Map.Entry<String, String> attr : tagAttrMap.entrySet()) {
                rootElement.addAttribute(attr.getKey(), attr.getValue());
            }
        }

        XmlCodecYdtListener listener = new XmlCodecYdtListener(XML, rootNode);
        listener.getElementStack().push(rootElement);

        // Walk through YDT and build the xml.
        YdtExtendedWalker extWalker = new DefaultYdtWalker();
        extWalker.walk(listener, rootNode);

        return rootElement.asXML();
    }

    @Override
    public String encodeYdtToProtocolFormat(YdtBuilder ydtBuilder) {
        return buildXmlForYdt(ydtBuilder);
    }

    @Override
    public YangCompositeEncoding encodeYdtToCompositeProtocolFormat(
            YdtBuilder ydtBuilder) {

        YangCompositeEncodingImpl encoding = new YangCompositeEncodingImpl();
        encoding.setResourceIdentifier(null);
        encoding.setResourceInformation(buildXmlForYdt(ydtBuilder));
        return encoding;
    }

    @Override
    public YdtBuilder decodeCompositeProtocolDataToYdt(
            YangCompositeEncoding protoData,
            Object schemaReg,
            YmsOperationType opType) {

        YdtExtendedBuilder extBuilder =
                new YangRequestWorkBench(E_RESTCONF_ROOT, null,
                                         opType,
                                         (YangSchemaRegistry) schemaReg,
                                         false);

        DefaultCodecUtils.convertUriToYdt(protoData.getResourceIdentifier(),
                                          extBuilder,
                                          CREATE);
        Document document;

        try {
            document = DocumentHelper
                    .parseText(protoData.getResourceInformation());
        } catch (DocumentException e) {
            throw new YchException(E_ROOT_ELEMENT);
        }

        XmlCodecListener listener = new XmlCodecListener();
        listener.setYdtExtBuilder(extBuilder);

        // Walk through xml and build the yang data tree.
        XmlWalker walker = new DefaultXmlCodecWalker();
        walker.walk(listener, document.getRootElement(),
                    document.getRootElement());
        return extBuilder;
    }

    @Override
    public YdtBuilder decodeProtocolDataToYdt(String protoData,
                                              Object schemaReg,
                                              YmsOperationType opType) {
        Document document;

        try {
            document = DocumentHelper.parseText(protoData);
        } catch (DocumentException e) {
            throw new YchException(E_ROOT_ELEMENT);
        }

        NetconfCodec codec = new NetconfCodec();
        // Find the root element in xml string
        Element rootElement =
                codec.getDataRootElement(document.getRootElement(), opType);

        if (rootElement == null) {
            throw new YchException(E_ROOT_KEY_ELEMENT);
        }

        // Get the YDT builder for the logical root name.
        YdtExtendedBuilder extBuilder =
                new YangRequestWorkBench(rootElement.getName(),
                                         rootElement.getNamespaceURI(),
                                         opType,
                                         (YangSchemaRegistry) schemaReg,
                                         false);

        XmlCodecListener listener = new XmlCodecListener();
        listener.setYdtExtBuilder(extBuilder);
        // Walk through xml and build the yang data tree.
        XmlWalker walker = new DefaultXmlCodecWalker();
        walker.walk(listener, rootElement, rootElement);
        return extBuilder;
    }
}
