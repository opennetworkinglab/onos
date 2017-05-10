/*
 * Copyright 2017-present Open Networking Laboratory
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
import org.onosproject.restconf.api.RestconfException;
import org.onosproject.restconf.utils.exceptions.RestconfUtilsException;
import org.onosproject.yang.model.DataNode;
import org.onosproject.yang.model.ResourceData;
import org.onosproject.yang.model.ResourceId;
import org.onosproject.yang.runtime.CompositeData;
import org.onosproject.yang.runtime.CompositeStream;
import org.onosproject.yang.runtime.DefaultCompositeData;
import org.onosproject.yang.runtime.DefaultCompositeStream;
import org.onosproject.yang.model.DefaultResourceData;
import org.onosproject.yang.runtime.DefaultRuntimeContext;
import org.onosproject.yang.runtime.RuntimeContext;
import org.onosproject.yang.runtime.YangRuntimeService;

import java.io.IOException;
import java.io.InputStream;

import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;

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

    private static final YangRuntimeService YANG_RUNTIME =
            DefaultServiceDirectory.getService(YangRuntimeService.class);

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
            rootNode = (ObjectNode) mapper.readTree(inputStream);
        } catch (IOException e) {
            throw new RestconfUtilsException("ERROR: InputStream failed to parse");
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
            throw new RestconfUtilsException("ERROR: Json Node failed to parse");
        }
        return inputStream;
    }

    /**
     * Convert URI to ResourceId.
     *
     * @param uri URI of the data resource
     * @return resource identifier
     */
    public static ResourceId convertUriToRid(String uri) {
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
    public static ResourceData convertJsonToDataNode(String uri,
                                                     ObjectNode rootNode) {
        RuntimeContext.Builder runtimeContextBuilder = new DefaultRuntimeContext.Builder();
        runtimeContextBuilder.setDataFormat(JSON_FORMAT);
        RuntimeContext context = runtimeContextBuilder.build();
        InputStream jsonData = null;
        if (rootNode != null) {
            jsonData = convertObjectNodeToInputStream(rootNode);
        }
        CompositeStream compositeStream = new DefaultCompositeStream(uri, jsonData);
        // CompositeStream --- YangRuntimeService ---> CompositeData.
        CompositeData compositeData = YANG_RUNTIME.decode(compositeStream, context);
        ResourceData resourceData = compositeData.resourceData();
        return resourceData;
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
        // CompositeData --- YangRuntimeService ---> CompositeStream.
        CompositeStream compositeStream = YANG_RUNTIME.encode(compositeData, context);
        InputStream inputStream = compositeStream.resourceData();
        ObjectNode rootNode = convertInputStreamToObjectNode(inputStream);
        if (rootNode == null) {
            throw new RestconfException("ERROR: InputStream can not be convert to ObjectNode",
                                        INTERNAL_SERVER_ERROR);
        }
        return rootNode;
    }
}
