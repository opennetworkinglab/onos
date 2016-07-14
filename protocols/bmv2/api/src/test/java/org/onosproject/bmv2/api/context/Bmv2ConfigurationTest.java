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

package org.onosproject.bmv2.api.context;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import com.google.common.testing.EqualsTester;
import org.hamcrest.collection.IsIterableContainingInOrder;
import org.junit.Before;
import org.junit.Test;
import org.onosproject.bmv2.api.runtime.Bmv2MatchParam;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsEqual.equalTo;

/**
 * BMv2 JSON configuration parser test.
 */
public class Bmv2ConfigurationTest {

    private JsonObject json;
    private JsonObject json2;

    @Before
    public void setUp() throws Exception {
        json = Json.parse(new BufferedReader(new InputStreamReader(
                this.getClass().getResourceAsStream("/simple.json")))).asObject();
        json2 = Json.parse(new BufferedReader(new InputStreamReader(
                this.getClass().getResourceAsStream("/simple.json")))).asObject();
    }

    @Test
    public void testParse() throws Exception {
        Bmv2Configuration config = Bmv2DefaultConfiguration.parse(json);
        Bmv2Configuration config2 = Bmv2DefaultConfiguration.parse(json2);

        new EqualsTester()
                .addEqualityGroup(config, config2)
                .testEquals();

        /* Check header types */
        Bmv2HeaderTypeModel stdMetaT = config.headerType("standard_metadata_t");
        Bmv2HeaderTypeModel ethernetT = config.headerType("ethernet_t");
        Bmv2HeaderTypeModel intrinsicMetaT = config.headerType("intrinsic_metadata_t");

        Bmv2HeaderTypeModel stdMetaT2 = config2.headerType("standard_metadata_t");
        Bmv2HeaderTypeModel ethernetT2 = config2.headerType("ethernet_t");
        Bmv2HeaderTypeModel intrinsicMetaT2 = config2.headerType("intrinsic_metadata_t");

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
                   stdMetaT.fields(), hasSize(8));
        assertThat("Incorrect size for header type fields",
                   ethernetT.fields(), hasSize(3));
        assertThat("Incorrect size for header type fields",
                   intrinsicMetaT.fields(), hasSize(4));

        // check that fields are in order
        assertThat("Incorrect order for header type fields",
                   stdMetaT.fields(), IsIterableContainingInOrder.contains(
                        stdMetaT.field("ingress_port"),
                        stdMetaT.field("packet_length"),
                        stdMetaT.field("egress_spec"),
                        stdMetaT.field("egress_port"),
                        stdMetaT.field("egress_instance"),
                        stdMetaT.field("instance_type"),
                        stdMetaT.field("clone_spec"),
                        stdMetaT.field("_padding")));

        /* Check actions */
        Bmv2ActionModel floodAction = config.action("flood");
        Bmv2ActionModel dropAction = config.action("_drop");
        Bmv2ActionModel fwdAction = config.action("set_egress_port");

        Bmv2ActionModel floodAction2 = config2.action("flood");
        Bmv2ActionModel dropAction2 = config2.action("_drop");
        Bmv2ActionModel fwdAction2 = config2.action("set_egress_port");

        new EqualsTester()
                .addEqualityGroup(floodAction, floodAction2)
                .addEqualityGroup(dropAction, dropAction2)
                .addEqualityGroup(fwdAction, fwdAction2)
                .testEquals();

        // existence
        assertThat("Json parsed value is null", floodAction, notNullValue());
        assertThat("Json parsed value is null", dropAction, notNullValue());
        assertThat("Json parsed value is null", fwdAction, notNullValue());

        // runtime data size
        assertThat("Incorrect size for action runtime data",
                   floodAction.runtimeDatas().size(), is(equalTo(0)));
        assertThat("Incorrect size for action runtime data",
                   dropAction.runtimeDatas().size(), is(equalTo(0)));
        assertThat("Incorrect size for action runtime data",
                   fwdAction.runtimeDatas().size(), is(equalTo(1)));

        // runtime data existence and parsing
        assertThat("Parsed Json value is null",
                   fwdAction.runtimeData("port"), notNullValue());
        assertThat("Incorrect value for action runtime data bitwidth",
                   fwdAction.runtimeData("port").bitWidth(), is(equalTo(9)));

        /* Check tables */
        Bmv2TableModel table0 = config.table(0);
        Bmv2TableModel table02 = config2.table(0);

        new EqualsTester()
                .addEqualityGroup(table0, table02)
                .testEquals();

        // existence
        assertThat("Parsed Json value is null", table0, notNullValue());

        // id and name correspondence
        assertThat("Incorrect value for table name",
                   table0.name(), is(equalTo("table0")));

        // keys size
        assertThat("Incorrect size for table keys",
                   table0.keys().size(), is(equalTo(4)));

        // key match type
        assertThat("Incorrect value for table key match type",
                   table0.keys().get(0).matchType(), is(equalTo(Bmv2MatchParam.Type.TERNARY)));

        // header type
        assertThat("Incorrect value for table key header type",
                   table0.keys().get(0).field().header().type(), is(equalTo(stdMetaT)));
    }
}