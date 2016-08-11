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
package org.onosproject.pce.cli;

import static org.slf4j.LoggerFactory.getLogger;

import java.util.List;
import java.util.LinkedList;

import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.commands.Option;

import org.onlab.util.DataRateUnit;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.incubator.net.tunnel.TunnelId;
import org.onosproject.net.intent.constraint.BandwidthConstraint;
import org.onosproject.net.intent.Constraint;
import org.onosproject.pce.pceservice.constraint.CostConstraint;
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
    Integer cost = null;

    @Option(name = "-b", aliases = "--bandwidth", description = "The bandwidth attribute of path. "
            + "Data rate unit is in Bps.", required = false, multiValued = false)
    Double bandwidth = null;

    @Override
    protected void execute() {
        log.info("executing pce-update-path");

        PceService service = get(PceService.class);

        List<Constraint> constrntList = new LinkedList<>();
        // Assign bandwidth. Data rate unit is in Bps.
        if (bandwidth != null) {
            constrntList.add(BandwidthConstraint.of(Double.valueOf(bandwidth), DataRateUnit.valueOf("BPS")));
        }

        // Cost validation
        if (cost != null) {
            if ((cost < 1) || (cost > 2)) {
                error("The cost attribute value is either IGP cost(1) or TE cost(2).");
                return;
            }
            CostConstraint.Type costType = CostConstraint.Type.values()[cost - 1];
            constrntList.add(CostConstraint.of(costType));
        }

        if (!service.updatePath(TunnelId.valueOf(id), constrntList)) {
            error("Path updation failed.");
            return;
        }
    }
}
