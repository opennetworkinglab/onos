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

@Service
@Command(scope = "onos", name = "purge-withdraw-intents",
    description = "Purge withdraw-intents and return size of intents registry")
public class CtpdIntentWithdrawCommand extends AbstractShellCommand {

    // reference to our service
    private ClosDeviceService service;

    @Override
    protected void doExecute() {
        print(get(ClosDeviceService.class).withdrawIntents());
    }
}