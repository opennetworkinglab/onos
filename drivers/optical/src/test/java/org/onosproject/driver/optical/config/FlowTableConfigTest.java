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
package org.onosproject.driver.optical.config;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.onosproject.net.PortNumber.portNumber;
import static org.onosproject.net.flow.instructions.Instructions.modL0Lambda;

import java.io.IOException;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.core.CoreServiceAdapter;
import org.onosproject.core.DefaultApplicationId;
import org.onosproject.net.DeviceId;
import org.onosproject.net.OchSignal;
import org.onosproject.net.PortNumber;
import org.onosproject.net.flow.DefaultFlowRule;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.FlowRule;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableSet;

public class FlowTableConfigTest extends BaseConfigTestHelper {

    private static final String SAMPLE = "flow_table_config.json";


    private static final DeviceId DID = DeviceId.deviceId("of:0000000000000001");
    private static final PortNumber PN_1 = portNumber(1);
    private static final PortNumber PN_2 = portNumber(2);
    private static final int FLOW_ID_3 = 3;
    private static final int PRIO_4 = 4;

    private static final DefaultApplicationId APP_ID =
                new DefaultApplicationId(FLOW_ID_3 >>> 48, "test");

    private static final OchSignal LAMBDA_42 = OchSignal.newFlexGridSlot(42);

    private static final FlowRule FLOW_RULE = DefaultFlowRule.builder()
                        .forDevice(DID)
                        .withCookie(FLOW_ID_3)
                        .makePermanent()
                        .withPriority(PRIO_4)
                        .withSelector(DefaultTrafficSelector.builder()
                                          .matchInPort(PN_1)
                                          .build())
                        .withTreatment(DefaultTrafficTreatment.builder()
                                           .setOutput(PN_2)
                                           .add(modL0Lambda(LAMBDA_42))
                                           .build())
                        .build();


    private ObjectMapper mapper;


    private JsonNode cfgnode;

    @Before
    public void setUp() throws Exception {

        directory.add(CoreService.class, new CoreServiceAdapter() {
            @Override
            public ApplicationId getAppId(Short id) {
                return APP_ID;
            }

            @Override
            public ApplicationId registerApplication(String name) {
                return APP_ID;
            }
        });

        mapper = testFriendlyMapper();
        JsonNode sample = loadJsonFromResource(SAMPLE, mapper);

        cfgnode = sample.path("devices")
                                    .path(DID.toString())
                                       .path(FlowTableConfig.CONFIG_KEY);

    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void readTest() throws JsonProcessingException, IOException {

        FlowTableConfig sut = new FlowTableConfig();
        sut.init(DID, FlowTableConfig.CONFIG_KEY, cfgnode, mapper, noopDelegate);

        assertThat(sut.flowtable(), is(equalTo(ImmutableSet.of(FLOW_RULE))));
    }

    @Test
    public void writeTest() throws JsonProcessingException, IOException {

        FlowTableConfig w = new FlowTableConfig();
        w.init(DID, FlowTableConfig.CONFIG_KEY, mapper.createObjectNode(), mapper, noopDelegate);

        Set<FlowRule> table = ImmutableSet.of(FLOW_RULE);
        w.flowtable(table);

        assertEquals(cfgnode, w.node());
    }

}
