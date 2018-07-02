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

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.stringContainsInOrder;
import static org.junit.Assert.*;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.input.ReaderInputStream;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.onlab.util.XmlString;
import org.onosproject.d.config.sync.DeviceConfigSynchronizationProviderService;
import org.onosproject.d.config.sync.impl.netconf.NetconfDeviceConfigSynchronizerComponent.NetconfContext;
import org.onosproject.d.config.sync.operation.SetRequest;
import org.onosproject.d.config.sync.operation.SetResponse;
import org.onosproject.d.config.sync.operation.SetResponse.Code;
import org.onosproject.net.DeviceId;
import org.onosproject.net.provider.ProviderId;
import org.onosproject.netconf.NetconfController;
import org.onosproject.netconf.NetconfException;
import org.onosproject.netconf.NetconfSession;
import org.onosproject.netconf.NetconfSessionAdapter;
import org.onosproject.yang.model.DataNode;
import org.onosproject.yang.model.DataNode.Type;
import org.onosproject.yang.model.InnerNode;
import org.onosproject.yang.model.LeafNode;
import org.onosproject.yang.model.ResourceData;
import org.onosproject.yang.model.ResourceId;
import org.onosproject.yang.model.SchemaContextProvider;
import org.onosproject.yang.runtime.AnnotatedNodeInfo;
import org.onosproject.yang.runtime.CompositeData;
import org.onosproject.yang.runtime.CompositeStream;
import org.onosproject.yang.runtime.DefaultAnnotatedNodeInfo;
import org.onosproject.yang.runtime.DefaultAnnotation;
import org.onosproject.yang.runtime.DefaultCompositeStream;
import org.onosproject.yang.runtime.RuntimeContext;
import org.onosproject.yang.runtime.YangRuntimeService;
import com.google.common.io.CharSource;

public class NetconfDeviceConfigSynchronizerProviderTest {

    private static final ProviderId PID = new ProviderId("netconf", "test");
    private static final DeviceId DID = DeviceId.deviceId("netconf:testDevice");

    private static final String XMLNS_XC = "xmlns:xc";
    private static final String NETCONF_1_0_BASE_NAMESPACE =
                                    "urn:ietf:params:xml:ns:netconf:base:1.0";

    private static final DefaultAnnotation XC_ANNOTATION =
            new DefaultAnnotation(XMLNS_XC, NETCONF_1_0_BASE_NAMESPACE);

    private static final DefaultAnnotation AN_XC_REPLACE_OPERATION =
                        new DefaultAnnotation("xc:operation", "replace");

    private static final DefaultAnnotation AN_XC_REMOVE_OPERATION =
            new DefaultAnnotation("xc:operation", "remove");

    /**
     *  Yang namespace for test config data.
     */
    private static final String TEST_NS = "testNS";

    private static final ResourceId RID_INTERFACES =
            ResourceId.builder().addBranchPointSchema("interfaces", TEST_NS).build();

    private NetconfDeviceConfigSynchronizerProvider sut;

    private NetconfContext ncCtx;


    // Set following accordingly to suite test scenario
    NetconfSession testNcSession;
    YangRuntimeService testYangRuntime;


    @Before
    public void setUp() throws Exception {

        ncCtx = new TestNetconfContext();

        sut = new NetconfDeviceConfigSynchronizerProvider(PID, ncCtx) {
            // overriding to avoid mocking whole NetconController and all that.
            @Override
            protected NetconfSession getNetconfSession(DeviceId deviceId) {
                assertEquals(DID, deviceId);
                return testNcSession;
            }
        };
    }

    @Test
    public void testReplaceOperation() throws Exception {
        // plug drivers with assertions
        testYangRuntime = onEncode((data, context) -> {
            assertEquals("xml", context.getDataFormat());
            assertThat(context.getProtocolAnnotations(), hasItem(XC_ANNOTATION));

            //  assert CompositeData
            ResourceData rData = data.resourceData();
            List<AnnotatedNodeInfo> infos = data.annotatedNodesInfo();

            ResourceId interfacesRid = RID_INTERFACES;
            AnnotatedNodeInfo intfsAnnot = DefaultAnnotatedNodeInfo.builder()
                    .resourceId(interfacesRid)
                    .addAnnotation(AN_XC_REPLACE_OPERATION)
                    .build();
            assertThat("interfaces has replace operation", infos, hasItem(intfsAnnot));

            // assertion for ResourceData.
            assertEquals(RID_INTERFACES, rData.resourceId());
            assertThat("has 1 child", rData.dataNodes(), hasSize(1));
            assertThat("which is interface",
                           rData.dataNodes().get(0).key().schemaId().name(),
                           is("interface"));
            // todo: assert the rest of the tree if it make sense.

            // FIXME it's unclear what URI is expected here
            String id = URI.create("netconf:testDevice").toString();

            String inXml = deviceConfigAsXml("replace");

            return toCompositeStream(id, inXml);
        });
        testNcSession = new TestEditNetconfSession();


        // building test data
        ResourceId interfacesId = RID_INTERFACES;
        DataNode interfaces = deviceConfigNode();
        SetRequest request = SetRequest.builder()
                .replace(interfacesId, interfaces)
                .build();

        // test start
        CompletableFuture<SetResponse> f = sut.setConfiguration(DID, request);
        SetResponse response = f.get(5, TimeUnit.MINUTES);

        assertEquals(Code.OK, response.code());
        assertEquals(request.subjects(), response.subjects());
    }


    @Test
    public void testDeleteOperation() throws Exception {
        // plug drivers with assertions
        testYangRuntime = onEncode((data, context) -> {
            assertEquals("xml", context.getDataFormat());
            assertThat(context.getProtocolAnnotations(), hasItem(XC_ANNOTATION));

            //  assert CompositeData
            ResourceData rData = data.resourceData();
            List<AnnotatedNodeInfo> infos = data.annotatedNodesInfo();

            ResourceId interfacesRid = RID_INTERFACES;
            AnnotatedNodeInfo intfsAnnot = DefaultAnnotatedNodeInfo.builder()
                    .resourceId(interfacesRid)
                    .addAnnotation(AN_XC_REMOVE_OPERATION)
                    .build();
            assertThat("interfaces has replace operation", infos, hasItem(intfsAnnot));

            // assertion for ResourceData.
            assertEquals(RID_INTERFACES, rData.resourceId());
            assertThat("has no child", rData.dataNodes(), hasSize(0));

            // FIXME it's unclear what URI is expected here
            String id = URI.create("netconf:testDevice").toString();

            String inXml = deviceConfigAsXml("remove");

            return toCompositeStream(id, inXml);
        });
        testNcSession = new TestEditNetconfSession();

        // building test data
        ResourceId interfacesId = RID_INTERFACES;
        SetRequest request = SetRequest.builder()
                .delete(interfacesId)
                .build();

        // test start
        CompletableFuture<SetResponse> f = sut.setConfiguration(DID, request);

        SetResponse response = f.get(5, TimeUnit.MINUTES);
        assertEquals(Code.OK, response.code());
        assertEquals(request.subjects(), response.subjects());
    }

    /**
     * DataNode for testing.
     *
     * <pre>
     *   +-interfaces
     *      |
     *      +- interface{intf-name="en0"}
     *           |
     *           +- speed = "10G"
     *           +- state = "up"
     *
     * </pre>
     * @return DataNode
     */
    private DataNode deviceConfigNode() {
        InnerNode.Builder intfs = InnerNode.builder("interfaces", TEST_NS);
        intfs.type(Type.SINGLE_INSTANCE_NODE);
        InnerNode.Builder intf = intfs.createChildBuilder("interface", TEST_NS);
        intf.type(Type.SINGLE_INSTANCE_LEAF_VALUE_NODE);
        intf.addKeyLeaf("name", TEST_NS, "Ethernet0/0");
        LeafNode.Builder speed = intf.createChildBuilder("mtu", TEST_NS, "1500");
        speed.type(Type.SINGLE_INSTANCE_LEAF_VALUE_NODE);

        intf.addNode(speed.build());
        intfs.addNode(intf.build());
        return intfs.build();
    }

    /**
     * {@link #deviceConfigNode()} as XML.
     *
     * @param operation xc:operation value on {@code interfaces} node
     * @return XML
     */
    private String deviceConfigAsXml(String operation) {
        return  "<interfaces xmlns=\"http://example.com/schema/1.2/config\""
                + " xc:operation=\"" + operation + "\">\n" +
                "  <interface>\n" +
                "    <name>Ethernet0/0</name>\n" +
                "    <mtu>1500</mtu>\n" +
                "  </interface>\n" +
                "</interfaces>";
    }

    private String rpcReplyOk(int messageid) {
        return "<rpc-reply message-id=\"" + messageid + "\"\n" +
               "      xmlns=\"urn:ietf:params:xml:ns:netconf:base:1.0\">\n" +
               "   <ok/>\n" +
               "</rpc-reply>";
    }

    private int fetchMessageId(String request) {
        int messageid;
        Pattern msgId = Pattern.compile("message-id=['\"]([0-9]+)['\"]");
        Matcher matcher = msgId.matcher(request);
        if (matcher.find()) {
            messageid = Integer.parseInt(matcher.group(1));
        } else {
            messageid = -1;
        }
        return messageid;
    }


    protected CompositeStream toCompositeStream(String id, String inXml) {
        try {
            InputStream xml = new ReaderInputStream(
                         CharSource.wrap(inXml)
                             .openStream());

            return new DefaultCompositeStream(id, xml);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Asserts that it received edit-config message and reply Ok.
     */
    private class TestEditNetconfSession extends NetconfSessionAdapter {
        @Override
        public CompletableFuture<String> rpc(String request)
                throws NetconfException {
            System.out.println("TestEditNetconfSession received:");
            System.out.println(XmlString.prettifyXml(request));

            // Extremely naive request rpc message check
            assertThat(request, stringContainsInOrder(Arrays.asList(
                                  "<rpc",
                                  "<edit-config",
                                  "<target",
                                  "<config",

                                  "</config>",
                                  "</edit-config>",
                                  "</rpc>")));

            assertThat("XML namespace decl exists",
                       request, Matchers.containsString("xmlns:xc"));

            assertThat("netconf operation exists",
                       request, Matchers.containsString("xc:operation"));

            return CompletableFuture.completedFuture(rpcReplyOk(fetchMessageId(request)));
        }
    }

    /**
     * Creates mock YangRuntimeService.
     *
     * @param body to execute when {@link YangRuntimeService#encode(CompositeData, RuntimeContext)} was called.
     * @return YangRuntimeService instance
     */
    TestYangRuntimeService onEncode(BiFunction<CompositeData, RuntimeContext, CompositeStream> body) {
        return new TestYangRuntimeService() {
            @Override
            public CompositeStream encode(CompositeData internal,
                                          RuntimeContext context) {
                return body.apply(internal, context);
            }
        };
    }

    private abstract class TestYangRuntimeService implements YangRuntimeService {

        @Override
        public CompositeStream encode(CompositeData internal,
                                      RuntimeContext context) {
            fail("stub not implemented");
            return null;
        }
        @Override
        public CompositeData decode(CompositeStream external,
                                    RuntimeContext context) {
            fail("stub not implemented");
            return null;
        }
    }

    private final class TestNetconfContext implements NetconfContext {
        @Override
        public DeviceConfigSynchronizationProviderService providerService() {
            fail("Add stub driver as necessary");
            return null;
        }

        @Override
        public SchemaContextProvider schemaContextProvider() {
            fail("Add stub driver as necessary");
            return null;
        }

        @Override
        public YangRuntimeService yangRuntime() {
            return testYangRuntime;
        }

        @Override
        public NetconfController netconfController() {
            fail("Add stub driver as necessary");
            return null;
        }
    }

}
