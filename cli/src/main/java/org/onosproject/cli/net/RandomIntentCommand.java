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
package org.onosproject.cli.net;

import com.google.common.collect.Lists;
import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.Host;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.host.HostService;
import org.onosproject.net.intent.HostToHostIntent;
import org.onosproject.net.intent.Intent;
import org.onosproject.net.intent.IntentService;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Installs point-to-point connectivity intents.
 */
@Command(scope = "onos", name = "push-random-intents",
         description = "Installs random intents to test throughput")
public class RandomIntentCommand extends AbstractShellCommand {

    @Argument(index = 0, name = "count",
              description = "Number of intents to push",
              required = true, multiValued = false)
    String countString = null;

    private IntentService service;
    private HostService hostService;
    private int count;

    @Override
    protected void execute() {
        service = get(IntentService.class);
        hostService = get(HostService.class);

        count = Integer.parseInt(countString);

        if (count > 0) {
            Collection<Intent> intents = generateIntents();
            submitIntents(intents);
        } else {
            withdrawIntents();
        }
    }

    private Collection<Intent> generateIntents() {
        TrafficSelector selector = DefaultTrafficSelector.builder().build();
        TrafficTreatment treatment = DefaultTrafficTreatment.builder().build();

        List<Host> hosts = Lists.newArrayList(hostService.getHosts());
        List<Intent> fullMesh = Lists.newArrayList();
        for (int i = 0; i < hosts.size(); i++) {
            for (int j = i + 1; j < hosts.size(); j++) {
                fullMesh.add(new HostToHostIntent(appId(),
                                                  hosts.get(i).id(),
                                                  hosts.get(j).id(),
                                                  selector, treatment));
            }
        }
        Collections.shuffle(fullMesh);
        return fullMesh.subList(0, Math.min(count, fullMesh.size()));
    }

    private void submitIntents(Collection<Intent> intents) {
        for (Intent intent : intents) {
            service.submit(intent);
        }
        print("Submitted %d host to host intents.", intents.size());
    }

    private void withdrawIntents() {
        for (Intent intent : service.getIntents()) {
            if (appId().equals(intent.appId())) {
                service.withdraw(intent);
            }
        }
        print("Withdrew all randomly generated host to host intents.");
    }

    @Override
    protected ApplicationId appId() {
        return get(CoreService.class).registerApplication("org.onosproject.cli-random");
    }
}
