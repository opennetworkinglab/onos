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

import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.commands.Option;

import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.incubator.net.tunnel.Tunnel;
import org.onosproject.net.AnnotationKeys;
import org.onosproject.pce.pceservice.api.PceService;

import org.slf4j.Logger;

/**
 * Supports quering PCE path.
 */
@Command(scope = "onos", name = "pce-query-path",
        description = "Supports querying PCE path.")
public class PceQueryPathCommand extends AbstractShellCommand {
    private final Logger log = getLogger(getClass());

    @Option(name = "-i", aliases = "--id", description = "path-id", required = false,
            multiValued = false)
    String id = null;

    @Override
    protected void execute() {
        log.info("executing pce-query-path");

        PceService service = get(PceService.class);
        if (null == id) {
            //TODO: need to uncomment below line once queryAllPath method is added to PceService
            Iterable<Tunnel> tunnels = null; // = service.queryAllPath();
            if (tunnels != null) {
                for (final Tunnel tunnel : tunnels) {
                    display(tunnel);
                }
            } else {
                print("No path is found.");
                return;
            }
        } else {
            //TODO: need to uncomment below line once queryPath method is added to PceService
            Tunnel tunnel = null; // = service.queryPath(PcePathId.of(id));
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
        print("\npath-id            : %d \n" +
                "source             : %s \n" +
                "destination        : %s \n" +
                "path-type          : %d \n" +
                "symbolic-path-name : %s \n" +
                "constraints:            \n" +
                "   cost            : %d \n" +
                "   bandwidth       : %.2f",
                tunnel.tunnelId().id(), tunnel.src().toString(), tunnel.dst().toString(),
                tunnel.type(), tunnel.tunnelName(), tunnel.path().cost(),
                tunnel.annotations().value(AnnotationKeys.BANDWIDTH));
    }
}
