/*
 * Copyright 2016 Open Networking Laboratory
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
package org.onosproject.pce.cli;

import static org.slf4j.LoggerFactory.getLogger;

import java.util.List;
import java.util.LinkedList;

import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.commands.Option;

import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.net.intent.Constraint;
import org.onosproject.pce.pceservice.api.PceService;

import org.slf4j.Logger;

/**
 * Supports updating the PCE path.
 */
@Command(scope = "onos", name = "pce-update-path",
        description = "Supports updating PCE path.")
public class PceUpdatePathCommand extends AbstractShellCommand {
    private final Logger log = getLogger(getClass());

    @Argument(index = 0, name = "id", description = "Path Id.", required = true, multiValued = false)
    String id = null;

    @Option(name = "-c", aliases = "--cost", description = "The cost attribute IGP cost (1) or TE cost (2).",
            required = false, multiValued = false)
    int cost = 0;

    @Option(name = "-b", aliases = "--bandwidth", description = "The bandwidth attribute of path. "
            + "Data rate unit is in Bps.", required = false, multiValued = false)
    double bandwidth = 0.0;

    @Override
    protected void execute() {
        log.info("executing pce-update-path");

        PceService service = get(PceService.class);

        List<Constraint> constrntList = new LinkedList<>();
        // Assign cost
        if (cost != 0) {
            //TODO: need to uncomment below lines once CostConstraint is ready
            //CostConstraint.Type costType = CostConstraint.Type.values()[Integer.valueOf(cost)];
            //constrntList.add(CostConstraint.of(costType));
        }

        // Assign bandwidth. Data rate unit is in Bps.
        if (bandwidth != 0.0) {
            //TODO: need to uncomment below line once BandwidthConstraint is ready
            //constrntList.add(LocalBandwidthConstraint.of(Double.valueOf(bandwidth), DataRateUnit.valueOf("BPS")));
        }

        //TODO: need to uncomment below lines once updatePath method is added to PceService
        //if (null == service.updatePath(PcePathId.of(id), constrntList)) {
        //    error("Path updation failed.");
        //    return;
        //}
    }
}
