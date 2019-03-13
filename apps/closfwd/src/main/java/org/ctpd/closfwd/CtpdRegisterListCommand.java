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
import com.fasterxml.jackson.databind.ObjectMapper;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.DefaultApplicationId;

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
@Command(scope = "onos", name = "clos-registry-list",
        description = "Access the content of CTPD registry")
public class CtpdRegisterListCommand extends AbstractShellCommand {

    // the String to hold the optional argument
    @Argument(index = 0, name = "uuid", description = "UUID of the requested entry. If not provided, list of entry keys will be printed.",
            required = false, multiValued = false)
    private String uuid = null;

    // reference to our service
    private ClosDeviceService service;

    @Override
    protected void doExecute() {
        service = get(ClosDeviceService.class);
        if(uuid==null)
        {
            Iterator<UUID> uuids = service.getRegisterUUIDs().iterator();
            while(uuids.hasNext())
            {
                UUID uuid = (UUID) uuids.next();
                Endpoint endpoint = service.getEndpoint(uuid);
                ApplicationId appId = service.getApplicationFlowId(endpoint);
                print(String.format("UUID:%40s  Vlan:%8s  Mac:%20s  Type:%30s  ApplicationId:%6s",uuid.toString(), endpoint.getVlan().toString(), endpoint.getMac(),
                endpoint.getClass().getName().substring(endpoint.getClass().getName().lastIndexOf('.') + 1), appId.id()));
            }
        }
        else
        {
            Endpoint endpoint = service.getEndpoint(UUID.fromString(uuid));
            if(endpoint!=null)
            {
                print(endpoint.toString());
            }
        }
    }
}




