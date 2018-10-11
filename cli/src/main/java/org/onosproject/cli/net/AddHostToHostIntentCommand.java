/*
 * Copyright 2014-present Open Networking Foundation
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

import java.util.List;

import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.onosproject.net.HostId;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.intent.Constraint;
import org.onosproject.net.intent.HostToHostIntent;
import org.onosproject.net.intent.IntentService;

/**
 * Installs host-to-host connectivity intent.
 */
@Service
@Command(scope = "onos", name = "add-host-intent",
         description = "Installs host-to-host connectivity intent")
public class AddHostToHostIntentCommand extends ConnectivityIntentCommand {

    @Argument(index = 0, name = "one", description = "One host ID",
              required = true, multiValued = false)
    @Completion(HostIdCompleter.class)
    String one = null;

    @Argument(index = 1, name = "two", description = "Another host ID",
              required = true, multiValued = false)
    @Completion(HostIdCompleter.class)
    String two = null;

    @Override
    protected void doExecute() {
        IntentService service = get(IntentService.class);

        HostId oneId = HostId.hostId(one);
        HostId twoId = HostId.hostId(two);

        TrafficSelector selector = buildTrafficSelector();
        TrafficTreatment treatment = buildTrafficTreatment();
        List<Constraint> constraints = buildConstraints();

        HostToHostIntent intent = HostToHostIntent.builder()
                .appId(appId())
                .key(key())
                .one(oneId)
                .two(twoId)
                .selector(selector)
                .treatment(treatment)
                .constraints(constraints)
                .priority(priority())
                .resourceGroup(resourceGroup())
                .build();
        service.submit(intent);
        print("Host to Host intent submitted:\n%s", intent.toString());
    }

}
