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
package org.onosproject.cli.net;

import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.net.config.NetworkConfigService;
import org.onosproject.net.config.basics.BasicRegionConfig;
import org.onosproject.net.region.RegionId;

/**
 * Annotate a region with a peer location. That is, when rendering the
 * first region, where should the second (peer) region node be
 * located on the layout. An example:
 * <pre>
 *     region-add-peer-loc rUK rES 50.4060 -3.3860
 * </pre>
 * When rendering the rUK region, the rES peer region node should be located
 * at latitude 50.4060 and longitude -3.3860.
 * <pre>
 *     region-add-peer-loc rUK rES 100.0 200.0 grid
 * </pre>
 * When rendering the rUK region, the rES peer region node should be located
 * at grid-Y 100 and grid-X 200.
 *
 */
@Service
@Command(scope = "onos", name = "region-add-peer-loc",
        description = "Adds a peer location annotation to a region.")
public class RegionAddPeerLocCommand extends AbstractShellCommand {

    private static final String GEO = "geo";
    private static final String GRID = "grid";

    @Argument(index = 0, name = "id", description = "Region ID",
            required = true, multiValued = false)
    @Completion(RegionIdCompleter.class)
    String id = null;

    @Argument(index = 1, name = "peer", description = "Peer region ID",
            required = true, multiValued = false)
    String peerId = null;

    @Argument(index = 2, name = "latOrY",
            description = "Geo latitude / Grid y-coord",
            required = true, multiValued = false)
    Double latOrY = null;

    @Argument(index = 3, name = "longOrX",
            description = "Geo longitude / Grid x-coord",
            required = true, multiValued = false)
    Double longOrX = null;

    @Argument(index = 4, name = "locType", description = "Location type {geo|grid}",
            required = false, multiValued = false)
    String locType = GEO;

    @Override
    protected void doExecute() {
        RegionId regionId = RegionId.regionId(id);

        NetworkConfigService cfgService = get(NetworkConfigService.class);
        BasicRegionConfig cfg = cfgService.getConfig(regionId, BasicRegionConfig.class);

        cfg.addPeerLocMapping(peerId, locType, latOrY, longOrX)
                .apply();
    }
}
