/*
 * Copyright 2019-present Open Networking Foundation
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
package org.onosproject.openstackvtap.codec;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.hamcrest.MatcherAssert;
import org.junit.Before;
import org.junit.Test;
import org.onlab.packet.IpAddress;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.TpPort;
import org.onosproject.codec.CodecContext;
import org.onosproject.codec.JsonCodec;
import org.onosproject.codec.impl.CodecManager;
import org.onosproject.core.CoreService;
import org.onosproject.openstackvtap.api.OpenstackVtapCriterion;
import org.onosproject.openstackvtap.impl.DefaultOpenstackVtapCriterion;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.onosproject.net.NetTestTools.APP_ID;
import static org.onosproject.openstackvtap.codec.OpenstackVtapCriterionJsonMatcher.matchVtapCriterion;
import static org.onosproject.openstackvtap.util.OpenstackVtapUtil.getProtocolStringFromType;
import static org.onosproject.openstackvtap.util.OpenstackVtapUtil.getProtocolTypeFromString;

/**
 * Unit tests for OpenstackVtapCriterion codec.
 */
public class OpenstackVtapCriterionCodecTest {

    MockCodecContext context;
    JsonCodec<OpenstackVtapCriterion> vtapCriterionCodec;

    final CoreService mockCoreService = createMock(CoreService.class);
    private static final String REST_APP_ID = "org.onosproject.rest";

    @Before
    public void setUp() {
        context = new MockCodecContext();
        vtapCriterionCodec = new OpenstackVtapCriterionCodec();

        assertThat(vtapCriterionCodec, notNullValue());

        expect(mockCoreService.registerApplication(REST_APP_ID))
                .andReturn(APP_ID).anyTimes();
        replay(mockCoreService);
        context.registerService(CoreService.class, mockCoreService);
    }

    /**
     * Tests the openstack vtap criterion encoding.
     */
    @Test
    public void testOpenstackVtapCriterionEncode() {
        OpenstackVtapCriterion criterion = DefaultOpenstackVtapCriterion.builder()
                .srcIpPrefix(IpPrefix.valueOf(IpAddress.valueOf("10.10.10.10"), 32))
                .dstIpPrefix(IpPrefix.valueOf(IpAddress.valueOf("20.20.20.20"), 32))
                .ipProtocol(getProtocolTypeFromString("tcp"))
                .srcTpPort(TpPort.tpPort(8080))
                .dstTpPort(TpPort.tpPort(9090))
                .build();

        ObjectNode criterionJson = vtapCriterionCodec.encode(criterion, context);
        assertThat(criterionJson, matchVtapCriterion(criterion));
    }

    /**
     * Tests the openstack vtap criterion decoding.
     */
    @Test
    public void testOpenstackVtapCriterionDecode() throws IOException {
        OpenstackVtapCriterion criterion = getVtapCriterion("OpenstackVtapCriterion.json");

        assertThat(criterion.srcIpPrefix().address().toString(), is("10.10.10.10"));
        assertThat(criterion.dstIpPrefix().address().toString(), is("20.20.20.20"));
        assertThat(getProtocolStringFromType(criterion.ipProtocol()), is("tcp"));
        assertThat(criterion.srcTpPort().toInt(), is(8080));
        assertThat(criterion.dstTpPort().toInt(), is(9090));
    }

    /**
     * Reads in an openstack vtap criterion from the given resource and decodes it.
     *
     * @param resourceName resource to use to read the JSON for the rule
     * @return decoded openstack vtap criterion
     * @throws IOException if processing the resource fails
     */
    private OpenstackVtapCriterion getVtapCriterion(String resourceName) throws IOException {
        InputStream jsonStream = OpenstackVtapCriterionCodecTest.class.getResourceAsStream(resourceName);
        JsonNode json = context.mapper().readTree(jsonStream);
        MatcherAssert.assertThat(json, notNullValue());
        OpenstackVtapCriterion criterion = vtapCriterionCodec.decode((ObjectNode) json, context);
        assertThat(criterion, notNullValue());
        return criterion;
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

        @SuppressWarnings("unchecked")
        @Override
        public <T> JsonCodec<T> codec(Class<T> entityClass) {
            if (entityClass == OpenstackVtapCriterion.class) {
                return (JsonCodec<T>) vtapCriterionCodec;
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
