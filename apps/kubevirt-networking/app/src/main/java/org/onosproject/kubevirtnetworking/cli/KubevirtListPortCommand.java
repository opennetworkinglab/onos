/*
 * Copyright 2021-present Open Networking Foundation
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
package org.onosproject.kubevirtnetworking.cli;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import org.apache.commons.lang.StringUtils;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.kubevirtnetworking.api.KubevirtPort;
import org.onosproject.kubevirtnetworking.api.KubevirtPortService;

import java.util.Comparator;
import java.util.List;

import static org.onosproject.kubevirtnetworking.api.Constants.CLI_IP_ADDRESSES_LENGTH;
import static org.onosproject.kubevirtnetworking.api.Constants.CLI_MAC_ADDRESS_LENGTH;
import static org.onosproject.kubevirtnetworking.api.Constants.CLI_MARGIN_LENGTH;
import static org.onosproject.kubevirtnetworking.api.Constants.CLI_NAME_LENGTH;
import static org.onosproject.kubevirtnetworking.util.KubevirtNetworkingUtil.genFormatString;
import static org.onosproject.kubevirtnetworking.util.KubevirtNetworkingUtil.prettyJson;

/**
 * Lists kubevirt ports.
 */
@Service
@Command(scope = "onos", name = "kubevirt-ports",
        description = "Lists all kubevirt ports")
public class KubevirtListPortCommand extends AbstractShellCommand {

    @Argument(name = "networkId", description = "Network ID")
    @Completion(KubevirtNetworkIdCompleter.class)
    private String networkId = null;

    @Override
    protected void doExecute() throws Exception {
        KubevirtPortService service = get(KubevirtPortService.class);

        List<KubevirtPort> ports = Lists.newArrayList(service.ports());
        ports.sort(Comparator.comparing(KubevirtPort::networkId));

        String format = genFormatString(ImmutableList.of(CLI_NAME_LENGTH,
                CLI_NAME_LENGTH, CLI_MAC_ADDRESS_LENGTH, CLI_IP_ADDRESSES_LENGTH));

        if (!Strings.isNullOrEmpty(networkId)) {
            ports.removeIf(port -> !port.networkId().equals(networkId));
        }

        if (outputJson()) {
            print("%s", json(ports));
        } else {
            print(format, "VM Name", "Network", "MAC Address", "Fixed IPs");
            for (KubevirtPort port: ports) {
                print(format,
                        StringUtils.substring(port.vmName(), 0,
                                CLI_NAME_LENGTH - CLI_MARGIN_LENGTH),
                        StringUtils.substring(port.networkId(), 0,
                                CLI_NAME_LENGTH - CLI_MARGIN_LENGTH),
                        StringUtils.substring(port.macAddress().toString(), 0,
                                CLI_MAC_ADDRESS_LENGTH - CLI_MARGIN_LENGTH),
                        port.ipAddress() == null ? "" : port.ipAddress());
            }
        }
    }

    private String json(List<KubevirtPort> ports) {
        ObjectMapper mapper = new ObjectMapper();
        ArrayNode result = mapper.createArrayNode();

        for (KubevirtPort port : ports) {
            result.add(jsonForEntity(port, KubevirtPort.class));
        }

        return prettyJson(mapper, result.toString());
    }
}
