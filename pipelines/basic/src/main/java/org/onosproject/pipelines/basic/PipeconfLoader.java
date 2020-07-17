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

package org.onosproject.pipelines.basic;

import com.google.common.collect.ImmutableList;
import org.onosproject.core.CoreService;
import org.onosproject.net.behaviour.inbandtelemetry.IntProgrammable;
import org.onosproject.net.behaviour.Pipeliner;
import org.onosproject.net.device.PortStatisticsDiscovery;
import org.onosproject.net.pi.model.DefaultPiPipeconf;
import org.onosproject.net.pi.model.PiPipeconf;
import org.onosproject.net.pi.model.PiPipeconfId;
import org.onosproject.net.pi.model.PiPipelineInterpreter;
import org.onosproject.net.pi.model.PiPipelineModel;
import org.onosproject.net.pi.service.PiPipeconfService;
import org.onosproject.p4runtime.model.P4InfoParser;
import org.onosproject.p4runtime.model.P4InfoParserException;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;

import java.net.URL;
import java.util.Collection;

import static org.onosproject.net.pi.model.PiPipeconf.ExtensionType.BMV2_JSON;
import static org.onosproject.net.pi.model.PiPipeconf.ExtensionType.P4_INFO_TEXT;

/**
 * Component that produces and registers the basic pipeconfs when loaded.
 */
@Component(immediate = true)
public final class PipeconfLoader {

    private static final String APP_NAME = "org.onosproject.pipelines.basic";
    private static final PiPipeconfId BASIC_PIPECONF_ID = new PiPipeconfId("org.onosproject.pipelines.basic");
    private static final String BASIC_JSON_PATH = "/p4c-out/bmv2/basic.json";
    private static final String BASIC_P4INFO = "/p4c-out/bmv2/basic_p4info.txt";

    public static final PiPipeconf BASIC_PIPECONF = buildBasicPipeconf();

    private static final PiPipeconfId INT_PIPECONF_ID = new PiPipeconfId("org.onosproject.pipelines.int");
    private static final String INT_JSON_PATH = "/p4c-out/bmv2/int.json";
    private static final String INT_P4INFO = "/p4c-out/bmv2/int_p4info.txt";

    public static final PiPipeconf INT_PIPECONF = buildIntPipeconf();

    private static final Collection<PiPipeconf> ALL_PIPECONFS = ImmutableList.of(BASIC_PIPECONF, INT_PIPECONF);

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    private PiPipeconfService piPipeconfService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    private CoreService coreService;

    @Activate
    public void activate() {
        coreService.registerApplication(APP_NAME);
        // Registers all pipeconf at component activation.
        ALL_PIPECONFS.forEach(piPipeconfService::register);
    }

    @Deactivate
    public void deactivate() {
        ALL_PIPECONFS.stream().map(PiPipeconf::id).forEach(piPipeconfService::unregister);
    }

    private static PiPipeconf buildBasicPipeconf() {
        final URL jsonUrl = PipeconfLoader.class.getResource(BASIC_JSON_PATH);
        final URL p4InfoUrl = PipeconfLoader.class.getResource(BASIC_P4INFO);

        return DefaultPiPipeconf.builder()
                .withId(BASIC_PIPECONF_ID)
                .withPipelineModel(parseP4Info(p4InfoUrl))
                .addBehaviour(PiPipelineInterpreter.class, BasicInterpreterImpl.class)
                .addBehaviour(Pipeliner.class, BasicPipelinerImpl.class)
                .addBehaviour(PortStatisticsDiscovery.class, PortStatisticsDiscoveryImpl.class)
                .addExtension(P4_INFO_TEXT, p4InfoUrl)
                .addExtension(BMV2_JSON, jsonUrl)
                // Put here other target-specific extensions,
                // e.g. Tofino's bin and context.json.
                .build();
    }

    private static PiPipeconf buildIntPipeconf() {
        final URL jsonUrl = PipeconfLoader.class.getResource(INT_JSON_PATH);
        final URL p4InfoUrl = PipeconfLoader.class.getResource(INT_P4INFO);

        // INT behavior is controlled using pipeline-specific flow rule,
        // not using flow objectives, so we just borrow pipeliner to basic pipeconf.
        return DefaultPiPipeconf.builder()
                .withId(INT_PIPECONF_ID)
                .withPipelineModel(parseP4Info(p4InfoUrl))
                .addBehaviour(PiPipelineInterpreter.class, BasicInterpreterImpl.class)
                .addBehaviour(Pipeliner.class, BasicPipelinerImpl.class)
                .addBehaviour(PortStatisticsDiscovery.class, PortStatisticsDiscoveryImpl.class)
                .addBehaviour(IntProgrammable.class, IntProgrammableImpl.class)
                .addExtension(P4_INFO_TEXT, p4InfoUrl)
                .addExtension(BMV2_JSON, jsonUrl)
                .build();
    }

    private static PiPipelineModel parseP4Info(URL p4InfoUrl) {
        try {
            return P4InfoParser.parse(p4InfoUrl);
        } catch (P4InfoParserException e) {
            throw new IllegalStateException(e);
        }
    }
}
