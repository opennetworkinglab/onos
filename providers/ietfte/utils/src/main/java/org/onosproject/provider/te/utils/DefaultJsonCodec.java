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

package org.onosproject.provider.te.utils;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onosproject.yms.ych.YangCompositeEncoding;
import org.onosproject.yms.ych.YangDataTreeCodec;
import org.onosproject.yms.ydt.YdtBuilder;
import org.onosproject.yms.ydt.YdtContext;
import org.onosproject.yms.ydt.YmsOperationType;
import org.onosproject.yms.ymsm.YmsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.onosproject.protocol.restconf.server.utils.parser.json.ParserUtils.convertJsonToYdt;
import static org.onosproject.protocol.restconf.server.utils.parser.json.ParserUtils.convertUriToYdt;
import static org.onosproject.protocol.restconf.server.utils.parser.json.ParserUtils.convertYdtToJson;
import static org.onosproject.protocol.restconf.server.utils.parser.json.ParserUtils.getJsonNameFromYdtNode;
import static org.onosproject.provider.te.utils.CodecTools.jsonToString;
import static org.onosproject.provider.te.utils.CodecTools.toJson;
import static org.onosproject.yms.ydt.YdtContextOperationType.NONE;
import static org.onosproject.yms.ych.YangResourceIdentifierType.URI;


/**
 * JSON/YDT Codec implementation.
 */
public class DefaultJsonCodec implements YangDataTreeCodec {
    private static final String RESTCONF_ROOT = "/onos/restconf";
    private static final String DATA = "data";
    private static final String SLASH = "/";

    private final YmsService ymsService;

    private final Logger log = LoggerFactory.getLogger(getClass());

    public DefaultJsonCodec(YmsService service) {
        ymsService = service;
    }

    @Override
    public String encodeYdtToProtocolFormat(YdtBuilder builder) {
        YdtContext context = builder.getRootNode();
        ObjectNode jsonNode = convertYdtToJson(getJsonNameFromYdtNode(context),
                                               builder.getRootNode(),
                                               ymsService.getYdtWalker());
        return jsonToString(jsonNode);
    }

    @Override
    public YangCompositeEncoding encodeYdtToCompositeProtocolFormat(
            YdtBuilder builder) {
        YdtContext rootNode = builder.getRootNode();
        String rootName = rootNode.getName();
        YdtContext child = rootNode.getFirstChild();
        String name = child.getName();
        String url = rootName + SLASH + DATA + SLASH + name;
        String jsonRoot = getJsonNameFromYdtNode(child);
        ObjectNode objectNode = convertYdtToJson(jsonRoot, child,
                                                 ymsService.getYdtWalker());
        String payload = jsonToString((ObjectNode) objectNode.get(jsonRoot));
        return new YangCompositeEncodingImpl(URI, url, payload);
    }

    @Override
    public YdtBuilder decodeProtocolDataToYdt(String protocolData,
                                              Object schemaRegistry,
                                              YmsOperationType opType) {
        // Get a new builder
        YdtBuilder builder = ymsService.getYdtBuilder(RESTCONF_ROOT,
                                                      null,
                                                      opType,
                                                      schemaRegistry);

        convertJsonToYdt(toJson(protocolData), builder);
        return builder;
    }

    @Override
    public YdtBuilder decodeCompositeProtocolDataToYdt(YangCompositeEncoding protocolData,
                                                       Object schemaRegistry,
                                                       YmsOperationType opType) {

        YdtBuilder builder = ymsService.getYdtBuilder(RESTCONF_ROOT,
                                                      null,
                                                      opType,
                                                      schemaRegistry);

        // YdtContextOperationType should be NONE for URI in QUERY_RESPONSE.
        convertUriToYdt(protocolData.getResourceIdentifier(), builder, NONE);

        // NULL/EMPTY for Resource data
        builder.setDefaultEditOperationType(null);

        // Convert the payload json body to ydt
        convertJsonToYdt(toJson(protocolData.getResourceInformation()), builder);
        return builder;
    }
}
