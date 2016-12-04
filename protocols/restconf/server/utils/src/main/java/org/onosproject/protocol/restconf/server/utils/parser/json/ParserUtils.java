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
            processPathSegments(paths, builder, opType, true);
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
     * @param paths            the list of path segments split from URI
     * @param builder          the base YDT builder
     * @param opType           the YDT operation type for the Path segment
     * @param isFirstIteration true if paths contains all the URI segments
     * @return the YDT builder with the tree info of paths
     */
    private static YdtBuilder processPathSegments(List<String> paths,
                                                  YdtBuilder builder,
                                                  YdtContextOperationType opType,
                                                  boolean isFirstIteration) {
        if (paths.isEmpty()) {
            return builder;
        }

        boolean isLastSegment = paths.size() == 1;

        /*
         * Process the first segment in path.
         *
         * BUG ONOS-5500: YMS requires the treatment for the very first
         * segment in the URI path to be different than the rest. So,
         * we added a parameter, isFirstIteration, to this function.
         * It is set to true by the caller when this function is called
         * the very first time (i.e,, "paths" contains all the segments).
         *
         */
        YdtContextOperationType opTypeForThisSegment = isLastSegment ? opType : NONE;
        String segment = paths.iterator().next();
        processSinglePathSegment(segment, builder, opTypeForThisSegment, isFirstIteration);

        if (isLastSegment) {
            // We have hit the base case of recursion.
            return builder;
        }

        /*
         * Chop off the first segment, and recursively process the rest
         * of the path segments.
         */
        List<String> remainPaths = paths.subList(1, paths.size());
        processPathSegments(remainPaths, builder, opType, false);

        return builder;
    }

    private static void processSinglePathSegment(String pathSegment,
                                                 YdtBuilder builder,
                                                 YdtContextOperationType opType,
                                                 boolean isTopLevelSegment) {
        if (pathSegment.contains(COLON)) {
            processPathSegmentWithNamespace(pathSegment, builder, opType, isTopLevelSegment);
        } else {
            processPathSegmentWithoutNamespace(pathSegment, builder, opType);
        }
    }

    private static void processPathSegmentWithNamespace(String pathSegment,
                                                        YdtBuilder builder,
                                                        YdtContextOperationType opType,
                                                        boolean isTopLevelSegment) {
        if (isTopLevelSegment) {
            /*
             * BUG ONOS-5500: If this segment refers to the first node in
             * the path (i.e., top level of the model hierarchy), then
             * YMS requires 2 YDT nodes to be added instead of one. The
             * first one contains the namespace, and the second contains
             * the node name. For other segments in the path, only one
             * YDT node is needed.
             */
            addModule(builder, pathSegment);
        }

        String nodeName = getLatterSegment(pathSegment, COLON);
        String namespace = getPreSegment(pathSegment, COLON);
        convertPathSegmentToYdtNode(nodeName, namespace, builder, opType);
    }

    private static void processPathSegmentWithoutNamespace(String pathSegment,
                                                           YdtBuilder builder,
                                                           YdtContextOperationType opType) {
        convertPathSegmentToYdtNode(pathSegment, null, builder, opType);
    }

    private static void convertPathSegmentToYdtNode(String pathSegment,
                                                    String namespace,
                                                    YdtBuilder builder,
                                                    YdtContextOperationType opType) {
        if (pathSegment.contains(EQUAL)) {
            addListOrLeafList(pathSegment, namespace, builder, opType);
        } else {
            addLeaf(pathSegment, namespace, builder, opType);
        }
    }

    private static YdtBuilder addListOrLeafList(String path,
                                                String namespace,
                                                YdtBuilder builder,
                                                YdtContextOperationType opType) {
        String nodeName = getPreSegment(path, EQUAL);
        String keyStr = getLatterSegment(path, EQUAL);
        if (keyStr == null) {
            throw new JsonParseException(ERROR_LIST_MSG);
        }

        if (keyStr.contains(COMMA)) {
            List<String> keys = Lists.
                    newArrayList(COMMA_SPLITTER.split(keyStr));
            builder.addMultiInstanceChild(nodeName, namespace, keys, opType);
        } else {
            builder.addMultiInstanceChild(nodeName, namespace,
                                          Lists.newArrayList(keyStr), opType);
        }
        return builder;
    }

    private static YdtBuilder addLeaf(String path,
                                      String namespace,
                                      YdtBuilder builder,
                                      YdtContextOperationType opType) {
        checkNotNull(path);
        builder.addChild(path, namespace, opType);
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


    /**
     * Extracts the node name from a YDT node and encodes it in JSON format.
     * A JSON encoded node name has the following format:
     * <p>
     * module_name ":" node_name
     * <p>
     * where module_name is name of the YANG module in which the data
     * resource is defined, and node_name is the name of the data resource.
     * <p>
     * If the YDT node is null or its node name field is null, then the function
     * returns null. If the node name field is not null but module name field is,
     * then the function returns only the node name.
     *
     * @param ydtContext YDT node of the target data resource
     * @return JSON encoded name of the target data resource
     */
    public static String getJsonNameFromYdtNode(YdtContext ydtContext) {
        if (ydtContext == null) {
            return null;
        }

        String nodeName = ydtContext.getName();
        if (nodeName == null) {
            return null;
        }

        /*
         * The namespace field in YDT node is a string which contains a list
         * of identifiers separated by colon (:). e.g.,
         *
         * {identifier ":" identifier}+
         *
         * The last identifier in the string is the YANG module name.
         */
        String moduleName = getModuleNameFromNamespace(ydtContext.getNamespace());
        if (moduleName == null) {
            return nodeName;
        } else {
            return moduleName + COLON + nodeName;
        }
    }

    private static String getModuleNameFromNamespace(String namespace) {
        if (namespace == null) {
            return null;
        }

        String moduleName = null;

        if (namespace.contains(COLON)) {
            String[] tokens = namespace.split(COLON);
            moduleName = tokens[tokens.length - 1];
        }

        return moduleName;
    }
}
