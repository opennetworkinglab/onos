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

package org.onosproject.pipelines.fabric.impl;

import org.onosproject.net.behaviour.inbandtelemetry.IntProgrammable;

import org.onosproject.net.behaviour.BngProgrammable;
import org.onosproject.net.behaviour.Pipeliner;
import org.onosproject.net.pi.model.DefaultPiPipeconf;
import org.onosproject.net.pi.model.PiPipeconf;
import org.onosproject.net.pi.model.PiPipelineInterpreter;
import org.onosproject.net.pi.model.PiPipelineModel;
import org.onosproject.p4runtime.model.P4InfoParser;
import org.onosproject.p4runtime.model.P4InfoParserException;
import org.onosproject.pipelines.fabric.FabricPipeconfService;
import org.onosproject.pipelines.fabric.impl.behaviour.FabricIntProgrammable;
import org.onosproject.pipelines.fabric.impl.behaviour.bng.FabricBngProgrammable;
import org.onosproject.pipelines.fabric.impl.behaviour.FabricInterpreter;
import org.onosproject.pipelines.fabric.impl.behaviour.pipeliner.FabricPipeliner;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.slf4j.Logger;

import java.net.URL;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Implementation of the FabricPipeconfService.
 */
@Component(immediate = true, service = FabricPipeconfService.class)
public final class FabricPipeconfManager implements FabricPipeconfService {

    private static final String INT_PROFILE_SUFFIX = "-int";
    private static final String FULL_PROFILE_SUFFIX = "-full";
    private static final String BNG_PROFILE_SUFFIX = "-bng";
    private static final String UPF_PROFILE_SUFFIX = "-spgw";

    private static Logger log = getLogger(FabricPipeconfLoader.class);

    @Activate
    public void activate() {
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        log.info("Stopped");
    }

    @Override
    public PiPipeconf buildFabricPipeconf(
            DefaultPiPipeconf.Builder builder, String profile, URL p4InfoUrl, URL cpuPortUrl) {
        return build(builder, profile, p4InfoUrl, cpuPortUrl);
    }

    static PiPipeconf build(
            DefaultPiPipeconf.Builder pipeconfBuilder,
            String profileName, URL p4InfoUrl, URL cpuPortUrl) {
        checkNotNull(pipeconfBuilder,
                     "pipeconfBuilder cannot be null");
        checkArgument(profileName != null && !profileName.isEmpty(),
                      "profileName cannot be null or empty");
        checkNotNull(p4InfoUrl,
                     "p4InfoUrl cannot be null (check if file exists)");
        checkNotNull(cpuPortUrl,
                     "cpuPortUrl cannot be null (check if file exists)");

        pipeconfBuilder
                .withPipelineModel(parseP4Info(p4InfoUrl))
                .addBehaviour(PiPipelineInterpreter.class, FabricInterpreter.class)
                .addBehaviour(Pipeliner.class, FabricPipeliner.class)
                .addExtension(PiPipeconf.ExtensionType.P4_INFO_TEXT, p4InfoUrl)
                .addExtension(PiPipeconf.ExtensionType.CPU_PORT_TXT, cpuPortUrl);

        // Add IntProgrammable behaviour for INT-enabled profiles.
        if (profileName.endsWith(INT_PROFILE_SUFFIX) ||
                profileName.endsWith(FULL_PROFILE_SUFFIX)) {
            pipeconfBuilder.addBehaviour(IntProgrammable.class, FabricIntProgrammable.class);
        }
        // Add BngProgrammable behavior for BNG-enabled pipelines.
        if (profileName.endsWith(BNG_PROFILE_SUFFIX)) {
            pipeconfBuilder.addBehaviour(BngProgrammable.class, FabricBngProgrammable.class);
        }
        return pipeconfBuilder.build();
    }

    private static PiPipelineModel parseP4Info(URL p4InfoUrl) {
        try {
            return P4InfoParser.parse(p4InfoUrl);
        } catch (P4InfoParserException e) {
            // FIXME: propagate exception that can be handled by whoever is
            //  trying to build pipeconfs.
            throw new IllegalStateException(e);
        }
    }
}
