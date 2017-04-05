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

import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;

import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.incubator.net.tunnel.Tunnel;
import org.onosproject.incubator.net.tunnel.TunnelId;
import org.onosproject.net.AnnotationKeys;
import org.onosproject.pce.pceservice.api.PceService;

import org.slf4j.Logger;

import java.util.List;

/**
 * Supports quering PCE load balanced path.
 */
@Command(scope = "onos", name = "pce-query-load-balancing-path",
        description = "Supports querying PCE path.")
public class PceQueryLoadBalancingPathCommand extends AbstractShellCommand {
    private final Logger log = getLogger(getClass());
    public static final String COST_TYPE = "costType";

    @Argument(index = 0, name = "pathName", description = "load balencing path name", required = true,
            multiValued = false)
    String name = null;

    @Override
    protected void execute() {
        log.info("executing pce-query-load-balancing-path");

        PceService service = get(PceService.class);

        if (name == null) {
            print("Path name is mandatory");
            return;
        }

        List<TunnelId> tunnelIds = service.queryLoadBalancingPath(name);
        if (tunnelIds == null || tunnelIds.isEmpty()) {
            print("Release path failed");
            return;
        }

        for (TunnelId id : tunnelIds) {
            Tunnel tunnel = service.queryPath(id);
            if (tunnel == null) {
                print("Path doesnot exists");
                return;
            }
            display(tunnel);
        }
    }

    /**
     * Display tunnel information on the terminal.
     *
     * @param tunnel pce tunnel
     */
    void display(Tunnel tunnel) {

        print("\npath-id                  : %s \n" +
                "source                   : %s \n" +
                "destination              : %s \n" +
                "path-type                : %s \n" +
                "symbolic-path-name       : %s \n" +
                "constraints:            \n" +
                "   cost                  : %s \n" +
                "   bandwidth             : %s",
                tunnel.tunnelId().id(), tunnel.path().src().deviceId().toString(),
                tunnel.path().dst().deviceId().toString(),
                tunnel.type().name(), tunnel.tunnelName(), tunnel.annotations().value(COST_TYPE),
                tunnel.annotations().value(AnnotationKeys.BANDWIDTH));
        print("Path                     : %s", tunnel.path().toString());
    }
}
