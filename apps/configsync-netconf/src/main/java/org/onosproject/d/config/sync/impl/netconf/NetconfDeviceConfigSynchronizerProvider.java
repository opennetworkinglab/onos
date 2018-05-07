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
package org.onosproject.d.config.sync.impl.netconf;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.CompletableFuture;
import org.onlab.util.XmlString;
import org.onosproject.d.config.ResourceIds;
import org.onosproject.d.config.sync.DeviceConfigSynchronizationProvider;
import org.onosproject.d.config.sync.impl.netconf.NetconfDeviceConfigSynchronizerComponent.NetconfContext;
import org.onosproject.d.config.sync.operation.SetRequest;
import org.onosproject.d.config.sync.operation.SetRequest.Change;
import org.onosproject.d.config.sync.operation.SetRequest.Change.Operation;
import org.onosproject.d.config.sync.operation.SetResponse;
import org.onosproject.d.config.sync.operation.SetResponse.Code;
import org.onosproject.net.DeviceId;
import org.onosproject.net.provider.AbstractProvider;
import org.onosproject.net.provider.ProviderId;
import org.onosproject.netconf.NetconfDevice;
import org.onosproject.netconf.NetconfException;
import org.onosproject.netconf.NetconfSession;
import org.onosproject.yang.model.DataNode;
import org.onosproject.yang.model.DefaultResourceData;
import org.onosproject.yang.model.InnerNode;
import org.onosproject.yang.model.ResourceData;
import org.onosproject.yang.model.ResourceId;
import org.onosproject.yang.runtime.AnnotatedNodeInfo;
import org.onosproject.yang.runtime.Annotation;
import org.onosproject.yang.runtime.CompositeData;
import org.onosproject.yang.runtime.CompositeStream;
import org.onosproject.yang.runtime.DefaultAnnotatedNodeInfo;
import org.onosproject.yang.runtime.DefaultAnnotation;
import org.onosproject.yang.runtime.DefaultCompositeData;
import org.onosproject.yang.runtime.DefaultRuntimeContext;
import org.onosproject.yang.runtime.RuntimeContext;
import org.slf4j.Logger;
import com.google.common.io.CharStreams;

/**
 * Dynamic config synchronizer provider for NETCONF.
 *
 * <ul>
 * <li> Converts POJO YANG into XML.
 * <li> Adds NETCONF envelope around it.
 * <li> Send request down to the device over NETCONF
 * </ul>
 */
public class NetconfDeviceConfigSynchronizerProvider
        extends AbstractProvider
        implements DeviceConfigSynchronizationProvider {

    private static final Logger log = getLogger(NetconfDeviceConfigSynchronizerProvider.class);

    // TODO this should probably be defined on YRT Serializer side
    /**
     * {@link RuntimeContext} parameter Dataformat specifying XML.
     */
    private static final String DATAFORMAT_XML = "xml";

    private static final String XMLNS_XC = "xmlns:xc";
    private static final String NETCONF_1_0_BASE_NAMESPACE =
                                    "urn:ietf:params:xml:ns:netconf:base:1.0";

    /**
     * Annotation to add xc namespace declaration.
     * {@value #XMLNS_XC}={@value #NETCONF_1_0_BASE_NAMESPACE}
     */
    private static final DefaultAnnotation XMLNS_XC_ANNOTATION =
                    new DefaultAnnotation(XMLNS_XC, NETCONF_1_0_BASE_NAMESPACE);

    private static final String XC_OPERATION = "xc:operation";


    private NetconfContext context;

    protected NetconfDeviceConfigSynchronizerProvider(ProviderId id,
                                                      NetconfContext context) {
        super(id);
        this.context = checkNotNull(context);
    }

    @Override
    public CompletableFuture<SetResponse> setConfiguration(DeviceId deviceId,
                                                           SetRequest request) {
        // sanity check and handle empty change?

        // TODOs:
        // - Construct convert request object into XML
        // --  [FutureWork] may need to introduce behaviour for Device specific
        //     workaround insertion

        StringBuilder rpc = new StringBuilder();

        // - Add NETCONF envelope
        rpc.append("<rpc xmlns=\"").append(NETCONF_1_0_BASE_NAMESPACE).append('"')
            .append(">");

        rpc.append("<edit-config>");
        rpc.append("<target>");
        // TODO directly writing to running for now
        rpc.append("<running/>");
        rpc.append("</target>\n");
        rpc.append("<config ")
            .append(XMLNS_XC).append("=\"").append(NETCONF_1_0_BASE_NAMESPACE).append("\">");
        // TODO netconf SBI should probably be adding these envelopes once
        // netconf SBI is in better shape
        // TODO In such case netconf sbi need to define namespace externally visible.
        // ("xc" in above instance)
        // to be used to add operations on config tree nodes


        // Convert change(s) into a DataNode tree
        for (Change change : request.changes()) {
            log.trace("change={}", change);

            // TODO switch statement can probably be removed
            switch (change.op()) {
            case REPLACE:
            case UPDATE:
            case DELETE:
                // convert DataNode -> ResourceData
                ResourceData data = toResourceData(change);

                // build CompositeData
                DefaultCompositeData.Builder compositeData =
                                        DefaultCompositeData.builder();

                // add ResourceData
                compositeData.resourceData(data);

                // add AnnotatedNodeInfo operation
                compositeData.addAnnotatedNodeInfo(toAnnotatedNodeInfo(change.op(), change.path()));

                RuntimeContext yrtContext = new DefaultRuntimeContext.Builder()
                                           .setDataFormat(DATAFORMAT_XML)
                                           .addAnnotation(XMLNS_XC_ANNOTATION)
                                           .build();
                CompositeData cdata = compositeData.build();
                log.trace("CompositeData:{}", cdata);
                CompositeStream xml = context.yangRuntime().encode(cdata,
                                                                   yrtContext);
                try {
                    CharStreams.copy(new InputStreamReader(xml.resourceData(), UTF_8), rpc);
                } catch (IOException e) {
                    log.error("IOException thrown", e);
                    // FIXME handle error
                }
                break;

            default:
                log.error("Should never reach here. {}", change);
                break;
            }
        }

        // - close NETCONF envelope
        // TODO eventually these should be handled by NETCONF SBI side
        rpc.append('\n');
        rpc.append("</config>");
        rpc.append("</edit-config>");
        rpc.append("</rpc>");

        // - send requests down to the device
        NetconfSession session = getNetconfSession(deviceId);
        if (session == null) {
            log.error("No session available for {}", deviceId);
            return completedFuture(SetResponse.response(request,
                                                        Code.FAILED_PRECONDITION,
                                                        "No session for " + deviceId));
        }
        try {
            // FIXME Netconf async API is currently screwed up, need to fix
            // NetconfSession, etc.
            CompletableFuture<String> response = session.rpc(rpc.toString());
            log.trace("raw request:\n{}", rpc);
            log.trace("prettified request:\n{}", XmlString.prettifyXml(rpc));
            return response.handle((resp, err) -> {
                if (err == null) {
                    log.trace("reply:\n{}", XmlString.prettifyXml(resp));
                    // FIXME check response properly
                    return SetResponse.ok(request);
                } else {
                    return SetResponse.response(request, Code.UNKNOWN, err.getMessage());
                }
            });
        } catch (NetconfException e) {
            // TODO Handle error
            log.error("NetconfException thrown", e);
            return completedFuture(SetResponse.response(request, Code.UNKNOWN, e.getMessage()));

        }
    }

    // overridable for ease of testing
    /**
     * Returns a session for the specified deviceId.
     *
     * @param deviceId for which we wish to retrieve a session
     * @return a NetconfSession with the specified node
     * or null if this node does not have the session to the specified Device.
     */
    protected NetconfSession getNetconfSession(DeviceId deviceId) {
        NetconfDevice device = context.netconfController().getNetconfDevice(deviceId);
        checkNotNull(device, "The specified deviceId could not be found by the NETCONF controller.");
        NetconfSession session = device.getSession();
        checkNotNull(session, "A session could not be retrieved for the specified deviceId.");
        return session;
    }

    /**
     * Creates AnnotatedNodeInfo for {@code node}.
     *
     * @param op operation
     * @param parent resourceId
     * @param node the node
     * @return AnnotatedNodeInfo
     */
    static AnnotatedNodeInfo annotatedNodeInfo(Operation op,
                                               ResourceId parent,
                                               DataNode node) {
        return DefaultAnnotatedNodeInfo.builder()
                .resourceId(ResourceIds.resourceId(parent, node))
                .addAnnotation(toAnnotation(op))
                .build();
    }

    /**
     * Creates AnnotatedNodeInfo for specified resource path.
     *
     * @param op operation
     * @param path resourceId
     * @return AnnotatedNodeInfo
     */
    static AnnotatedNodeInfo toAnnotatedNodeInfo(Operation op,
                                               ResourceId path) {
        return DefaultAnnotatedNodeInfo.builder()
                .resourceId(path)
                .addAnnotation(toAnnotation(op))
                .build();
    }

    /**
     * Transform DataNode into a ResourceData.
     *
     * @param change object
     * @return ResourceData
     */
    static ResourceData toResourceData(Change change) {
        DefaultResourceData.Builder builder = DefaultResourceData.builder();
        builder.resourceId(change.path());
        if (change.op() != Change.Operation.DELETE) {
            DataNode dataNode = change.val();
            if (dataNode instanceof InnerNode) {
                ((InnerNode) dataNode).childNodes().values().forEach(builder::addDataNode);
            } else {
                log.error("Unexpected DataNode encountered", change);
            }
        }

        return builder.build();
    }

    static Annotation toAnnotation(Operation op) {
        switch (op) {
        case DELETE:
            return new DefaultAnnotation(XC_OPERATION, "remove");
        case REPLACE:
            return new DefaultAnnotation(XC_OPERATION, "replace");
        case UPDATE:
            return new DefaultAnnotation(XC_OPERATION, "merge");
        default:
            throw new IllegalArgumentException("Unknown operation " + op);
        }
    }

}
