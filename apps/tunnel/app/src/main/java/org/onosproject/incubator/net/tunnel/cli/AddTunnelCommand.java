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
package org.onosproject.incubator.net.tunnel.cli;

import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.incubator.net.domain.IntentDomainId;
import org.onosproject.incubator.net.domain.IntentDomainService;
import org.onosproject.incubator.net.domain.TunnelPrimitive;
import org.onosproject.net.ConnectPoint;

import java.util.NoSuchElementException;

/**
 * Installs intent domain tunnel primitive.
 */
@Service
@Command(scope = "onos", name = "add-domain-tunnel",
         description = "Installs intent domain tunnel primitive")
public class AddTunnelCommand extends AbstractShellCommand {

    @Argument(index = 0, name = "one",
              description = "Port one",
              required = true, multiValued = false)
    String oneString = null;

    @Argument(index = 1, name = "two",
              description = "Port two",
              required = true, multiValued = false)
    String twoString = null;

    @Override
    protected void doExecute() {
        IntentDomainService service = get(IntentDomainService.class);

        ConnectPoint one = ConnectPoint.deviceConnectPoint(oneString);
        ConnectPoint two = ConnectPoint.deviceConnectPoint(twoString);

        TunnelPrimitive tunnel = new TunnelPrimitive(appId(), one, two);

        // get the first domain (there should only be one)
        final IntentDomainId domainId;
        try {
            domainId = service.getDomains().iterator().next().id();
        } catch (NoSuchElementException | NullPointerException e) {
            print("No domains found");
            return;
        }

        service.request(domainId, tunnel).forEach(r -> service.submit(domainId, r));

        print("Intent domain tunnel submitted:\n%s", tunnel);
    }
}
