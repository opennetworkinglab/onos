/*
 * Copyright 2016 Open Networking Laboratory
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

package org.onosproject.olt.cli;

import org.apache.karaf.shell.commands.Command;
import org.onlab.packet.VlanId;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.net.ConnectPoint;
import org.onosproject.olt.AccessDeviceService;

import java.util.Map;

/**
 * Shows provisioned subscribers.
 */
@Command(scope = "onos", name = "subscribers",
        description = "Shows provisioned subscribers")
public class ShowSubscribersCommand extends AbstractShellCommand {

    private static final String FORMAT = "port=%s, cvlan=%s";

    @Override
    protected void execute() {
        AccessDeviceService service = AbstractShellCommand.get(AccessDeviceService.class);
        service.getSubscribers().forEach(this::display);
    }

    private void display(Map.Entry<ConnectPoint, VlanId> subscriber) {
        print(FORMAT, subscriber.getKey(), subscriber.getValue());
    }
}
