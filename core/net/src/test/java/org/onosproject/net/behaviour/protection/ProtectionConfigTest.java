/*
 * Copyright 2016-present Open Networking Foundation
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
package org.onosproject.net.behaviour.protection;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;
import static org.onosproject.net.PortNumber.portNumber;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.onlab.junit.TestUtils;
import org.onlab.junit.TestUtils.TestUtilsException;
import org.onlab.osgi.ServiceDirectory;
import org.onlab.osgi.TestServiceDirectory;
import org.onlab.packet.VlanId;
import org.onosproject.codec.CodecService;
import org.onosproject.codec.impl.CodecManager;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.FilteredConnectPoint;
import org.onosproject.net.config.BaseConfig;
import org.onosproject.net.config.ConfigApplyDelegate;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.TrafficSelector;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.NumericNode;
import com.google.common.collect.ImmutableList;

public class ProtectionConfigTest {

    private static TestServiceDirectory directory;
    private static ServiceDirectory original;


    // no-op
    private ConfigApplyDelegate delegate = cfg -> { };


    private final DeviceId did = DeviceId.deviceId("of:0000000000000001");
    private final String fingerprint = "(IntentKey_XYZW)";
    private final DeviceId peer = DeviceId.deviceId("of:0000000000000002");


    private final ConnectPoint workingCp = new ConnectPoint(did, portNumber(1));
    private final ConnectPoint backupCp = new ConnectPoint(did, portNumber(2));

    private final TrafficSelector vlan100 = DefaultTrafficSelector.builder()
            .matchVlanId(VlanId.vlanId((short) 100))
            .build();


    private final TransportEndpointDescription working
        = TransportEndpointDescription.builder()
            .withOutput(new FilteredConnectPoint(workingCp, vlan100))
            .withEnabled(true)
            .build();


    private final TransportEndpointDescription backup
        = TransportEndpointDescription.builder()
            .withOutput(new FilteredConnectPoint(backupCp, vlan100))
            .withEnabled(true)
            .build();

    private final List<TransportEndpointDescription> paths
        = ImmutableList.of(working, backup);

    private final ProtectedTransportEndpointDescription descr
        = ProtectedTransportEndpointDescription.of(paths,
                                                   did,
                                                   fingerprint);


    private ProtectionConfig sut;

    private ObjectMapper mapper;

    /**
     * {@value #SAMPLE_JSON_PATH} after parsing.
     */
    private JsonNode node;

    @BeforeClass
    public static void setUpClass() throws TestUtilsException {
        directory = new TestServiceDirectory();

        CodecManager codecService = new CodecManager();
        codecService.activate();
        directory.add(CodecService.class, codecService);

        // replace service directory used by BaseConfig
        original = TestUtils.getField(BaseConfig.class, "services");
        TestUtils.setField(BaseConfig.class, "services", directory);
    }

    @AfterClass
    public static void tearDownClass() throws TestUtilsException {
        TestUtils.setField(BaseConfig.class, "services", original);
    }

    @Before
    public void setUp() throws JsonProcessingException, IOException, TestUtilsException {

        mapper = new ObjectMapper();
        // Jackson configuration for ease of Numeric node comparison
        // - treat integral number node as long node
        mapper.enable(DeserializationFeature.USE_LONG_FOR_INTS);
        mapper.setNodeFactory(new JsonNodeFactory(false) {
            @Override
            public NumericNode numberNode(int v) {
                return super.numberNode((long) v);
            }
            @Override
            public NumericNode numberNode(short v) {
                return super.numberNode((long) v);
            }
        });

        InputStream stream = ProtectionConfigTest.class
                                .getResourceAsStream("protection_config.json");
        JsonNode tree = mapper.readTree(stream);

        node = tree.path("devices")
                            .path(did.toString())
                            .path(ProtectionConfig.CONFIG_KEY);
        assertTrue(node.isObject());

    }

    @Test
    public void readTest() {
        sut = new ProtectionConfig();
        sut.init(did, ProtectionConfig.CONFIG_KEY, node, mapper, delegate);

        assertThat(sut.subject(), is(did));
        assertThat(sut.paths().size(), is(2));

        TransportEndpointDescription readWorking = sut.paths().get(0);
        assertThat(readWorking.isEnabled(), is(true));
        assertThat(readWorking.output().connectPoint(), is(workingCp));
        assertThat(readWorking.output().trafficSelector(), is(vlan100));

        TransportEndpointDescription readBackup = sut.paths().get(1);
        assertThat(readBackup.isEnabled(), is(true));
        assertThat(readBackup.output().connectPoint(), is(backupCp));
        assertThat(readBackup.output().trafficSelector(), is(vlan100));

        assertThat(sut.fingerprint(), is(fingerprint));
        assertThat(sut.peer(), is(peer));
    }

    @Test
    public void writeTest() throws JsonProcessingException, IOException {
        ProtectionConfig w = new ProtectionConfig();
        w.init(did, ProtectionConfig.CONFIG_KEY, mapper.createObjectNode(), mapper, delegate);

        // write fields
        w.paths(paths);
        w.fingerprint(fingerprint);
        w.peer(peer);

        // reparse JSON
        JsonNode r = mapper.readTree(w.node().toString());

        sut = new ProtectionConfig();
        sut.init(did, ProtectionConfig.CONFIG_KEY, r, mapper, delegate);

        // verify equivalence
        assertThat(sut.paths().size(), is(2));

        TransportEndpointDescription readWorking = sut.paths().get(0);
        assertThat(readWorking.isEnabled(), is(true));
        assertThat(readWorking.output().connectPoint(), is(workingCp));
        assertThat(readWorking.output().trafficSelector(), is(vlan100));

        TransportEndpointDescription readBackup = sut.paths().get(1);
        assertThat(readBackup.isEnabled(), is(true));
        assertThat(readBackup.output().connectPoint(), is(backupCp));
        assertThat(readBackup.output().trafficSelector(), is(vlan100));


        assertThat(sut.fingerprint(), is(fingerprint));
        assertThat(sut.peer(), is(peer));
    }
}
