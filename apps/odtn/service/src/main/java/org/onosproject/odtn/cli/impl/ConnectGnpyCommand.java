/*
 * Copyright 2018-present Open Networking Foundation
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

package org.onosproject.odtn.cli.impl;

import com.google.common.annotations.Beta;
import com.google.common.base.Preconditions;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.odtn.GnpyService;
import org.slf4j.Logger;

import static org.slf4j.LoggerFactory.getLogger;

@Beta
@Service
@Command(scope = "onos", name = "odtn-connect-gnpy-command",
        description = "show tapi context command")
public class ConnectGnpyCommand extends AbstractShellCommand {

    private static final Logger log = getLogger(ConnectGnpyCommand.class);

    @Argument(index = 0, name = "protocol",
            description = "protocol for requests, e.g. HTTP or HTTPS",
            required = true, multiValued = false)
    String protocol = "";

    @Argument(index = 1, name = "ip",
            description = "Ip of GNPy instance",
            required = true, multiValued = false)
    String ip = "";

    @Argument(index = 2, name = "port",
            description = "Protocol of GNPy instance",
            required = true, multiValued = false)
    String port = "";

    @Argument(index = 3, name = "username",
            description = "Username of GNPy instance",
            required = true, multiValued = false)
    String username = "";

    @Argument(index = 4, name = "password",
            description = "Password of GNPy instance",
            required = true, multiValued = false)
    String password = "";

    @Option(name = "-d", aliases = "--disconnect",
            description = "If this argument is passed the connection with gNPY is removed.",
            required = false, multiValued = false)
    private boolean disconnect = false;

    @Override
    protected void doExecute() {
        GnpyService service = get(GnpyService.class);
        String msg;
        if (disconnect) {
            msg = service.disconnectGnpy() ? "gnpy disconnect" : "error in disconnecting gnpy";
        } else {
            Preconditions.checkNotNull(ip, "Ip must be specified");
            Preconditions.checkNotNull(password, "password must be specified");
            msg = service.connectGnpy(protocol, ip, port, username, password) ? "gnpy connected" :
                    "error in connecting gnpy, please check logs";
        }
        print(msg);
    }

}
