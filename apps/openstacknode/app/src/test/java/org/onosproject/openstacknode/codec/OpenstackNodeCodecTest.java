/*
 * Copyright 2018-present Open Networking Foundation
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
package org.onosproject.openstacknode.codec;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableList;
import org.hamcrest.MatcherAssert;
import org.junit.Before;
import org.junit.Test;
import org.onlab.packet.IpAddress;
import org.onosproject.codec.CodecContext;
import org.onosproject.codec.JsonCodec;
import org.onosproject.codec.impl.CodecManager;
import org.onosproject.core.CoreService;
import org.onosproject.net.DeviceId;
import org.onosproject.net.behaviour.ControllerInfo;
import org.onosproject.openstacknode.api.DefaultOpenstackAuth;
import org.onosproject.openstacknode.api.DefaultOpenstackNode;
import org.onosproject.openstacknode.api.DpdkConfig;
import org.onosproject.openstacknode.api.DpdkInterface;
import org.onosproject.openstacknode.api.KeystoneConfig;
import org.onosproject.openstacknode.api.NeutronConfig;
import org.onosproject.openstacknode.api.NodeState;
import org.onosproject.openstacknode.api.OpenstackAuth;
import org.onosproject.openstacknode.api.OpenstackNode;
import org.onosproject.openstacknode.api.OpenstackPhyInterface;
import org.onosproject.openstacknode.api.OpenstackSshAuth;
import org.onosproject.openstacknode.api.DefaultDpdkConfig;
import org.onosproject.openstacknode.api.DefaultDpdkInterface;
import org.onosproject.openstacknode.api.DefaultKeystoneConfig;
import org.onosproject.openstacknode.api.DefaultNeutronConfig;
import org.onosproject.openstacknode.api.DefaultOpenstackPhyInterface;
import org.onosproject.openstacknode.api.DefaultOpenstackSshAuth;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.onosproject.net.NetTestTools.APP_ID;
import static org.onosproject.openstacknode.codec.OpenstackNodeJsonMatcher.matchesOpenstackNode;

/**
 * Unit tests for OpenstackNode codec.
 */
public class OpenstackNodeCodecTest {
    MockCodecContext context;

    JsonCodec<OpenstackNode> openstackNodeCodec;
    JsonCodec<OpenstackPhyInterface> openstackPhyIntfJsonCodec;
    JsonCodec<ControllerInfo> openstackControllerJsonCodec;
    JsonCodec<OpenstackAuth> openstackAuthJsonCodec;
    JsonCodec<OpenstackSshAuth> openstackSshAuthJsonCodec;
    JsonCodec<DpdkConfig> dpdkConfigJsonCodec;
    JsonCodec<DpdkInterface> dpdkInterfaceJsonCodec;
    JsonCodec<KeystoneConfig> keystoneConfigJsonCodec;
    JsonCodec<NeutronConfig> neutronConfigJsonCodec;

    final CoreService mockCoreService = createMock(CoreService.class);
    private static final String REST_APP_ID = "org.onosproject.rest";

    @Before
    public void setUp() {
        context = new MockCodecContext();
        openstackNodeCodec = new OpenstackNodeCodec();
        openstackPhyIntfJsonCodec = new OpenstackPhyInterfaceCodec();
        openstackControllerJsonCodec = new OpenstackControllerCodec();
        openstackAuthJsonCodec = new OpenstackAuthCodec();
        openstackSshAuthJsonCodec = new OpenstackSshAuthCodec();
        dpdkConfigJsonCodec = new DpdkConfigCodec();
        dpdkInterfaceJsonCodec = new DpdkInterfaceCodec();
        keystoneConfigJsonCodec = new KeystoneConfigCodec();
        neutronConfigJsonCodec = new NeutronConfigCodec();

        assertThat(openstackNodeCodec, notNullValue());
        assertThat(openstackPhyIntfJsonCodec, notNullValue());
        assertThat(openstackControllerJsonCodec, notNullValue());
        assertThat(openstackAuthJsonCodec, notNullValue());
        assertThat(openstackSshAuthJsonCodec, notNullValue());
        assertThat(dpdkConfigJsonCodec, notNullValue());
        assertThat(dpdkInterfaceJsonCodec, notNullValue());
        assertThat(keystoneConfigJsonCodec, notNullValue());
        assertThat(neutronConfigJsonCodec, notNullValue());

        expect(mockCoreService.registerApplication(REST_APP_ID))
                .andReturn(APP_ID).anyTimes();
        replay(mockCoreService);
        context.registerService(CoreService.class, mockCoreService);
    }

    /**
     * Tests the openstack compute node encoding.
     */
    @Test
    public void testOpenstackComputeNodeEncode() {

        OpenstackPhyInterface phyIntf1 = DefaultOpenstackPhyInterface.builder()
                                .network("mgmtnetwork")
                                .intf("eth3")
                                .build();
        OpenstackPhyInterface phyIntf2 = DefaultOpenstackPhyInterface.builder()
                                .network("oamnetwork")
                                .intf("eth4")
                                .build();

        OpenstackSshAuth sshAuth = DefaultOpenstackSshAuth.builder()
                .id("sdn")
                .password("sdn")
                .build();

        ControllerInfo controller1 =
                new ControllerInfo(IpAddress.valueOf("10.10.10.2"), 6653, "tcp");
        ControllerInfo controller2 =
                new ControllerInfo(IpAddress.valueOf("10.10.10.3"), 6663, "tcp");

        OpenstackNode node = DefaultOpenstackNode.builder()
                                .hostname("compute")
                                .type(OpenstackNode.NodeType.COMPUTE)
                                .state(NodeState.INIT)
                                .managementIp(IpAddress.valueOf("10.10.10.1"))
                                .intgBridge(DeviceId.deviceId("br-int"))
                                .vlanIntf("vlan")
                                .dataIp(IpAddress.valueOf("20.20.20.2"))
                                .phyIntfs(ImmutableList.of(phyIntf1, phyIntf2))
                                .controllers(ImmutableList.of(controller1, controller2))
                                .sshAuthInfo(sshAuth)
                                .build();

        ObjectNode nodeJson = openstackNodeCodec.encode(node, context);
        assertThat(nodeJson, matchesOpenstackNode(node));
    }

    /**
     * Tests the openstack compute node decoding.
     *
     * @throws IOException io exception
     */
    @Test
    public void testOpenstackComputeNodeDecode() throws IOException {
        OpenstackNode node = getOpenstackNode("OpenstackComputeNode.json");

        assertThat(node.hostname(), is("compute-01"));
        assertThat(node.type().name(), is("COMPUTE"));
        assertThat(node.managementIp().toString(), is("172.16.130.4"));
        assertThat(node.dataIp().toString(), is("172.16.130.4"));
        assertThat(node.intgBridge().toString(), is("of:00000000000000a1"));
        assertThat(node.vlanIntf(), is("eth2"));
        assertThat(node.phyIntfs().size(), is(2));
        assertThat(node.controllers().size(), is(2));
        assertThat(node.sshAuthInfo().id(), is("sdn"));
        assertThat(node.sshAuthInfo().password(), is("sdn"));
        assertThat(node.datapathType(), is(DpdkConfig.DatapathType.NORMAL));


        node.phyIntfs().forEach(intf -> {
            if (intf.network().equals("mgmtnetwork")) {
                assertThat(intf.intf(), is("eth3"));
            }
            if (intf.network().equals("oamnetwork")) {
                assertThat(intf.intf(), is("eth4"));
            }
        });

        node.controllers().forEach(ctrl -> {
            if (ctrl.ip().toString().equals("10.10.10.2")) {
                assertThat(ctrl.port(), is(6653));
            }
            if (ctrl.ip().toString().equals("10.10.10.3")) {
                assertThat(ctrl.port(), is(6663));
            }
        });
    }

    /**
     * Tests the openstack compute node encoding with dpdk config.
     */
    @Test
    public void testOpenstackDpdkComputeNodeEncode() {
        DpdkInterface dpdkInterface1 = DefaultDpdkInterface.builder()
                .deviceName("br-int")
                .intf("dpdk0")
                .mtu(Long.valueOf(1600))
                .pciAddress("0000:85:00.0")
                .type(DpdkInterface.Type.DPDK)
                .build();

        DpdkInterface dpdkInterface2 = DefaultDpdkInterface.builder()
                .deviceName("br-tun")
                .intf("dpdk1")
                .pciAddress("0000:85:00.1")
                .type(DpdkInterface.Type.DPDK)
                .build();

        Collection<DpdkInterface> dpdkInterfaceCollection = new ArrayList<>();
        dpdkInterfaceCollection.add(dpdkInterface1);
        dpdkInterfaceCollection.add(dpdkInterface2);


        DpdkConfig dpdkConfig = DefaultDpdkConfig.builder()
                .datapathType(DpdkConfig.DatapathType.NETDEV)
                .dpdkIntfs(dpdkInterfaceCollection)
                .build();

        OpenstackNode node = DefaultOpenstackNode.builder()
                .hostname("compute")
                .type(OpenstackNode.NodeType.COMPUTE)
                .state(NodeState.INIT)
                .managementIp(IpAddress.valueOf("10.10.10.1"))
                .intgBridge(DeviceId.deviceId("br-int"))
                .vlanIntf("vlan")
                .dataIp(IpAddress.valueOf("20.20.20.2"))
                .dpdkConfig(dpdkConfig)
                .build();

        ObjectNode nodeJson = openstackNodeCodec.encode(node, context);
        assertThat(nodeJson, matchesOpenstackNode(node));
    }

    /**
     * Tests the openstack compute node decoding with dpdk config.
     *
     * @throws IOException io exception
     */
    @Test
    public void testOpenstackDpdkComputeNodeDecode() throws IOException {
        OpenstackNode node = getOpenstackNode("OpenstackDpdkComputeNode.json");

        assertEquals(node.datapathType(), DpdkConfig.DatapathType.NETDEV);
        assertEquals(node.socketDir(), "/var/lib/libvirt/qemu");
        assertEquals(node.dpdkConfig().dpdkIntfs().size(), 2);
    }

    /**
     * Tests the openstack controller node encoding.
     */
    @Test
    public void testOpenstackControllerNodeEncode() {
        OpenstackAuth auth = DefaultOpenstackAuth.builder()
                .version("v2.0")
                .protocol(OpenstackAuth.Protocol.HTTP)
                .project("admin")
                .username("admin")
                .password("nova")
                .perspective(OpenstackAuth.Perspective.PUBLIC)
                .build();

        String endpoint = "172.16.130.10:35357/v2.0";

        KeystoneConfig keystoneConfig = DefaultKeystoneConfig.builder()
                                            .endpoint(endpoint)
                                            .authentication(auth)
                                            .build();

        NeutronConfig neutronConfig = DefaultNeutronConfig.builder()
                                            .useMetadataProxy(true)
                                            .metadataProxySecret("onos")
                                            .novaMetadataIp("172.16.130.10")
                                            .novaMetadataPort(8775)
                                            .build();

        OpenstackNode node = DefaultOpenstackNode.builder()
                .hostname("controller")
                .type(OpenstackNode.NodeType.CONTROLLER)
                .state(NodeState.INIT)
                .managementIp(IpAddress.valueOf("172.16.130.10"))
                .keystoneConfig(keystoneConfig)
                .neutronConfig(neutronConfig)
                .build();

        ObjectNode nodeJson = openstackNodeCodec.encode(node, context);
        assertThat(nodeJson, matchesOpenstackNode(node));
    }

    /**
     * Tests the openstack controller node decoding.
     */
    @Test
    public void testOpenstackControllerNodeDecode() throws IOException {
        OpenstackNode node = getOpenstackNode("OpenstackControllerNode.json");

        assertThat(node.hostname(), is("controller"));
        assertThat(node.type().name(), is("CONTROLLER"));
        assertThat(node.managementIp().toString(), is("172.16.130.10"));

        KeystoneConfig keystoneConfig = node.keystoneConfig();
        OpenstackAuth auth = keystoneConfig.authentication();
        String endpoint = keystoneConfig.endpoint();

        assertThat(auth.version(), is("v2.0"));
        assertThat(auth.protocol(), is(OpenstackAuth.Protocol.HTTP));
        assertThat(auth.username(), is("admin"));
        assertThat(auth.password(), is("nova"));
        assertThat(auth.project(), is("admin"));
        assertThat(auth.perspective(), is(OpenstackAuth.Perspective.PUBLIC));

        assertThat(endpoint, is("172.16.130.10:35357/v2.0"));

        NeutronConfig neutronConfig = node.neutronConfig();

        assertThat(neutronConfig.useMetadataProxy(), is(true));
        assertThat(neutronConfig.metadataProxySecret(), is("onos"));
        assertThat(neutronConfig.novaMetadataIp(), is("172.16.130.10"));
        assertThat(neutronConfig.novaMetadataPort(), is(8775));
    }

    /**
     * Tests the openstack obsolete controller node decoding.
     */
    @Test
    public void testOpenstackObsoleteControllerNodeDecode() throws IOException {
        OpenstackNode node = getOpenstackNode("OpenstackObsoleteControllerNode.json");

        assertThat(node.hostname(), is("controller"));
        assertThat(node.type().name(), is("CONTROLLER"));
        assertThat(node.managementIp().toString(), is("172.16.130.10"));

        KeystoneConfig keystoneConfig = node.keystoneConfig();
        OpenstackAuth auth = keystoneConfig.authentication();
        String endpoint = keystoneConfig.endpoint();

        assertThat(auth.version(), is("v2.0"));
        assertThat(auth.protocol(), is(OpenstackAuth.Protocol.HTTP));
        assertThat(auth.username(), is("admin"));
        assertThat(auth.password(), is("nova"));
        assertThat(auth.project(), is("admin"));
        assertThat(auth.perspective(), is(OpenstackAuth.Perspective.PUBLIC));

        assertThat(endpoint, is("172.16.130.10:35357/v2.0"));
    }

    /**
     * Reads in an openstack node from the given resource and decodes it.
     *
     * @param resourceName resource to use to read the JSON for the rule
     * @return decoded openstack node
     * @throws IOException if processing the resource fails
     */
    private OpenstackNode getOpenstackNode(String resourceName) throws IOException {
        InputStream jsonStream = OpenstackNodeCodecTest.class.getResourceAsStream(resourceName);
        JsonNode json = context.mapper().readTree(jsonStream);
        MatcherAssert.assertThat(json, notNullValue());
        OpenstackNode node = openstackNodeCodec.decode((ObjectNode) json, context);
        assertThat(node, notNullValue());
        return node;
    }

    /**
     * Mock codec context for use in codec unit tests.
     */
    private class MockCodecContext implements CodecContext {
        private final ObjectMapper mapper = new ObjectMapper();
        private final CodecManager manager = new CodecManager();
        private final Map<Class<?>, Object> services = new HashMap<>();

        /**
         * Constructs a new mock codec context.
         */
        public MockCodecContext() {
            manager.activate();
        }

        @Override
        public ObjectMapper mapper() {
            return mapper;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> JsonCodec<T> codec(Class<T> entityClass) {
            if (entityClass == OpenstackPhyInterface.class) {
                return (JsonCodec<T>) openstackPhyIntfJsonCodec;
            }
            if (entityClass == ControllerInfo.class) {
                return (JsonCodec<T>) openstackControllerJsonCodec;
            }
            if (entityClass == OpenstackAuth.class) {
                return (JsonCodec<T>) openstackAuthJsonCodec;
            }
            if (entityClass == OpenstackSshAuth.class) {
                return (JsonCodec<T>) openstackSshAuthJsonCodec;
            }
            if (entityClass == DpdkConfig.class) {
                return (JsonCodec<T>) dpdkConfigJsonCodec;
            }
            if (entityClass == DpdkInterface.class) {
                return (JsonCodec<T>) dpdkInterfaceJsonCodec;
            }
            if (entityClass == KeystoneConfig.class) {
                return (JsonCodec<T>) keystoneConfigJsonCodec;
            }
            if (entityClass == NeutronConfig.class) {
                return (JsonCodec<T>) neutronConfigJsonCodec;
            }
            return manager.getCodec(entityClass);
        }

        @SuppressWarnings("unchecked")
        @Override
        public <T> T getService(Class<T> serviceClass) {
            return (T) services.get(serviceClass);
        }

        // for registering mock services
        public <T> void registerService(Class<T> serviceClass, T impl) {
            services.put(serviceClass, impl);
        }
    }
}
