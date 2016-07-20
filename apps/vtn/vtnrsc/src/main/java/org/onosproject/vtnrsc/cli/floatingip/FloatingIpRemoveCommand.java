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

import java.util.Set;

import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.commands.Option;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.vtnrsc.FloatingIp;
import org.onosproject.vtnrsc.FloatingIpId;
import org.onosproject.vtnrsc.floatingip.FloatingIpService;

import com.google.common.collect.Sets;

/**
 * Supports for remove a floating IP.
 */
@Command(scope = "onos", name = "floatingip-remove", description = "Supports for removing a floating IP")
public class FloatingIpRemoveCommand extends AbstractShellCommand {
    @Option(name = "-I", aliases = "--id", description = "The floating IP identifier",
            required = false, multiValued = false)
    String id = null;

    @Option(name = "-i", aliases = "--fixedIp", description = "The fixed IP of floating IP",
            required = false, multiValued = false)
    String fixedIp = null;

    @Option(name = "-l", aliases = "--floatingIp", description = "The floating IP of floating IP",
            required = false, multiValued = false)
    String floatingIp = null;

    @Override
    protected void execute() {
        FloatingIpService service = get(FloatingIpService.class);
        if (id == null && fixedIp == null && floatingIp == null) {
            print(null, "one of id, fixedIp, floatingIp should not be null");
        }
        try {
            Set<FloatingIpId> floatingIpSet = Sets.newHashSet();
            if (id != null) {
                floatingIpSet.add(FloatingIpId.of(id));
                service.removeFloatingIps(floatingIpSet);
            } else {
                Iterable<FloatingIp> floatingIps = service.getFloatingIps();
                if (floatingIps == null) {
                    return;
                }
                if (fixedIp != null) {
                    for (FloatingIp floatingIp : floatingIps) {
                        if (floatingIp.fixedIp().toString().equals(fixedIp)) {
                            floatingIpSet.add(floatingIp.id());
                            service.removeFloatingIps(floatingIpSet);
                            return;
                        }
                    }
                    print(null, "The fixedIp is not existed");
                    return;
                }
                if (floatingIp != null) {
                    for (FloatingIp floatingIpObj : floatingIps) {
                        if (floatingIpObj.fixedIp().toString()
                                .equals(floatingIp)) {
                            floatingIpSet.add(floatingIpObj.id());
                            service.removeFloatingIps(floatingIpSet);
                            return;
                        }
                    }
                    print(null, "The floatingIp is not existed");
                    return;
                }
            }
        } catch (Exception e) {
            print(null, e.getMessage());
        }
    }
}
