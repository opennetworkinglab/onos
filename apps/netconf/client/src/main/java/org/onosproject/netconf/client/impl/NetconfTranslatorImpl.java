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

package org.onosproject.netconf.client.impl;

import com.google.common.annotations.Beta;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onosproject.cluster.NodeId;
import org.onosproject.net.DeviceId;
import org.onosproject.netconf.NetconfController;
import org.onosproject.netconf.NetconfDevice;
import org.onosproject.netconf.NetconfException;
import org.onosproject.netconf.NetconfSession;
import org.onosproject.netconf.client.NetconfTranslator;
import org.onosproject.yang.model.DataNode;
import org.onosproject.yang.model.InnerNode;
import org.onosproject.yang.model.KeyLeaf;
import org.onosproject.yang.model.LeafListKey;
import org.onosproject.yang.model.LeafNode;
import org.onosproject.yang.model.ListKey;
import org.onosproject.yang.model.NodeKey;
import org.onosproject.yang.model.ResourceData;
import org.onosproject.yang.model.ResourceId;
import org.onosproject.yang.model.SchemaContext;
import org.onosproject.yang.model.SchemaContextProvider;
import org.onosproject.yang.model.SchemaId;
import org.onosproject.yang.runtime.CompositeStream;
import org.onosproject.yang.runtime.DefaultAnnotatedNodeInfo;
import org.onosproject.yang.runtime.DefaultAnnotation;
import org.onosproject.yang.runtime.DefaultCompositeData;
import org.onosproject.yang.runtime.DefaultCompositeStream;
import org.onosproject.yang.model.DefaultResourceData;
import org.onosproject.yang.runtime.DefaultRuntimeContext;
import org.onosproject.yang.runtime.DefaultYangSerializerContext;
import org.onosproject.yang.runtime.YangRuntimeService;
import org.onosproject.yang.runtime.YangSerializerContext;
import org.onosproject.yang.runtime.SerializerHelper;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onosproject.netconf.TargetConfig.RUNNING;
import static org.onosproject.yang.model.DataNode.Type.SINGLE_INSTANCE_LEAF_VALUE_NODE;
import static org.onosproject.yang.runtime.SerializerHelper.addDataNode;

/*FIXME these imports are not visible using OSGI*/

/*FIXME these imports are not visible using OSGI*/

/*TODO once the API's are finalized this comment will be made more specified.*/

/**
 * Translator which accepts data types defined for the DynamicConfigService and
 * makes the appropriate calls to NETCONF devices before encoding and returning
 * responses in formats suitable for the DynamicConfigService.
 * <p>
 * NOTE: This entity does not ensure you are the master of a device you attempt
 * to contact. If you are not the master an error will be thrown because there
 * will be no session available.
 */
@Beta
@Service
@Component(immediate = true)
public class NetconfTranslatorImpl implements NetconfTranslator {

    private final Logger log = LoggerFactory
            .getLogger(getClass());

    private NodeId localNodeId;

    private static final String GET_CONFIG_MESSAGE_REGEX =
            "<data>\n?\\s*(.*?)\n?\\s*</data>";
    private static final int GET_CONFIG_CORE_MESSAGE_GROUP = 1;
    private static final Pattern GET_CONFIG_CORE_MESSAGE_PATTERN =
            Pattern.compile(GET_CONFIG_MESSAGE_REGEX, Pattern.DOTALL);
    private static final String GET_CORE_MESSAGE_REGEX = "<data>\n?\\s*(.*?)\n?\\s*</data>";
    private static final int GET_CORE_MESSAGE_GROUP = 1;
    private static final Pattern GET_CORE_MESSAGE_PATTERN =
            Pattern.compile(GET_CORE_MESSAGE_REGEX, Pattern.DOTALL);

    private static final String NETCONF_1_0_BASE_NAMESPACE =
            "urn:ietf:params:xml:ns:netconf:base:1.0";

    private static final String GET_URI = "urn:ietf:params:xml:ns:yang:" +
            "yrt-ietf-network:networks/network/node";
    private static final String XML_ENCODING_SPECIFIER = "xml";
    private static final String OP_SPECIFIER = "xc:operation";
    private static final String REPLACE_OP_SPECIFIER = "replace";
    private static final String DELETE_OP_SPECIFIER = "delete";
    private static final String XMLNS_XC_SPECIFIER = "xmlns:xc";
    private static final String XMLNS_SPECIFIER = "xmlns";

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected NetconfController netconfController;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected YangRuntimeService yangRuntimeService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected SchemaContextProvider schemaContextProvider;

    @Activate
    public void activate(ComponentContext context) {
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        log.info("Stopped");
    }

    @Override
    public ResourceData getDeviceConfig(DeviceId deviceId) throws IOException {
        NetconfSession session = getNetconfSession(deviceId);
        /*FIXME "running" will be replaced with an enum once netconf supports multiple datastores.*/
        String reply = session.getConfig(RUNNING);
        Matcher protocolStripper = GET_CONFIG_CORE_MESSAGE_PATTERN.matcher(reply);
        reply = protocolStripper.group(GET_CONFIG_CORE_MESSAGE_GROUP);
        return yangRuntimeService.decode(
                new DefaultCompositeStream(
                        null,
                        /*FIXME is UTF_8 the appropriate encoding? */
                        new ByteArrayInputStream(reply.getBytes(StandardCharsets.UTF_8))),
                new DefaultRuntimeContext.Builder()
                        .setDataFormat(XML_ENCODING_SPECIFIER)
                        .addAnnotation(
                                new DefaultAnnotation(XMLNS_SPECIFIER,
                                                      NETCONF_1_0_BASE_NAMESPACE))
                        .build()).resourceData();
    }

    @Override
    public boolean editDeviceConfig(DeviceId deviceId, ResourceData resourceData,
                                    NetconfTranslator.OperationType operationType) throws IOException {
        NetconfSession session = getNetconfSession(deviceId);
        SchemaContext context = schemaContextProvider
                .getSchemaContext(ResourceId.builder().addBranchPointSchema("/", null).build());
        ResourceData modifiedPathResourceData = getResourceData(resourceData.resourceId(),
                                                                resourceData.dataNodes(),
                                                                new DefaultYangSerializerContext(context, null));
        DefaultCompositeData.Builder compositeDataBuilder = DefaultCompositeData
                .builder()
                .resourceData(modifiedPathResourceData);
        for (DataNode node : resourceData.dataNodes()) {
            ResourceId resourceId = resourceData.resourceId();
            if (operationType != OperationType.DELETE) {
                resourceId = getAnnotatedNodeResourceId(
                        resourceData.resourceId(), node);
            }
            if (resourceId != null) {
                DefaultAnnotatedNodeInfo.Builder annotatedNodeInfo =
                        DefaultAnnotatedNodeInfo.builder();
                annotatedNodeInfo.resourceId(resourceId);
                annotatedNodeInfo.addAnnotation(
                        new DefaultAnnotation(
                                OP_SPECIFIER, operationType == OperationType.DELETE ?
                                DELETE_OP_SPECIFIER : REPLACE_OP_SPECIFIER));
                compositeDataBuilder.addAnnotatedNodeInfo(annotatedNodeInfo.build());
            }
        }
        CompositeStream config = yangRuntimeService.encode(
                compositeDataBuilder.build(),
                new DefaultRuntimeContext.Builder()
                        .setDataFormat(XML_ENCODING_SPECIFIER)
                        .addAnnotation(new DefaultAnnotation(
                                XMLNS_XC_SPECIFIER, NETCONF_1_0_BASE_NAMESPACE))
                        .build());
        /* FIXME need to fix to string conversion. */

        try {
            String reply = session.requestSync(Utils.editConfig(streamToString(
                    config.resourceData())));
        } catch (NetconfException e) {
            log.error("failed to send a request sync", e);
            return false;
        }
            /* NOTE: a failure to edit is reflected as a NetconfException.*/
        return true;
    }

    @Override
    public ResourceData getDeviceState(DeviceId deviceId) throws IOException {
        NetconfSession session = getNetconfSession(deviceId);
        /*TODO the first parameter will come into use if get is required to support filters.*/
        String reply = session.get(null, null);
        Matcher protocolStripper = GET_CORE_MESSAGE_PATTERN.matcher(reply);
        reply = protocolStripper.group(GET_CORE_MESSAGE_GROUP);
        return yangRuntimeService.decode(
                new DefaultCompositeStream(
                        null,
                /*FIXME is UTF_8 the appropriate encoding? */
                        new ByteArrayInputStream(reply.getBytes(StandardCharsets.UTF_8))),
                new DefaultRuntimeContext.Builder()
                        .setDataFormat(XML_ENCODING_SPECIFIER)
                        .addAnnotation(
                                new DefaultAnnotation(
                                        XMLNS_SPECIFIER,
                                        NETCONF_1_0_BASE_NAMESPACE))
                        .build()).resourceData();
        /* NOTE: a failure to get is reflected as a NetconfException.*/
    }

    /**
     * Returns a session for the specified deviceId if this node is its master,
     * returns null otherwise.
     *
     * @param deviceId the id of node for witch we wish to retrieve a session
     * @return a NetconfSession with the specified node or null
     */
    private NetconfSession getNetconfSession(DeviceId deviceId) {
        NetconfDevice device = netconfController.getNetconfDevice(deviceId);
        checkNotNull(device, "The specified deviceId could not be found by the NETCONF controller.");
        NetconfSession session = device.getSession();
        checkNotNull(session, "A session could not be retrieved for the specified deviceId.");
        return session;
    }

    /**
     * Accepts a stream and converts it to a string.
     *
     * @param stream the stream to be converted
     * @return a string with the same sequence of characters as the stream
     * @throws IOException if reading from the stream fails
     */
    private String streamToString(InputStream stream) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
        StringBuilder builder = new StringBuilder();

        String nextLine = reader.readLine();
        while (nextLine != null) {
            builder.append(nextLine);
            nextLine = reader.readLine();
        }
        return builder.toString();
    }

    /**
     * Returns resource data having resource id as "/" and data node tree
     * starting from "/" by creating data nodes for given resource id(parent
     * node for given list of nodes) and list of child nodes.
     * <p>
     * This api will be used in encode flow only.
     *
     * @param rid   resource identifier till parent node
     * @param nodes list of data nodes
     * @param cont  yang serializer context
     * @return resource data.
     */
    public static ResourceData getResourceData(
            ResourceId rid, List<DataNode> nodes, YangSerializerContext cont) {
        if (rid == null) {
            ResourceData.Builder resData = DefaultResourceData.builder();
            for (DataNode node : nodes) {
                resData.addDataNode(node);
            }
            return resData.build();
        }
        List<NodeKey> keys = rid.nodeKeys();
        Iterator<NodeKey> it = keys.iterator();
        DataNode.Builder dbr = SerializerHelper.initializeDataNode(cont);

        // checking the resource id weather it is getting started from / or not

        while (it.hasNext()) {
            NodeKey nodekey = it.next();
            SchemaId sid = nodekey.schemaId();
            dbr = addDataNode(dbr, sid.name(), sid.namespace(),
                              null, null);
            if (nodekey instanceof ListKey) {
                for (KeyLeaf keyLeaf : ((ListKey) nodekey).keyLeafs()) {
                    String val;
                    if (keyLeaf.leafValue() == null) {
                        val = null;
                    } else {
                        val = keyLeaf.leafValAsString();
                    }
                    dbr = addDataNode(dbr, keyLeaf.leafSchema().name(),
                                      sid.namespace(), val,
                                      SINGLE_INSTANCE_LEAF_VALUE_NODE);
                }
            }
        }

        if (dbr instanceof LeafNode.Builder &&
                (nodes != null || !nodes.isEmpty())) {
            //exception "leaf/leaf-list can not have child node"
        }

        if (nodes != null && !nodes.isEmpty()) {
            // adding the parent node for given list of nodes
            for (DataNode node : nodes) {
                dbr = ((InnerNode.Builder) dbr).addNode(node);
            }
        }
/*FIXME this can be uncommented for use with versions of onos-yang-tools newer than 1.12.0-b6*/
//        while (dbr.parent() != null) {
//            dbr = SerializerHelper.exitDataNode(dbr);
//        }

        ResourceData.Builder resData = DefaultResourceData.builder();

        resData.addDataNode(dbr.build());
        resData.resourceId(null);
        return resData.build();
    }

    /**
     * Returns resource id for annotated data node by adding resource id of top
     * level data node to given resource id.
     * <p>
     * Annotation will be added to node based on the updated resource id.
     * This api will be used in encode flow only.
     *
     * @param rid  resource identifier till parent node
     * @param node data node
     * @return updated resource id.
     */
    public static ResourceId getAnnotatedNodeResourceId(ResourceId rid,
                                                        DataNode node) {

        String val;
        ResourceId.Builder rIdBldr = ResourceId.builder();
        if (rid != null) {
            try {
                rIdBldr = rid.copyBuilder();
            } catch (CloneNotSupportedException e) {
                e.printStackTrace();
            }
        } else {
            rIdBldr.addBranchPointSchema("/", null);
        }
        DataNode.Type type = node.type();
        NodeKey k = node.key();
        SchemaId sid = k.schemaId();

        switch (type) {

            case MULTI_INSTANCE_LEAF_VALUE_NODE:
                val = ((LeafListKey) k).value().toString();
                rIdBldr.addLeafListBranchPoint(sid.name(), sid.namespace(), val);
                break;

            case MULTI_INSTANCE_NODE:
                rIdBldr.addBranchPointSchema(sid.name(), sid.namespace());
// Preparing the list of key values for multiInstanceNode
                for (KeyLeaf keyLeaf : ((ListKey) node.key()).keyLeafs()) {
                    val = keyLeaf.leafValAsString();
                    rIdBldr.addKeyLeaf(keyLeaf.leafSchema().name(), sid.namespace(), val);
                }
                break;
            case SINGLE_INSTANCE_LEAF_VALUE_NODE:
            case SINGLE_INSTANCE_NODE:
                rIdBldr.addBranchPointSchema(sid.name(), sid.namespace());
                break;

            default:
                throw new IllegalArgumentException();
        }
        return rIdBldr.build();
    }
}
