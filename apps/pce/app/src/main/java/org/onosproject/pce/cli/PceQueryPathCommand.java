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

import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.commands.Option;

import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.incubator.net.tunnel.Tunnel;
import org.onosproject.incubator.net.tunnel.TunnelId;
import org.onosproject.net.AnnotationKeys;
import org.onosproject.pce.pceservice.ExplicitPathInfo;
import org.onosproject.pce.pceservice.api.PceService;

import org.slf4j.Logger;

import java.util.List;

/**
 * Supports quering PCE path.
 */
@Command(scope = "onos", name = "pce-query-path",
        description = "Supports querying PCE path.")
public class PceQueryPathCommand extends AbstractShellCommand {
    private final Logger log = getLogger(getClass());
    public static final String COST_TYPE = "costType";

    @Option(name = "-i", aliases = "--id", description = "path-id", required = false,
            multiValued = false)
    String id = null;

    @Override
    protected void execute() {
        log.info("executing pce-query-path");

        PceService service = get(PceService.class);
        if (null == id) {
            Iterable<Tunnel> tunnels = service.queryAllPath();
            if (tunnels != null) {
                for (final Tunnel tunnel : tunnels) {
                    display(tunnel);
                }
            } else {
                print("No path is found.");
                return;
            }
        } else {
            Tunnel tunnel = service.queryPath(TunnelId.valueOf(id));
            if (tunnel == null) {
                print("Path doesnot exists.");
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
        List<ExplicitPathInfo> explicitPathInfoList = AbstractShellCommand.get(PceService.class)
                .explicitPathInfoList(tunnel.tunnelName().value());

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
        if (explicitPathInfoList != null) {
            for (ExplicitPathInfo e : explicitPathInfoList) {
                print("explicitPathObjects      : \n" +
                      "    type                 : %s \n" +
                      "    value                : %s ",
                      String.valueOf(e.type().type()), e.value().toString());
            }
        }
    }
}
