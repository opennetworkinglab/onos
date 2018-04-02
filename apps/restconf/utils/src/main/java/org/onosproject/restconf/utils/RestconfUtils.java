/*
 * Copyright 2017-present Open Networking Foundation
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

package org.onosproject.restconf.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.io.IOUtils;
import org.onlab.osgi.DefaultServiceDirectory;
import org.onosproject.restconf.api.RestconfError;
import org.onosproject.restconf.api.RestconfException;
import org.onosproject.restconf.api.RestconfRpcOutput;
import org.onosproject.yang.model.DataNode;
import org.onosproject.yang.model.DefaultResourceData;
import org.onosproject.yang.model.ResourceData;
import org.onosproject.yang.model.ResourceId;
import org.onosproject.yang.model.RpcOutput;
import org.onosproject.yang.runtime.CompositeData;
import org.onosproject.yang.runtime.CompositeStream;
import org.onosproject.yang.runtime.DefaultCompositeData;
import org.onosproject.yang.runtime.DefaultCompositeStream;
import org.onosproject.yang.runtime.DefaultRuntimeContext;
import org.onosproject.yang.runtime.RuntimeContext;
import org.onosproject.yang.runtime.YangRuntimeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Optional;

import static javax.ws.rs.core.Response.Status.*;
import static org.onlab.util.Tools.readTreeFromStream;

/**
 * Utilities used by the RESTCONF app.
 */
public final class RestconfUtils {
    /**
     * No instantiation.
     */
    private RestconfUtils() {
    }

    /**
     * Data format required by YangRuntime Service.
     */
    private static final String JSON_FORMAT = "JSON";
    private static final String SLASH = "/";

    private static final YangRuntimeService YANG_RUNTIME =
            DefaultServiceDirectory.getService(YangRuntimeService.class);

    private static final Logger log = LoggerFactory.getLogger(RestconfUtils.class);

    /**
     * Converts an input stream to JSON objectNode.
     *
     * @param inputStream the InputStream from Resource Data
     * @return JSON representation of the data resource
     */
    public static ObjectNode convertInputStreamToObjectNode(InputStream inputStream) {
        ObjectNode rootNode;
        ObjectMapper mapper = new ObjectMapper();
        try {
            rootNode = readTreeFromStream(mapper, inputStream);
        } catch (IOException e) {
            throw new RestconfException("ERROR: InputStream failed to parse",
                    e, RestconfError.ErrorTag.OPERATION_FAILED, INTERNAL_SERVER_ERROR,
                    Optional.empty());
        }
        return rootNode;
    }

    /**
     * Convert ObjectNode to InputStream.
     *
     * @param rootNode JSON representation of the data resource
     * @return the InputStream from Resource Data
     */
    public static InputStream convertObjectNodeToInputStream(ObjectNode rootNode) {
        String json = rootNode.toString();
        InputStream inputStream;
        try {
            inputStream = IOUtils.toInputStream(json);
        } catch (Exception e) {
            throw new RestconfException("ERROR: Json Node failed to parse", e,
                RestconfError.ErrorTag.MALFORMED_MESSAGE, BAD_REQUEST,
                Optional.empty());
        }
        return inputStream;
    }

    /**
     * Convert URI to ResourceId. If the URI represents the datastore resource
     * (i.e., the root of datastore), a null is returned.
     *
     * @param uri URI of the data resource
     * @return resource identifier
     */
    public static ResourceId convertUriToRid(URI uri) {
        ResourceData resourceData = convertJsonToDataNode(uri, null);
        return resourceData.resourceId();
    }

    /**
     * Convert URI and ObjectNode to ResourceData.
     *
     * @param uri      URI of the data resource
     * @param rootNode JSON representation of the data resource
     * @return represents type of node in data store
     */
    public static ResourceData convertJsonToDataNode(URI uri,
                                                     ObjectNode rootNode) {
        RuntimeContext.Builder runtimeContextBuilder = new DefaultRuntimeContext.Builder();
        runtimeContextBuilder.setDataFormat(JSON_FORMAT);
        RuntimeContext context = runtimeContextBuilder.build();
        ResourceData resourceData = null;
        InputStream jsonData = null;
        try {
            if (rootNode != null) {
                jsonData = convertObjectNodeToInputStream(rootNode);
            }
            String uriString = getRawUriPath(uri);

            CompositeStream compositeStream = new DefaultCompositeStream(uriString, jsonData);
            // CompositeStream --- YangRuntimeService ---> CompositeData.
            CompositeData compositeData = YANG_RUNTIME.decode(compositeStream, context);
            resourceData = compositeData.resourceData();
        } catch (RestconfException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("convertJsonToDataNode failure: {}", ex.getMessage(), ex);
            log.info("Failed JSON: \n{}", rootNode);
            log.debug("convertJsonToDataNode failure", ex);
            throw new RestconfException("ERROR: JSON cannot be converted to DataNode",
                    ex, RestconfError.ErrorTag.OPERATION_FAILED, INTERNAL_SERVER_ERROR,
                    Optional.of(uri.getPath()));
        }
        if (resourceData == null) {
            throw new RestconfException("ERROR: JSON cannot be converted to DataNode",
                RestconfError.ErrorTag.DATA_MISSING, CONFLICT,
                Optional.of(uri.getPath()), Optional.empty());
        }
        return resourceData;
    }

    private static String getRawUriPath(URI uri) {
        String path = uri.getRawPath();
        if (path.equals("/onos/restconf/data")) {
            return null;
        }

        return path.replaceAll("^/onos/restconf/data/", "")
                .replaceAll("^/onos/restconf/operations/", "");
    }

    /**
     * Convert Resource Id and Data Node to Json ObjectNode.
     *
     * @param rid      resource identifier
     * @param dataNode represents type of node in data store
     * @return JSON representation of the data resource
     */
    public static ObjectNode convertDataNodeToJson(ResourceId rid, DataNode dataNode) {
        RuntimeContext.Builder runtimeContextBuilder = DefaultRuntimeContext.builder();
        runtimeContextBuilder.setDataFormat(JSON_FORMAT);
        RuntimeContext context = runtimeContextBuilder.build();
        DefaultResourceData.Builder resourceDataBuilder = DefaultResourceData.builder();
        resourceDataBuilder.addDataNode(dataNode);
        resourceDataBuilder.resourceId(rid);
        ResourceData resourceData = resourceDataBuilder.build();
        DefaultCompositeData.Builder compositeDataBuilder = DefaultCompositeData.builder();
        compositeDataBuilder.resourceData(resourceData);
        CompositeData compositeData = compositeDataBuilder.build();
        ObjectNode rootNode = null;
        try {
            // CompositeData --- YangRuntimeService ---> CompositeStream.
            CompositeStream compositeStream = YANG_RUNTIME.encode(compositeData, context);
            InputStream inputStream = compositeStream.resourceData();
            rootNode = convertInputStreamToObjectNode(inputStream);
        } catch (Exception ex) {
            log.error("convertInputStreamToObjectNode failure: {}", ex.getMessage());
            log.debug("convertInputStreamToObjectNode failure", ex);
        }
        if (rootNode == null) {
            throw new RestconfException("ERROR: InputStream can not be convert to ObjectNode",
                    null, RestconfError.ErrorTag.DATA_MISSING, CONFLICT,
                    Optional.empty());
        }
        return rootNode;
    }

    /**
     * Removes the last path segment from the given URI. That is, returns
     * the parent of the given URI.
     *
     * @param uri given URI
     * @return parent URI
     */
    public static URI rmLastPathSegment(URI uri) {
        if (uri == null) {
            return null;
        }

        UriBuilder builder = UriBuilder.fromUri(uri);
        String newPath = rmLastPathSegmentStr(uri.getRawPath());
        builder.replacePath(newPath);

        return builder.build();
    }

    private static String rmLastPathSegmentStr(String rawPath) {
        if (rawPath == null) {
            return null;
        }
        int pos = rawPath.lastIndexOf(SLASH);
        if (pos <= 0) {
            return null;
        }

        return rawPath.substring(0, pos);
    }

    /**
     * Creates a RESTCONF RPC output object from a given YANG RPC output object.
     *
     * @param cmdId     resource ID of the RPC
     * @param rpcOutput given RPC output in YANG format
     * @return RPC output in RESTCONF format
     */
    public static RestconfRpcOutput convertRpcOutput(ResourceId cmdId, RpcOutput rpcOutput) {
        RestconfRpcOutput restconfRpcOutput = new RestconfRpcOutput();

        restconfRpcOutput.status(convertResponseStatus(rpcOutput.status()));
        if (rpcOutput.data() != null) {
            restconfRpcOutput.output(convertDataNodeToJson(cmdId, rpcOutput.data()));
        }

        return restconfRpcOutput;
    }

    private static Response.Status convertResponseStatus(RpcOutput.Status status) {
        switch (status) {
            case RPC_SUCCESS:
                return OK;
            case RPC_FAILURE:
                return EXPECTATION_FAILED;
            case RPC_NODATA:
                return NO_CONTENT;
            case RPC_TIMEOUT:
                return REQUEST_TIMEOUT;
            default:
                return BAD_REQUEST;
        }
    }
}
