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

package org.onosproject.protocol.restconf.server.utils.parser.json;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import org.onosproject.protocol.restconf.server.utils.exceptions.JsonParseException;
import org.onosproject.protocol.restconf.server.utils.parser.api.JsonBuilder;
import org.onosproject.protocol.restconf.server.utils.parser.api.NormalizedYangNode;
import org.onosproject.yms.ydt.YdtBuilder;
import org.onosproject.yms.ydt.YdtContext;
import org.onosproject.yms.ydt.YdtContextOperationType;
import org.onosproject.yms.ydt.YdtListener;
import org.onosproject.yms.ydt.YdtType;
import org.onosproject.yms.ydt.YdtWalker;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onosproject.yms.ydt.YdtContextOperationType.NONE;

/**
 * Utils to complete the conversion between JSON and YDT(YANG DATA MODEL).
 */
public final class ParserUtils {

    private static final Splitter SLASH_SPLITTER = Splitter.on('/');
    private static final Splitter COMMA_SPLITTER = Splitter.on(',');
    private static final String EQUAL = "=";
    private static final String COMMA = ",";
    private static final String COLON = ":";
    private static final String URI_ENCODING_CHAR_SET = "ISO-8859-1";
    private static final String ERROR_LIST_MSG = "List/Leaf-list node should be " +
            "in format \"nodeName=key\"or \"nodeName=instance-value\"";
    private static final String ERROR_MODULE_MSG = "First node should be in " +
            "format \"moduleName:nodeName\"";

    // no instantiation
    private ParserUtils() {
    }

    /**
     * Converts  URI identifier to YDT builder.
     *
     * @param id      the uri identifier from web request
     * @param builder the base ydt builder
     * @param opType  the ydt operation type for the uri
     */
    public static void convertUriToYdt(String id,
                                       YdtBuilder builder,
                                       YdtContextOperationType opType) {
        checkNotNull(id, "uri identifier should not be null");
        List<String> paths = urlPathArgsDecode(SLASH_SPLITTER.split(id));
        if (!paths.isEmpty()) {
            processPathSegments(paths, builder, opType);
        }
    }

    /**
     * Converts  JSON objectNode to YDT builder. The objectNode can be any
     * standard JSON node, node just for RESTconf payload.
     *
     * @param objectNode the objectNode from web request
     * @param builder    the base ydt builder
     */
    public static void convertJsonToYdt(ObjectNode objectNode,
                                        YdtBuilder builder) {

        JsonToYdtListener listener = new JsonToYdtListener(builder);
        new DefaultJsonWalker().walk(listener, null, objectNode);
    }

    /**
     * Converts a Ydt context tree to a JSON object.
     *
     * @param rootName the name of the YdtContext from which the YdtListener
     *                 start to builder a Json Object
     * @param context  a abstract data model for YANG data
     * @param walker   abstraction of an entity which provides interfaces for
     *                 YDT walk
     * @return the JSON node corresponding the YANG data
     */
    public static ObjectNode convertYdtToJson(String rootName,
                                              YdtContext context,
                                              YdtWalker walker) {
        JsonBuilder builder = new DefaultJsonBuilder();
        YdtListener listener = new YdtToJsonListener(rootName, builder);
        walker.walk(listener, context);
        return builder.getTreeNode();
    }

    /**
     * Converts a list of path segments to a YDT builder tree.
     *
     * @param paths   the list of path segments split from URI
     * @param builder the base YDT builder
     * @param opType  the YDT operation type for the Path segment
     * @return the YDT builder with the tree info of paths
     */
    private static YdtBuilder processPathSegments(List<String> paths,
                                                  YdtBuilder builder,
                                                  YdtContextOperationType opType) {
        if (paths.isEmpty()) {
            return builder;
        }
        boolean isLastNode = paths.size() == 1;
        YdtContextOperationType opTypeForThisNode = isLastNode ? opType : NONE;

        String path = paths.iterator().next();
        if (path.contains(COLON)) {
            addModule(builder, path);
            addNode(path, builder, opTypeForThisNode);
        } else if (path.contains(EQUAL)) {
            addListOrLeafList(path, builder, opTypeForThisNode);
        } else {
            addLeaf(path, builder, opTypeForThisNode);
        }

        if (isLastNode) {
            return builder;
        }
        List<String> remainPaths = paths.subList(1, paths.size());
        processPathSegments(remainPaths, builder, opType);

        return builder;
    }

    private static YdtBuilder addListOrLeafList(String path,
                                                YdtBuilder builder,
                                                YdtContextOperationType opType) {
        String nodeName = getPreSegment(path, EQUAL);
        String keyStr = getLatterSegment(path, EQUAL);
        if (keyStr == null) {
            throw new JsonParseException(ERROR_LIST_MSG);
        }
        builder.setDefaultEditOperationType(opType);
        if (keyStr.contains(COMMA)) {
            List<String> keys = Lists.
                    newArrayList(COMMA_SPLITTER.split(keyStr));
            builder.addMultiInstanceChild(nodeName, null, keys);
        } else {
            builder.addMultiInstanceChild(nodeName, null,
                                          Lists.newArrayList(keyStr));
        }
        return builder;
    }

    private static YdtBuilder addLeaf(String path, YdtBuilder builder,
                                      YdtContextOperationType opType) {
        checkNotNull(path);
        builder.addChild(path, null, opType);
        return builder;
    }

    private static YdtBuilder addModule(YdtBuilder builder, String path) {
        String moduleName = getPreSegment(path, COLON);
        if (moduleName == null) {
            throw new JsonParseException(ERROR_MODULE_MSG);
        }
        builder.addChild(moduleName, null, YdtType.SINGLE_INSTANCE_NODE);
        return builder;
    }

    private static YdtBuilder addNode(String path, YdtBuilder builder,
                                      YdtContextOperationType opType) {
        String nodeName = getLatterSegment(path, COLON);
        builder.addChild(nodeName,
                         null,
                         YdtType.SINGLE_INSTANCE_NODE,
                         opType);
        return builder;
    }

    /**
     * Returns the previous segment of a path which is separated by a split char.
     * For example:
     * <pre>
     * "foo:bar", ":"   -->  "foo"
     * </pre>
     *
     * @param path      the original path string
     * @param splitChar char used to split the path
     * @return the previous segment of the path
     */
    private static String getPreSegment(String path, String splitChar) {
        int idx = path.indexOf(splitChar);
        if (idx == -1) {
            return null;
        }

        if (path.indexOf(splitChar, idx + 1) != -1) {
            return null;
        }

        return path.substring(0, idx);
    }

    /**
     * Returns the latter segment of a path which is separated by a split char.
     * For example:
     * <pre>
     * "foo:bar", ":"   -->  "bar"
     * </pre>
     *
     * @param path      the original path string
     * @param splitChar char used to split the path
     * @return the latter segment of the path
     */
    private static String getLatterSegment(String path, String splitChar) {
        int idx = path.indexOf(splitChar);
        if (idx == -1) {
            return path;
        }

        if (path.indexOf(splitChar, idx + 1) != -1) {
            return null;
        }

        return path.substring(idx + 1);
    }

    /**
     * Converts a list of path from the original format to ISO-8859-1 code.
     *
     * @param paths the original paths
     * @return list of decoded paths
     */
    public static List<String> urlPathArgsDecode(Iterable<String> paths) {
        try {
            List<String> decodedPathArgs = new ArrayList<>();
            for (String pathArg : paths) {
                String decode = URLDecoder.decode(pathArg,
                                                  URI_ENCODING_CHAR_SET);
                decodedPathArgs.add(decode);
            }
            return decodedPathArgs;
        } catch (UnsupportedEncodingException e) {
            throw new JsonParseException("Invalid URL path arg '" +
                                                 paths + "': ", e);
        }
    }

    /**
     * Converts a field to a simple YANG node description which contains the
     * namespace and name information.
     *
     * @param field field name of a JSON body, or a segment of a URI
     *              in a request of RESTCONF
     * @return a simple normalized YANG node
     */
    public static NormalizedYangNode buildNormalizedNode(String field) {
        String namespace = getPreSegment(field, COLON);
        String name = getLatterSegment(field, COLON);
        return new NormalizedYangNode(namespace, name);
    }
}
