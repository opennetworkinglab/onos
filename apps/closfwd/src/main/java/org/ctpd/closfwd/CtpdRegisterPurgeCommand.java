/*
 * Copyright 2014 Open Networking Laboratory
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

package org.ctpd.closfwd;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.Iterator;


import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.net.HostId;

import org.ctpd.closfwd.ClosDeviceService;
import org.ctpd.closfwd.Endpoint;

import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;

import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.onlab.util.XmlString;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.cli.net.DeviceIdCompleter;
import org.onosproject.cli.net.PortNumberCompleter;

/**
 * A demo service that lists the endpoints for which intents are installed.
 */
@Service
@Command(scope = "onos", name = "purge-clos",
        description = "Command to purge clos, force purge clos and purge-clos in product enviorement")
public class CtpdRegisterPurgeCommand extends AbstractShellCommand {

    // reference to our service
    private ClosDeviceService service;

    @Argument(index = 0, name = "proForce", description = "Force to purge clos and production enviorement config. Options valid: pro, force, pro-force and null",
            required = false, multiValued = false)
    private String proForce = null;

    @Override
    protected void doExecute() {
        service = get(ClosDeviceService.class);

        if (service.getProductionEnviorement() && proForce==null){
            print("Please, use 'purge-clos pro' or 'purge-clos pro-force' to purge a production enviorement");
        }else if (!service.getProductionEnviorement() && proForce==null){
            print("Registry + VpdcRegistry size: "+get(ClosDeviceService.class).purgeClos());
        }else if (service.getProductionEnviorement() && !(proForce.equals("pro") || proForce.equals("pro-force"))){
            print("Please, use 'purge-clos pro' or 'purge-clos pro-force' to purge a production enviorement");
        }else{
            if(proForce.equals("force") || proForce.equals("pro-force")){
                print("Permanent registry flowsIds size: "+get(ClosDeviceService.class).purgeFlowIdPermanentClos());
                print(get(ClosDeviceService.class).withdrawIntents());
            }else if (proForce.equals("pro")){
                print("Registry + VpdcRegistry size: "+get(ClosDeviceService.class).purgeClos());
            }else{
                print("Use 'purge-clos' or 'purge-clos force' and 'purge-clos pro' or 'purge-clos pro-force' when is a product enviorement");
            }
        }

    }
}