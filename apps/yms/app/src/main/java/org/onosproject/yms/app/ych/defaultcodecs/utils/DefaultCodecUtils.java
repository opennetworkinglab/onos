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

package org.onosproject.yms.app.ych.defaultcodecs.utils;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import org.onosproject.yms.app.ych.YchException;
import org.onosproject.yms.ydt.YdtBuilder;
import org.onosproject.yms.ydt.YdtContextOperationType;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onosproject.yms.ydt.YdtContextOperationType.NONE;
import static org.onosproject.yms.ydt.YdtType.SINGLE_INSTANCE_NODE;

/**
 * Utils to complete the conversion between JSON and YDT(YANG DATA MODEL).
 */
public final class DefaultCodecUtils {

    private static final Splitter SLASH_SPLITTER = Splitter.on('/');
    private static final Splitter COMMA_SPLITTER = Splitter.on(',');
    private static final String EQUAL = "=";
    private static final String COMMA = ",";
    private static final String COLON = ":";
    private static final String URI_ENCODING_CHAR_SET = "ISO-8859-1";
    private static final String URI_NULL_CHECK_ERROR = "uri identifier " +
            "should not be null";
    private static final String URI_MODULE_FORMAT = "Illegal URI, First " +
            "node should be in format \"moduleName:nodeName\"";

    private static final String URI_LEAF_FORMAT = "Illegal URI, List or " +
            "Leaf-list node should be in format \"nodeName=key\"or " +
            "\"nodeName=instance-value\"";

    // no instantiation
    private DefaultCodecUtils() {
    }

    /**
     * Converts  URI identifier to YDT builder.
     *
     * @param identifier the uri identifier from web request
     * @param builder    the base YDT builder
     * @param ydtOpType  the YDT context operation type
     * @return the YDT builder with the tree info of identifier
     */
    public static YdtBuilder convertUriToYdt(
            String identifier,
            YdtBuilder builder,
            YdtContextOperationType ydtOpType) {
        checkNotNull(identifier, URI_NULL_CHECK_ERROR);
        List<String> segmentPaths =
                urlPathArgsDecode(SLASH_SPLITTER.split(identifier));
        if (segmentPaths.isEmpty()) {
            return null;
        }
        processPathSegments(segmentPaths, builder, ydtOpType);
        return builder;
    }

    /**
     * Converts a list of path segments to a YDT builder tree.
     *
     * @param paths     the list of path segments split from URI
     * @param builder   the base YDT builder
     * @param ydtOpType the YDT context operation type
     * @return the YDT builder with the tree info of paths
     */
    private static YdtBuilder processPathSegments(
            List<String> paths,
            YdtBuilder builder,
            YdtContextOperationType ydtOpType) {
        if (paths.isEmpty()) {
            return builder;
        }
        boolean isLastNode = paths.size() == 1;
        YdtContextOperationType thisOpType = isLastNode ? ydtOpType : NONE;

        final String path = paths.iterator().next();
        if (path.contains(COLON)) {
            addModule(builder, path);
            addNode(path, builder, thisOpType);
        } else if (path.contains(EQUAL)) {
            addListOrLeafList(path, builder, thisOpType);
        } else {
            addLeaf(path, builder, thisOpType);
        }

        if (isLastNode) {
            return builder;
        }
        List<String> remainPaths = paths.subList(1, paths.size());
        processPathSegments(remainPaths, builder, ydtOpType);

        return builder;
    }

    /**
     * Returns YDT builder after adding module node.
     *
     * @param builder YDT builder
     * @param path    path segment
     * @return the YDT builder
     */
    private static YdtBuilder addModule(YdtBuilder builder, String path) {
        String moduleName = getPreSegment(path, COLON);
        if (moduleName == null) {
            throw new YchException(URI_MODULE_FORMAT);
        }
        builder.addChild(moduleName, null, SINGLE_INSTANCE_NODE);
        return builder;
    }

    /**
     * Returns YDT builder after adding single instance node.
     *
     * @param path      path segments
     * @param builder   YDT builder
     * @param ydtOpType YDT context operation type
     * @return the YDT builder
     */
    private static YdtBuilder addNode(String path, YdtBuilder builder,
                                      YdtContextOperationType ydtOpType) {
        String nodeName = getPostSegment(path, COLON);
        builder.addChild(nodeName, null, SINGLE_INSTANCE_NODE, ydtOpType);
        return builder;
    }

    /**
     * Returns YDT builder after adding multi instance node.
     *
     * @param path    path segments
     * @param builder YDT builder
     * @param opType  the YDT context operation type
     * @return the YDT builder
     */
    private static YdtBuilder addListOrLeafList(
            String path,
            YdtBuilder builder,
            YdtContextOperationType opType) {
        String nodeName = getPreSegment(path, EQUAL);
        String keyStr = getPostSegment(path, EQUAL);
        if (keyStr == null) {
            throw new YchException(URI_LEAF_FORMAT);
        }
        builder.setDefaultEditOperationType(opType);
        if (keyStr.contains(COMMA)) {
            List<String> keys = Lists.newArrayList(
                    COMMA_SPLITTER.split(keyStr));
            builder.addMultiInstanceChild(nodeName, null, keys, null);
        } else {
            builder.addMultiInstanceChild(nodeName, null,
                                          Lists.newArrayList(keyStr), null);
        }
        return builder;
    }

    /**
     * Returns YDT builder after adding leaf.
     *
     * @param path      path segments
     * @param builder   YDT builder
     * @param ydtOpType YDT context operation type
     * @return the YDT builder
     */
    private static YdtBuilder addLeaf(String path, YdtBuilder builder,
                                      YdtContextOperationType ydtOpType) {
        checkNotNull(path);
        builder.addChild(path, null, ydtOpType);
        return builder;
    }

    /**
     * Returns the node name before the specified character in the string.
     *
     * @param path      path segment
     * @param splitChar character in the string
     * @return the node name string
     */
    private static String getPreSegment(String path, String splitChar) {
        int idx = path.indexOf(splitChar);
        if (idx == -1) {
            return null;
        }

        if (path.indexOf(':', idx + 1) != -1) {
            return null;
        }

        return path.substring(0, idx);
    }

    /**
     * Returns the string after the specified character in the string.
     *
     * @param path      path segment
     * @param splitChar character in the string
     * @return the substring after specified character
     */
    private static String getPostSegment(String path, String splitChar) {
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
    private static List<String> urlPathArgsDecode(Iterable<String> paths) {
        try {
            List<String> decodedPathArgs = new ArrayList<>();
            for (String pathArg : paths) {
                String decode = URLDecoder.decode(pathArg,
                                                  URI_ENCODING_CHAR_SET);
                decodedPathArgs.add(decode);
            }
            return decodedPathArgs;
        } catch (UnsupportedEncodingException e) {
            throw new YchException("Invalid URL path arg '" + paths + "': ", e);
        }
    }
}
