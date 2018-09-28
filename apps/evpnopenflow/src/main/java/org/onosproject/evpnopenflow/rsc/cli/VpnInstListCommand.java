/*
 * Copyright 2017-present Open Networking Foundation
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

package org.onosproject.evpnopenflow.rsc.cli;

import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.evpnopenflow.rsc.EvpnConstants;
import org.onosproject.evpnopenflow.rsc.VpnInstance;
import org.onosproject.evpnopenflow.rsc.vpninstance.VpnInstanceService;

import java.util.Collection;

/**
 * Support for displaying EVPN VPN instances.
 */
@Service
@Command(scope = "onos", name = "evpn-instance-list", description = "Lists " +
        "all EVPN instances")
public class VpnInstListCommand extends AbstractShellCommand {

    @Override
    protected void doExecute() {
        VpnInstanceService service = get(VpnInstanceService.class);
        Collection<VpnInstance> vpnInstances = service
                .getInstances();
        vpnInstances.forEach(vpnInstance -> {
            print(EvpnConstants.FORMAT_VPN_INSTANCE, vpnInstance.id(),
                  vpnInstance.description(),
                  vpnInstance.vpnInstanceName(),
                  vpnInstance.routeDistinguisher(),
                  vpnInstance.getExportRouteTargets(),
                  vpnInstance.getImportRouteTargets());
        });
    }

}
