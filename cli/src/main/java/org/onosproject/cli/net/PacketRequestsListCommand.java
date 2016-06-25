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
package org.onosproject.cli.net;

import org.apache.karaf.shell.commands.Command;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.net.packet.PacketRequest;
import org.onosproject.net.packet.PacketService;

/**
 * Lists packet requests.
 */
@Command(scope = "onos", name = "packet-requests",
        description = "Lists packet requests")
public class PacketRequestsListCommand extends AbstractShellCommand {

    private static final String FMT = "nodeId=%s appId=%s, priority=%s, criteria=%s";

    @Override
    protected void execute() {
        PacketService service = get(PacketService.class);
        if (outputJson()) {
            // TODO: implement this
            print("Not implemented.");
        } else {
            service.getRequests().forEach(this::print);
        }
    }

    private void print(PacketRequest request) {
        print(FMT, request.nodeId(), request.appId().name(), request.priority(), request.selector().criteria());
    }

}
