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

import org.onosproject.core.CoreService;
import org.onosproject.net.device.PortStatisticsDiscovery;
import org.onosproject.net.pi.model.DefaultPiPipeconf;
import org.onosproject.net.pi.model.PiPipeconf;
import org.onosproject.net.pi.model.PiPipeconfId;
import org.onosproject.net.pi.service.PiPipeconfService;
import org.onosproject.pipelines.fabric.impl.behaviour.FabricPortStatisticsDiscovery;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URL;
import java.util.Collection;
import java.util.Objects;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static org.onosproject.pipelines.fabric.impl.FabricPipeconfManager.build;
import static org.osgi.framework.wiring.BundleWiring.LISTRESOURCES_RECURSE;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Component responsible of building and registering fabric pipeconfs at app
 * activation.
 * <p>
 * This implementation looks at the content of the resource path for p4c output,
 * automatically building different pipeconfs for different profiles, target and
 * platforms.
 */
@Component(immediate = true)
public final class FabricPipeconfLoader {

    public static final String PIPELINE_APP_NAME = "org.onosproject.pipelines.fabric";
    public static final String PIPELINE_APP_NAME_UPF = "org.onosproject.pipelines.fabric.upf";

    private static Logger log = getLogger(FabricPipeconfLoader.class);

    private static final String SEP = File.separator;
    private static final String SPECTRUM = "spectrum";
    private static final String BMV2 = "bmv2";
    private static final String DEFAULT_PLATFORM = "default";
    private static final String BMV2_JSON = "bmv2.json";
    private static final String P4INFO_TXT = "p4info.txt";
    private static final String CPU_PORT_TXT = "cpu_port.txt";
    private static final String SPECTRUM_BIN = "spectrum.bin";

    private static final String BASE_PIPECONF_ID = "org.onosproject.pipelines";
    private static final String P4C_OUT_PATH = "/p4c-out";
    // p4c-out/<profile>/<target>/<platform>
    private static final String P4C_RES_BASE_PATH = P4C_OUT_PATH + "/%s/%s/%s/";

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    private PiPipeconfService piPipeconfService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    private CoreService coreService;

    private Collection<PiPipeconf> pipeconfs;


    @Activate
    public void activate() {
        coreService.registerApplication(PIPELINE_APP_NAME);
        coreService.registerApplication(PIPELINE_APP_NAME_UPF);
        // Registers all pipeconf at component activation.
        pipeconfs = buildAllPipeconfs();
        pipeconfs.forEach(piPipeconfService::register);
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        pipeconfs.stream()
                .map(PiPipeconf::id)
                .forEach(piPipeconfService::unregister);
        pipeconfs = null;
        log.info("Stopped");
    }

    private Collection<PiPipeconf> buildAllPipeconfs() {
        return FrameworkUtil
                .getBundle(this.getClass())
                .adapt(BundleWiring.class)
                // List all resource files in /p4c-out
                .listResources(P4C_OUT_PATH, "*", LISTRESOURCES_RECURSE)
                .stream()
                // Filter only directories
                .filter(name -> name.endsWith(SEP))
                // Derive profile, target, and platform and build pipeconf.
                .map(this::buildPipeconfFromPath)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private PiPipeconf buildPipeconfFromPath(String path) {
        String[] pieces = path.split(SEP);
        // We expect a path of 4 elements, e.g.
        // p4c-out/<profile>/<target>/<platform>
        if (pieces.length != 4) {
            return null;
        }
        String profile = pieces[1];
        String target = pieces[2];
        String platform = pieces[3];
        final PiPipeconf pipeconf;
        try {
            switch (target) {
                case BMV2:
                    pipeconf = bmv2Pipeconf(profile, platform);
                    break;
                case SPECTRUM:
                    pipeconf = spectrumPipeconf(profile, platform);
                    break;
                default:
                    log.warn("Unknown target '{}', skipping pipeconf build...",
                             target);
                    return null;
            }
        } catch (FileNotFoundException e) {
            log.warn("Unable to build pipeconf at {} because file is missing: {}",
                     path, e.getMessage());
            return null;
        }

        return pipeconf;
    }

    private PiPipeconf bmv2Pipeconf(String profile, String platform)
            throws FileNotFoundException {
        final URL bmv2JsonUrl = this.getClass().getResource(format(
                P4C_RES_BASE_PATH + BMV2_JSON, profile, BMV2, platform));
        final URL p4InfoUrl = this.getClass().getResource(format(
                P4C_RES_BASE_PATH + P4INFO_TXT, profile, BMV2, platform));
        final URL cpuPortUrl = this.getClass().getResource(format(
                P4C_RES_BASE_PATH + CPU_PORT_TXT, profile, BMV2, platform));

        checkFileExists(bmv2JsonUrl, BMV2_JSON);
        checkFileExists(p4InfoUrl, P4INFO_TXT);
        checkFileExists(cpuPortUrl, CPU_PORT_TXT);

        final DefaultPiPipeconf.Builder builder = DefaultPiPipeconf.builder()
                .withId(makePipeconfId(platform, profile))
                .addBehaviour(PortStatisticsDiscovery.class,
                              FabricPortStatisticsDiscovery.class)
                .addExtension(PiPipeconf.ExtensionType.BMV2_JSON, bmv2JsonUrl);

        return build(builder, profile, p4InfoUrl, cpuPortUrl);
    }

    // FIXME: this method should be removed. Instead, there should be a
    //  third-party app using the FabricPipeconfService to register a
    //  Mellanox-specific version of the fabric pipeconfs.
    private PiPipeconf spectrumPipeconf(String profile, String platform)
            throws FileNotFoundException {

        final URL spectrumBinUrl = this.getClass().getResource(format(
                P4C_RES_BASE_PATH + SPECTRUM_BIN, profile, SPECTRUM, platform));
        final URL p4InfoUrl = this.getClass().getResource(format(
                P4C_RES_BASE_PATH + P4INFO_TXT, profile, SPECTRUM, platform));
        final URL cpuPortUrl = this.getClass().getResource(format(
                P4C_RES_BASE_PATH + CPU_PORT_TXT, profile, SPECTRUM, platform));

        checkFileExists(spectrumBinUrl, SPECTRUM_BIN);
        checkFileExists(p4InfoUrl, P4INFO_TXT);
        checkFileExists(cpuPortUrl, CPU_PORT_TXT);

        final DefaultPiPipeconf.Builder builder = DefaultPiPipeconf.builder()
                .withId(makePipeconfId(platform, profile))
                .addExtension(PiPipeconf.ExtensionType.SPECTRUM_BIN, spectrumBinUrl);

        return build(builder, profile, p4InfoUrl, cpuPortUrl);
    }

    private void checkFileExists(URL url, String name)
            throws FileNotFoundException {
        if (url == null) {
            throw new FileNotFoundException(name);
        }
    }

    private PiPipeconfId makePipeconfId(String platform, String profile) {
        final String id = platform.equals(DEFAULT_PLATFORM)
                // Omit platform if default, e.g. with BMv2 pipeconf
                ? format("%s.%s", BASE_PIPECONF_ID, profile)
                : format("%s.%s.%s", BASE_PIPECONF_ID, profile, platform);
        return new PiPipeconfId(id);
    }
}
