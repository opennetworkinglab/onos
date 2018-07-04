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
package org.onosproject.openstackvtap.cli;

import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.openstackvtap.api.OpenstackVtap;
import org.onosproject.openstackvtap.api.OpenstackVtapService;

import java.util.Set;

import static org.onosproject.openstackvtap.util.OpenstackVtapUtil.getVtapTypeFromString;

/**
 * Command line interface for listing openstack vTap rules.
 */
@Command(scope = "onos", name = "openstack-vtap-list",
        description = "OpenstackVtap list")
public class OpenstackVtapListCommand extends AbstractShellCommand {

    private final OpenstackVtapService vTapService = get(OpenstackVtapService.class);

    @Argument(index = 0, name = "type",
            description = "vTap type [all|tx|rx]",
            required = false, multiValued = false)
    String vTapType = "none";

    private static final String FORMAT = "ID { %s }: type [%s], srcIP [%s], dstIP [%s]";
    private static final String FORMAT_TX_DEVICES  = "   tx devices: %s";
    private static final String FORMAT_RX_DEVICES  = "   rx devices: %s";

    @Override
    protected void execute() {
        OpenstackVtap.Type type = getVtapTypeFromString(vTapType);
        Set<OpenstackVtap> openstackVtaps = vTapService.getVtaps(type);
        for (OpenstackVtap vTap : openstackVtaps) {
            print(FORMAT,
                    vTap.id().toString(),
                    vTap.type().toString(),
                    vTap.vTapCriterion().srcIpPrefix().toString(),
                    vTap.vTapCriterion().dstIpPrefix().toString());
            print(FORMAT_TX_DEVICES, vTap.txDeviceIds());
            print(FORMAT_RX_DEVICES, vTap.rxDeviceIds());
        }
    }
}
