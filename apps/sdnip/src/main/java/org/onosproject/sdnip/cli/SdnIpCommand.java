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
package org.onosproject.sdnip.cli;

import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.EncapsulationType;
import org.onosproject.net.config.NetworkConfigService;
import org.onosproject.sdnip.SdnIp;
import org.onosproject.sdnip.config.SdnIpConfig;

/**
 * CLI to interact with the SDN-IP application.
 */
@Command(scope = "onos", name = "sdnip",
        description = "Manages the SDN-IP application")
public class SdnIpCommand extends AbstractShellCommand {

    // Color codes and style
    private static final String BOLD = "\u001B[1m";
    private static final String COLOR_ERROR = "\u001B[31m";
    private static final String RESET = "\u001B[0m";

    // Messages and string formatter
    private static final String ENCAP_NOT_FOUND =
            COLOR_ERROR + "Encapsulation type " + BOLD + "%s" + RESET +
                    COLOR_ERROR + " not found" + RESET;

    private static final String SDNIP_COMMAND_NOT_FOUND =
            COLOR_ERROR + "SDN-IP command " + BOLD + "%s" + RESET + COLOR_ERROR +
                    " not found" + RESET;

    @Argument(index = 0, name = "command", description = "Command name" +
            " {set-encap}",
            required = true, multiValued = false)
    String command = null;

    @Argument(index = 1, name = "encapType", description = "The encapsulation" +
            " type {NONE | VLAN | MPLS}",
            required = true, multiValued = false)
    String encapType = null;

    @Override
    protected void execute() {
        if (command != null) {
            switch (command) {
                case "set-encap":
                    setEncap(encapType);
                    break;
                default:
                    print(SDNIP_COMMAND_NOT_FOUND, command);
            }
        }
    }

    /**
     * Sets the encapsulation type for SDN-IP.
     *
     * @param encap the encapsulation type
     */
    private void setEncap(String encap) {
        EncapsulationType encapType = EncapsulationType.enumFromString(encap);
        if (encapType.equals(EncapsulationType.NONE) &&
                !encapType.toString().equals(encap)) {
            print(ENCAP_NOT_FOUND, encap);
            return;
        }

        NetworkConfigService configService = get(NetworkConfigService.class);
        CoreService coreService = get(CoreService.class);
        ApplicationId appId = coreService.getAppId(SdnIp.SDN_IP_APP);

        SdnIpConfig config = configService.addConfig(appId, SdnIpConfig.class);

        config.setEncap(encapType);
        config.apply();

        //configService.applyConfig(appId, SdnIpConfig.class, config.node());
    }
}
