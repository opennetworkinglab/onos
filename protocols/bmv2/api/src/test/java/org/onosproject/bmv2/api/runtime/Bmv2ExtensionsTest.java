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

package org.onosproject.bmv2.api.runtime;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import com.google.common.testing.EqualsTester;
import org.junit.Before;
import org.junit.Test;
import org.onlab.packet.MacAddress;
import org.onosproject.bmv2.api.context.Bmv2Configuration;
import org.onosproject.bmv2.api.context.Bmv2DefaultConfiguration;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class Bmv2ExtensionsTest {

    private Bmv2Configuration config;

    @Before
    public void setUp() throws Exception {
        JsonObject json = Json.parse(new BufferedReader(new InputStreamReader(
                this.getClass().getResourceAsStream("/simple.json")))).asObject();
        config = Bmv2DefaultConfiguration.parse(json);
    }

    @Test
    public void testExtensionSelectorBuilder() throws Exception {

        Bmv2ExtensionSelector extSelectorExact = Bmv2ExtensionSelector.builder()
                .forConfiguration(config)
                .matchExact("standard_metadata", "ingress_port", (short) 255)
                .matchExact("ethernet", "etherType", 512)
                .matchExact("ethernet", "dstAddr", 1024L)
                .matchExact("ethernet", "srcAddr", MacAddress.BROADCAST.toBytes())
                .build();

        Bmv2ExtensionSelector extSelectorTernary = Bmv2ExtensionSelector.builder()
                .forConfiguration(config)
                .matchTernary("standard_metadata", "ingress_port", (short) 255, (short) 255)
                .matchTernary("ethernet", "etherType", 512, 512)
                .matchTernary("ethernet", "dstAddr", 1024L, 1024L)
                .matchTernary("ethernet", "srcAddr", MacAddress.BROADCAST.toBytes(), MacAddress.NONE.toBytes())
                .build();

        Bmv2ExtensionSelector extSelectorLpm = Bmv2ExtensionSelector.builder()
                .forConfiguration(config)
                .matchLpm("standard_metadata", "ingress_port", (short) 255, 1)
                .matchLpm("ethernet", "etherType", 512, 2)
                .matchLpm("ethernet", "dstAddr", 1024L, 3)
                .matchLpm("ethernet", "srcAddr", MacAddress.BROADCAST.toBytes(), 4)
                .build();

        Bmv2ExtensionSelector extSelectorValid = Bmv2ExtensionSelector.builder()
                .forConfiguration(config)
                .matchValid("standard_metadata", "ingress_port", true)
                .matchValid("ethernet", "etherType", true)
                .matchValid("ethernet", "dstAddr", false)
                .matchValid("ethernet", "srcAddr", false)
                .build();

        assertThat(extSelectorExact.parameterMap().size(), is(4));
        assertThat(extSelectorTernary.parameterMap().size(), is(4));
        assertThat(extSelectorLpm.parameterMap().size(), is(4));
        assertThat(extSelectorValid.parameterMap().size(), is(4));

        // TODO add more tests, e.g. check for byte sequences content and size.
    }

    @Test
    public void testExtensionTreatmentBuilder() throws Exception {

        Bmv2ExtensionTreatment treatment = Bmv2ExtensionTreatment.builder()
                .forConfiguration(config)
                .setActionName("set_egress_port")
                .addParameter("port", 1)
                .build();

        assertThat(treatment.action().parameters().size(), is(1));

        // TODO add more tests, e.g. check for byte sequences content and size.
    }

    @Test
    public void testExtensionSelectorSerialization() throws Exception {

        Bmv2ExtensionSelector original = Bmv2ExtensionSelector.builder()
                .forConfiguration(config)
                .matchExact("standard_metadata", "ingress_port", (short) 255)
                .matchLpm("ethernet", "etherType", 512, 4)
                .matchTernary("ethernet", "dstAddr", 1024L, 512L)
                .matchValid("ethernet", "srcAddr", true)
                .build();

        Bmv2ExtensionSelector other = Bmv2ExtensionSelector.empty();
        other.deserialize(original.serialize());

        new EqualsTester()
                .addEqualityGroup(original, other)
                .testEquals();
    }

    @Test
    public void testExtensionTreatmentSerialization() throws Exception {

        Bmv2ExtensionTreatment original = Bmv2ExtensionTreatment.builder()
                .forConfiguration(config)
                .setActionName("set_egress_port")
                .addParameter("port", 1)
                .build();

        Bmv2ExtensionTreatment other = Bmv2ExtensionTreatment.empty();
        other.deserialize(original.serialize());

        new EqualsTester()
                .addEqualityGroup(original, other)
                .testEquals();
    }
}