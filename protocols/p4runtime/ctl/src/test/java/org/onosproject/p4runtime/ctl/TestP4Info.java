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

package org.onosproject.p4runtime.ctl;

import com.google.protobuf.ExtensionRegistry;
import com.google.protobuf.TextFormat;
import org.junit.Test;
import p4.config.P4InfoOuterClass;

import java.io.InputStream;
import java.io.InputStreamReader;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class TestP4Info {

    private InputStream p4InfoStream = this.getClass().getResourceAsStream("/default.p4info");

    @Test
    public void testP4InfoBrowser() throws Exception {

        InputStreamReader input = new InputStreamReader(p4InfoStream);
        ExtensionRegistry extensionRegistry = ExtensionRegistry.getEmptyRegistry();
        P4InfoOuterClass.P4Info.Builder builder = P4InfoOuterClass.P4Info.newBuilder();

        builder.clear();
        TextFormat.getParser().merge(input, extensionRegistry, builder);
        P4InfoOuterClass.P4Info p4Info = builder.build();

        P4InfoBrowser browser = new P4InfoBrowser(p4Info);

        assertThat(browser.tables().hasName("table0"), is(true));
        assertThat(browser.actions().hasName("set_egress_port"), is(true));

        int tableId = browser.tables().getByName("table0").getPreamble().getId();
        int actionId = browser.actions().getByName("set_egress_port").getPreamble().getId();

        assertThat(browser.matchFields(tableId).hasName("standard_metadata.ingress_port"), is(true));
        assertThat(browser.actionParams(actionId).hasName("port"), is(true));

        // TODO: improve, assert browsing of other entities (counters, meters, etc.)
    }
}
