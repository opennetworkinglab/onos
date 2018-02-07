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

package org.onosproject.pipelines.fabric;

import com.google.common.collect.ImmutableList;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
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
import org.onosproject.pipelines.fabric.pipeliner.FabricPipeliner;

import java.net.URL;
import java.util.Collection;

import static org.onosproject.net.pi.model.PiPipeconf.ExtensionType.BMV2_JSON;
import static org.onosproject.net.pi.model.PiPipeconf.ExtensionType.P4_INFO_TEXT;

/**
 * Pipeline config loader for fabric pipeline.
 */
@Component(immediate = true)
public class PipeconfLoader {

    public static final PiPipeconfId FABRIC_PIPECONF_ID =
            new PiPipeconfId("org.onosproject.pipelines.fabric");

    private static final String FABRIC_JSON_PATH = "/p4c-out/bmv2/fabric.json";
    private static final String FABRIC_P4INFO_PATH = "/p4c-out/bmv2/fabric.p4info";

    private static final PiPipeconf FABRIC_PIPECONF = buildFabricPipeconf();

    // XXX: Use a collection to hold only one pipeconf because we might separate
    // fabric pipeconf to leaf/spine pipeconf in the future.
    private static final Collection<PiPipeconf> ALL_PIPECONFS =
            ImmutableList.of(FABRIC_PIPECONF);

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    private PiPipeconfService piPipeconfService;

    @Activate
    public void activate() {
        // Registers all pipeconf at component activation.
        ALL_PIPECONFS.forEach(piPipeconfService::register);
    }

    @Deactivate
    public void deactivate() {
        ALL_PIPECONFS.stream().map(PiPipeconf::id).forEach(piPipeconfService::remove);
    }

    private static PiPipeconf buildFabricPipeconf() {
        final URL jsonUrl = PipeconfLoader.class.getResource(FABRIC_JSON_PATH);
        final URL p4InfoUrl = PipeconfLoader.class.getResource(FABRIC_P4INFO_PATH);
        final PiPipelineModel model = parseP4Info(p4InfoUrl);
        // TODO: add properties to pipeconf instead of adding it to driver
        return DefaultPiPipeconf.builder()
                .withId(FABRIC_PIPECONF_ID)
                .withPipelineModel(model)
                .addBehaviour(PiPipelineInterpreter.class, FabricInterpreter.class)
                .addBehaviour(Pipeliner.class, FabricPipeliner.class)
                .addBehaviour(PortStatisticsDiscovery.class, FabricPortStatisticsDiscovery.class)
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
