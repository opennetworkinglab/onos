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

package org.onosproject.pi.demo.app.ecmp;

import org.onosproject.bmv2.model.Bmv2PipelineModelParser;
import org.onosproject.driver.pipeline.DefaultSingleTablePipeline;
import org.onosproject.drivers.p4runtime.DefaultP4PortStatisticsDiscovery;
import org.onosproject.net.behaviour.Pipeliner;
import org.onosproject.net.device.PortStatisticsDiscovery;
import org.onosproject.net.pi.model.DefaultPiPipeconf;
import org.onosproject.net.pi.model.PiPipeconf;
import org.onosproject.net.pi.model.PiPipeconfId;
import org.onosproject.net.pi.model.PiPipelineInterpreter;

import java.net.URL;
import java.util.Collection;
import java.util.Collections;

import static org.onosproject.net.pi.model.PiPipeconf.ExtensionType.BMV2_JSON;
import static org.onosproject.net.pi.model.PiPipeconf.ExtensionType.P4_INFO_TEXT;

final class EcmpPipeconfFactory {

    private static final String BMV2_PIPECONF_ID = "pi-demo-ecmp";
    private static final URL BMV2_P4INFO_URL = EcmpFabricApp.class.getResource("/ecmp.p4info");
    private static final URL BMV2_JSON_URL = EcmpFabricApp.class.getResource("/ecmp.json");

    private static final PiPipeconf BMV2_PIPECONF = buildBmv2Pipeconf();

    private EcmpPipeconfFactory() {
        // Hides constructor.
    }

    static Collection<PiPipeconf> getAll() {
        return Collections.singleton(BMV2_PIPECONF);
    }

    private static PiPipeconf buildBmv2Pipeconf() {
        return DefaultPiPipeconf.builder()
                .withId(new PiPipeconfId(BMV2_PIPECONF_ID))
                .withPipelineModel(Bmv2PipelineModelParser.parse(BMV2_JSON_URL))
                .addBehaviour(PiPipelineInterpreter.class, EcmpInterpreter.class)
                .addBehaviour(Pipeliner.class, DefaultSingleTablePipeline.class)
                .addBehaviour(PortStatisticsDiscovery.class, DefaultP4PortStatisticsDiscovery.class)
                .addExtension(P4_INFO_TEXT, BMV2_P4INFO_URL)
                .addExtension(BMV2_JSON, BMV2_JSON_URL)
                .build();
    }
}
