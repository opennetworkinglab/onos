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
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.wiring.BundleWiring;
import org.slf4j.Logger;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URL;
import java.util.Collection;
import java.util.Objects;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static org.onosproject.net.pi.model.PiPipeconf.ExtensionType;
import static org.osgi.framework.wiring.BundleWiring.LISTRESOURCES_RECURSE;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Pipeconf loader for fabric.p4 which uses p4c output available in the resource
 * path to automatically build pipeconfs for different profiles, target and
 * platforms.
 */
@Component(immediate = true)
public class PipeconfLoader {

    // TODO: allow adding properties to pipeconf instead of adding it to driver

    private static Logger log = getLogger(PipeconfLoader.class);

    private static final String BASE_PIPECONF_ID = "org.onosproject.pipelines";

    private static final String P4C_OUT_PATH = "/p4c-out";

    // profile/target/platform
    private static final String P4C_RES_BASE_PATH = P4C_OUT_PATH + "/%s/%s/%s/";

    private static final String SEP = File.separator;
    private static final String TOFINO = "tofino";
    private static final String BMV2 = "bmv2";
    private static final String DEFAULT_PLATFORM = "default";
    private static final String BMV2_JSON = "bmv2.json";
    private static final String P4INFO_TXT = "p4info.txt";
    private static final String TOFINO_BIN = "tofino.bin";
    private static final String TOFINO_CTX_JSON = "context.json";

    private static final Collection<PiPipeconf> PIPECONFS = buildAllPipeconf();

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    private PiPipeconfService piPipeconfService;

    @Activate
    public void activate() {
        // Registers all pipeconf at component activation.
        PIPECONFS.forEach(piPipeconfService::register);
    }

    @Deactivate
    public void deactivate() {
        PIPECONFS.stream().map(PiPipeconf::id).forEach(piPipeconfService::remove);
    }

    private static Collection<PiPipeconf> buildAllPipeconf() {
        return FrameworkUtil
                .getBundle(PipeconfLoader.class)
                .adapt(BundleWiring.class)
                // List all resource files in /p4c-out
                .listResources(P4C_OUT_PATH, "*", LISTRESOURCES_RECURSE)
                .stream()
                // Filter only directories
                .filter(name -> name.endsWith(SEP))
                // Derive profile, target, and platform and build pipeconf.
                .map(PipeconfLoader::buildPipeconfFromPath)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private static PiPipeconf buildPipeconfFromPath(String path) {
        String[] pieces = path.split(SEP);
        // We expect a path of 4 elements, e.g.
        // p4c-out/<profile>/<target>/<platform>
        if (pieces.length != 4) {
            return null;
        }
        String profile = pieces[1];
        String target = pieces[2];
        String platform = pieces[3];
        try {
            switch (target) {
                case BMV2:
                    return buildBmv2Pipeconf(profile, platform);
                case TOFINO:
                    return buildTofinoPipeconf(profile, platform);
                default:
                    log.warn("Unknown target '{}', skipping pipeconf build...",
                             target);
                    return null;
            }
        } catch (FileNotFoundException e) {
            log.warn("Unable to build pipeconf at {} because one or more p4c outputs are missing",
                     path);
            return null;
        }
    }

    private static PiPipeconf buildBmv2Pipeconf(String profile, String platform)
            throws FileNotFoundException {
        final URL bmv2JsonUrl = PipeconfLoader.class.getResource(format(
                P4C_RES_BASE_PATH + BMV2_JSON, profile, BMV2, platform));
        final URL p4InfoUrl = PipeconfLoader.class.getResource(format(
                P4C_RES_BASE_PATH + P4INFO_TXT, profile, BMV2, platform));
        if (bmv2JsonUrl == null || p4InfoUrl == null) {
            throw new FileNotFoundException();
        }
        return basePipeconfBuilder(profile, platform, p4InfoUrl)
                .addExtension(ExtensionType.BMV2_JSON, bmv2JsonUrl)
                .build();
    }

    private static PiPipeconf buildTofinoPipeconf(String profile, String platform)
            throws FileNotFoundException {
        final URL tofinoBinUrl = PipeconfLoader.class.getResource(format(
                P4C_RES_BASE_PATH + TOFINO_BIN, profile, TOFINO, platform));
        final URL contextJsonUrl = PipeconfLoader.class.getResource(format(
                P4C_RES_BASE_PATH + TOFINO_CTX_JSON, profile, TOFINO, platform));
        final URL p4InfoUrl = PipeconfLoader.class.getResource(format(
                P4C_RES_BASE_PATH + P4INFO_TXT, profile, TOFINO, platform));
        if (tofinoBinUrl == null || contextJsonUrl == null || p4InfoUrl == null) {
            throw new FileNotFoundException();
        }
        return basePipeconfBuilder(profile, platform, p4InfoUrl)
                .addExtension(ExtensionType.TOFINO_BIN, tofinoBinUrl)
                .addExtension(ExtensionType.TOFINO_CONTEXT_JSON, contextJsonUrl)
                .build();
    }

    private static DefaultPiPipeconf.Builder basePipeconfBuilder(
            String profile, String platform, URL p4InfoUrl) {
        final String pipeconfId = platform.equals(DEFAULT_PLATFORM)
                // Omit platform if default, e.g. with BMv2 pipeconf
                ? format("%s.%s", BASE_PIPECONF_ID, profile)
                : format("%s.%s.%s", BASE_PIPECONF_ID, profile, platform);
        final PiPipelineModel model = parseP4Info(p4InfoUrl);
        return DefaultPiPipeconf.builder()
                .withId(new PiPipeconfId(pipeconfId))
                .withPipelineModel(model)
                .addBehaviour(PiPipelineInterpreter.class,
                              FabricInterpreter.class)
                .addBehaviour(Pipeliner.class,
                              FabricPipeliner.class)
                .addBehaviour(PortStatisticsDiscovery.class,
                              FabricPortStatisticsDiscovery.class)
                .addExtension(ExtensionType.P4_INFO_TEXT, p4InfoUrl);
    }

    private static PiPipelineModel parseP4Info(URL p4InfoUrl) {
        try {
            return P4InfoParser.parse(p4InfoUrl);
        } catch (P4InfoParserException e) {
            throw new IllegalStateException(e);
        }
    }
}
