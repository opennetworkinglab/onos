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
import org.onosproject.net.Device;
import org.onosproject.net.Port;


import org.onosproject.core.ApplicationId;

import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.device.DeviceAdminService;


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
@Command(scope = "onos", name = "restore-host-ports",
        description = "Reinstall every endpoint in clos")
public class CtpdRestoreHostPorts extends AbstractShellCommand {

    // reference to our service
    private ClosDeviceService service;
    private DeviceAdminService deviceAdminService;

    @Override
    protected void doExecute() {
        service = get(ClosDeviceService.class);
        deviceAdminService = get(DeviceAdminService.class);

        Set<Device> devices = service.getDevicesClosFwd();
        print("Restoring host ports: "+service.getHostPorts());
        for(Device device : devices){
            print("Device "+device.id());
            if(service.getHostPorts().get(device.id()) != null){
                Set<PortNumber> ports = service.getHostPorts().get(device.id()).value();
                for(PortNumber port : ports){
                    log.debug("Broughting cli UP port "+port);
                    print("Restoring port "+port);
                    deviceAdminService.changePortState(device.id(), port, true);

                }
			}
        }
    }
}