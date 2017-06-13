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

package org.onosproject.bmv2.model;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import com.google.common.collect.Sets;
import com.google.common.testing.EqualsTester;
import org.hamcrest.collection.IsIterableContainingInOrder;
import org.junit.Before;
import org.junit.Test;
import org.onosproject.net.pi.model.PiHeaderFieldModel;
import org.onosproject.net.pi.model.PiHeaderModel;
import org.onosproject.net.pi.model.PiHeaderTypeModel;
import org.onosproject.net.pi.model.PiMatchType;
import org.onosproject.net.pi.model.PiTableMatchFieldModel;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Set;
import java.util.stream.Collectors;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.core.IsEqual.equalTo;

/**
 * BMv2 JSON configuration parser test.
 */
public class Bmv2PipelineModelParserTest {

    private JsonObject json;
    private JsonObject json2;

    @Before
    public void setUp() throws Exception {
        json = Json.parse(new BufferedReader(new InputStreamReader(
                this.getClass().getResourceAsStream("/default.json")))).asObject();
        json2 = Json.parse(new BufferedReader(new InputStreamReader(
                this.getClass().getResourceAsStream("/default.json")))).asObject();
    }

    @Test
    public void testParse() throws Exception {
        Bmv2PipelineModel config = Bmv2PipelineModelParser.parse(json);
        Bmv2PipelineModel config2 = Bmv2PipelineModelParser.parse(json2);

        new EqualsTester()
                .addEqualityGroup(config, config2)
                .testEquals();

        /* Check header types */
        Bmv2HeaderTypeModel stdMetaT =
                (Bmv2HeaderTypeModel) config.headerType("standard_metadata").orElse(null);
        Bmv2HeaderTypeModel ethernetT =
                (Bmv2HeaderTypeModel) config.headerType("ethernet_t").orElse(null);
        Bmv2HeaderTypeModel intrinsicMetaT =
                (Bmv2HeaderTypeModel) config.headerType("intrinsic_metadata_t").orElse(null);

        Bmv2HeaderTypeModel stdMetaT2 =
                (Bmv2HeaderTypeModel) config2.headerType("standard_metadata").orElse(null);
        Bmv2HeaderTypeModel ethernetT2 =
                (Bmv2HeaderTypeModel) config2.headerType("ethernet_t").orElse(null);
        Bmv2HeaderTypeModel intrinsicMetaT2 =
                (Bmv2HeaderTypeModel) config2.headerType("intrinsic_metadata_t").orElse(null);

        new EqualsTester()
                .addEqualityGroup(stdMetaT, stdMetaT2)
                .addEqualityGroup(ethernetT, ethernetT2)
                .addEqualityGroup(intrinsicMetaT, intrinsicMetaT2)
                .testEquals();

        // existence
        assertThat("Json parsed value is null", stdMetaT, notNullValue());
        assertThat("Json parsed value is null", ethernetT, notNullValue());
        assertThat("Json parsed value is null", intrinsicMetaT, notNullValue());

        // fields size
        assertThat("Incorrect size for header type fields",
                   stdMetaT.fields(), hasSize(18));
        assertThat("Incorrect size for header type fields",
                   ethernetT.fields(), hasSize(3));
        assertThat("Incorrect size for header type fields",
                   intrinsicMetaT.fields(), hasSize(4));

        // check that fields are in order
        assertThat("Incorrect order for header type fields",
                   stdMetaT.fields(), IsIterableContainingInOrder.contains(
                        stdMetaT.field("ingress_port").get(),
                        stdMetaT.field("egress_spec").get(),
                        stdMetaT.field("egress_port").get(),
                        stdMetaT.field("clone_spec").get(),
                        stdMetaT.field("instance_type").get(),
                        stdMetaT.field("drop").get(),
                        stdMetaT.field("recirculate_port").get(),
                        stdMetaT.field("packet_length").get(),
                        stdMetaT.field("enq_timestamp").get(),
                        stdMetaT.field("enq_qdepth").get(),
                        stdMetaT.field("deq_timedelta").get(),
                        stdMetaT.field("deq_qdepth").get(),
                        stdMetaT.field("ingress_global_timestamp").get(),
                        stdMetaT.field("lf_field_list").get(),
                        stdMetaT.field("mcast_grp").get(),
                        stdMetaT.field("resubmit_flag").get(),
                        stdMetaT.field("egress_rid").get(),
                        stdMetaT.field("_padding").get()
                ));

        /* Check actions */
        Bmv2ActionModel noAction =
                (Bmv2ActionModel) config.action("NoAction").orElse(null);
        Bmv2ActionModel setEgressPortAction =
                (Bmv2ActionModel) config.action("set_egress_port_0").orElse(null);
        Bmv2ActionModel sendToCpuAction =
                (Bmv2ActionModel) config.action("send_to_cpu_0").orElse(null);
        Bmv2ActionModel dropAction =
                (Bmv2ActionModel) config.action("_drop_0").orElse(null);
        Bmv2ActionModel processPortCountersCountPacketAction =
                (Bmv2ActionModel) config.action("process_port_counters_0.count_packet").orElse(null);


        Bmv2ActionModel noAction2 =
                (Bmv2ActionModel) config.action("NoAction").orElse(null);
        Bmv2ActionModel setEgressPortAction2 =
                (Bmv2ActionModel) config.action("set_egress_port_0").orElse(null);
        Bmv2ActionModel sendToCpuAction2 =
                (Bmv2ActionModel) config.action("send_to_cpu_0").orElse(null);
        Bmv2ActionModel dropAction2 =
                (Bmv2ActionModel) config.action("_drop_0").orElse(null);
        Bmv2ActionModel processPortCountersCountPacketAction2 =
                (Bmv2ActionModel) config.action("process_port_counters_0.count_packet").orElse(null);

        new EqualsTester()
                .addEqualityGroup(noAction, noAction2)
                .addEqualityGroup(setEgressPortAction, setEgressPortAction2)
                .addEqualityGroup(sendToCpuAction, sendToCpuAction2)
                .addEqualityGroup(dropAction, dropAction2)
                .addEqualityGroup(processPortCountersCountPacketAction, processPortCountersCountPacketAction2)
                .testEquals();

        // existence
        assertThat("Json parsed value is null", noAction, notNullValue());
        assertThat("Json parsed value is null", setEgressPortAction, notNullValue());
        assertThat("Json parsed value is null", sendToCpuAction, notNullValue());
        assertThat("Json parsed value is null", dropAction, notNullValue());
        assertThat("Json parsed value is null", processPortCountersCountPacketAction, notNullValue());

        // runtime data size
        assertThat("Incorrect size for action runtime data",
                   noAction.params().size(), is(equalTo(0)));
        assertThat("Incorrect size for action runtime data",
                   setEgressPortAction.params().size(), is(equalTo(1)));
        assertThat("Incorrect size for action runtime data",
                   sendToCpuAction.params().size(), is(equalTo(0)));
        assertThat("Incorrect size for action runtime data",
                   dropAction.params().size(), is(equalTo(0)));
        assertThat("Incorrect size for action runtime data",
                   processPortCountersCountPacketAction.params().size(), is(equalTo(0)));

        // runtime data existence and parsing
        assertThat("Parsed Json value is null",
                   setEgressPortAction.param("port").orElse(null), notNullValue());
        assertThat("Incorrect value for action runtime data bitwidth",
                   setEgressPortAction.param("port").get().bitWidth(), is(equalTo(9)));


        /* Check tables */
        Bmv2TableModel table0 =
                (Bmv2TableModel) config.table("table0").orElse(null);
        Bmv2TableModel table02 =
                (Bmv2TableModel) config2.table("table0").orElse(null);

        new EqualsTester()
                .addEqualityGroup(table0, table02)
                .testEquals();

        // existence
        assertThat("Parsed Json value is null", table0, notNullValue());

        // id and name correspondence
        assertThat("Incorrect value for table name",
                   table0.name(), is(equalTo("table0")));

        Set<PiTableMatchFieldModel> matchFields = Sets.newHashSet(table0.matchFields());

        // keys size
        assertThat("Incorrect size for table keys",
                   matchFields.size(), is(equalTo(4)));

        Set<PiMatchType> matchTypes = matchFields.stream()
                .map(PiTableMatchFieldModel::matchType)
                .collect(Collectors.toSet());

        // key match type
        assertThat("Incorrect value for table key match type",
                   matchTypes, containsInAnyOrder(PiMatchType.TERNARY));

        Set<PiHeaderTypeModel> headerTypeModels = matchFields.stream()
                .map(PiTableMatchFieldModel::field)
                .map(PiHeaderFieldModel::header)
                .map(PiHeaderModel::type)
                .collect(Collectors.toSet());

        // header type
        assertThat("Incorrect value for table key header type",
                   headerTypeModels, containsInAnyOrder(ethernetT, stdMetaT));

    }
}