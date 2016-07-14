/*
 * Copyright 2015-present Open Networking Laboratory
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
package org.onosproject.vtnrsc.cli.floatingip;

import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.commands.Option;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.vtnrsc.FloatingIpId;
import org.onosproject.vtnrsc.FloatingIp;
import org.onosproject.vtnrsc.floatingip.FloatingIpService;

/**
 * Supports for query a floating IP.
 */
@Command(scope = "onos", name = "floatingips", description = "Supports for querying a floating IP")
public class FloatingIpQueryCommand extends AbstractShellCommand {
    @Option(name = "-I", aliases = "--id", description = "The floating IP identifier",
            required = false, multiValued = false)
    String id = null;

    @Option(name = "-i", aliases = "--fixedIp", description = "The fixed IP of floating IP",
            required = false, multiValued = false)
    String fixedIp = null;

    @Option(name = "-l", aliases = "--floatingIp", description = "The floating IP of floating IP",
            required = false, multiValued = false)
    String floatingIp = null;

    private static final String FMT = "floatingIpId=%s, networkId=%s, tenantId=%s, portId=%s,"
            + "routerId=%s, fixedIp=%s, floatingIp=%s, status=%s";

    @Override
    protected void execute() {
        FloatingIpService service = get(FloatingIpService.class);
        if (id != null) {
            FloatingIp floatingIp = service.getFloatingIp(FloatingIpId
                    .of(id));
            printFloatingIp(floatingIp);
        } else if (fixedIp != null || floatingIp != null) {
            Iterable<FloatingIp> floatingIps = service.getFloatingIps();
            if (floatingIps == null) {
                return;
            }
            if (fixedIp != null) {
                for (FloatingIp floatingIp : floatingIps) {
                    if (floatingIp.fixedIp().toString().equals(fixedIp)) {
                        printFloatingIp(floatingIp);
                        return;
                    }
                }
                print(null, "The fixedIp is not existed");
            }
            if (floatingIp != null) {
                for (FloatingIp floatingIpObj : floatingIps) {
                    if (floatingIpObj.fixedIp().toString().equals(floatingIp)) {
                        printFloatingIp(floatingIpObj);
                        return;
                    }
                }
                print(null, "The floatingIp is not existed");
            }
        } else {
            Iterable<FloatingIp> floatingIps = service.getFloatingIps();
            if (floatingIps == null) {
                return;
            }
            for (FloatingIp floatingIp : floatingIps) {
                printFloatingIp(floatingIp);
            }
        }
    }

    private void printFloatingIp(FloatingIp floatingIp) {
        print(FMT, floatingIp.id(), floatingIp.networkId(),
              floatingIp.tenantId(), floatingIp.portId(),
              floatingIp.routerId(), floatingIp.fixedIp(),
              floatingIp.floatingIp(), floatingIp.status());
    }
}
