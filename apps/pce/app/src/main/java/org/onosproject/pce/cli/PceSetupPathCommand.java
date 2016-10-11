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

import java.util.Collection;
import java.util.List;
import java.util.LinkedList;

import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.commands.Option;

import org.onlab.util.DataRateUnit;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.incubator.net.tunnel.Tunnel;
import org.onosproject.incubator.net.tunnel.TunnelService;
import org.onosproject.net.DeviceId;
import org.onosproject.net.intent.constraint.BandwidthConstraint;
import org.onosproject.net.intent.Constraint;
import org.onosproject.pce.pceservice.constraint.CostConstraint;
import org.onosproject.pce.pceservice.LspType;
import org.onosproject.pce.pceservice.api.PceService;

import org.slf4j.Logger;

/**
 * Supports creating the pce path.
 */
@Command(scope = "onos", name = "pce-setup-path", description = "Supports creating pce path.")
public class PceSetupPathCommand extends AbstractShellCommand {
    private final Logger log = getLogger(getClass());

    @Argument(index = 0, name = "src", description = "source device.", required = true, multiValued = false)
    String src = null;

    @Argument(index = 1, name = "dst", description = "destination device.", required = true, multiValued = false)
    String dst = null;

    @Argument(index = 2, name = "type", description = "LSP type:" + " It includes "
            + "PCE tunnel with signalling in network (0), "
            + "PCE tunnel without signalling in network with segment routing (1), "
            + "PCE tunnel without signalling in network (2).",
            required = true, multiValued = false)
    int type = 0;

    @Argument(index = 3, name = "name", description = "symbolic-path-name.", required = true, multiValued = false)
    String name = null;

    @Option(name = "-c", aliases = "--cost", description = "The cost attribute IGP cost(1) or TE cost(2)",
            required = false, multiValued = false)
    int cost = 2;

    @Option(name = "-b", aliases = "--bandwidth", description = "The bandwidth attribute of path. "
            + "Data rate unit is in BPS.", required = false, multiValued = false)
    double bandwidth = 0.0;

    @Override
    protected void execute() {
        log.info("executing pce-setup-path");

        PceService service = get(PceService.class);
        TunnelService tunnelService = get(TunnelService.class);

        DeviceId srcDevice = DeviceId.deviceId(src);
        DeviceId dstDevice = DeviceId.deviceId(dst);
        List<Constraint> listConstrnt = new LinkedList<>();

        // LSP type validation
        if ((type < 0) || (type > 2)) {
           error("The LSP type value can be PCE tunnel with signalling in network (0), " +
                 "PCE tunnel without signalling in network with segment routing (1), " +
                 "PCE tunnel without signalling in network (2).");
           return;
        }
        LspType lspType = LspType.values()[type];

        //Validating tunnel name, duplicated tunnel names not allowed
        Collection<Tunnel> existingTunnels = tunnelService.queryTunnel(Tunnel.Type.MPLS);
        for (Tunnel t : existingTunnels) {
            if (t.tunnelName().toString().equals(name)) {
                error("Path creation failed, Tunnel name already exists");
                return;
            }
        }

        // Add bandwidth
        // bandwidth default data rate unit is in BPS
        if (bandwidth != 0.0) {
            listConstrnt.add(BandwidthConstraint.of(bandwidth, DataRateUnit.valueOf("BPS")));
        }

        // Add cost
        // Cost validation
        if ((cost < 1) || (cost > 2)) {
            error("The cost attribute value either IGP cost(1) or TE cost(2).");
            return;
        }
        // Here 'cost - 1' indicates the index of enum
        CostConstraint.Type costType = CostConstraint.Type.values()[cost - 1];
        listConstrnt.add(CostConstraint.of(costType));

        if (!service.setupPath(srcDevice, dstDevice, name, listConstrnt, lspType)) {
            error("Path creation failed.");
        }
    }
}
