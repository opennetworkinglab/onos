/*
 * Copyright 2021-present Open Networking Foundation
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
package org.onosproject.kubevirtnetworking.cli;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.kubevirtnetworking.api.KubevirtSecurityGroup;
import org.onosproject.kubevirtnetworking.api.KubevirtSecurityGroupRule;
import org.onosproject.kubevirtnetworking.api.KubevirtSecurityGroupService;

import java.util.List;

import static org.onosproject.kubevirtnetworking.util.KubevirtNetworkingUtil.prettyJson;

/**
 * Show a detailed security group info.
 */
@Service
@Command(scope = "onos", name = "kubevirt-security-group",
        description = "Displays a security group details")
public class KubevirtShowSecurityGroupCommand extends AbstractShellCommand {

    @Option(name = "--name",
            description = "Filter security group by specific name", multiValued = true)
    @Completion(KubevirtSecurityGroupCompleter.class)
    private List<String> names;

    @Override
    protected void doExecute() throws Exception {
        KubevirtSecurityGroupService service = get(KubevirtSecurityGroupService.class);

        if (names == null || names.size() == 0) {
            print("Need to specify at least one security group name using --name option.");
            return;
        }

        for (String name : names) {
            KubevirtSecurityGroup sg = service.securityGroups().stream()
                    .filter(s -> s.name().equals(name))
                    .findAny().orElse(null);
            if (sg == null) {
                print("Unable to find %s", name);
                continue;
            }

            if (outputJson()) {
                print("%s", json(sg));
            } else {
                printSecurityGroup(sg);
            }
        }
    }

    private void printSecurityGroup(KubevirtSecurityGroup sg) {
        print("Name: %s", sg.name());
        print("  ID: %s", sg.id());
        print("  Description: %s", sg.description());

        int counter = 1;
        for (KubevirtSecurityGroupRule rule : sg.rules()) {
            print("  Rule #%d:", counter);
            print("    ID: %s", rule.id());
            print("    Direction: %s", rule.direction());
            print("    EtherType: %s", rule.etherType());
            print("    Protocol: %s", rule.protocol());
            print("    PortRangeMax: %s", rule.portRangeMax());
            print("    PortRangeMin: %s", rule.portRangeMin());
            print("    RemoteIpPrefix: %s", rule.remoteIpPrefix());
            print("    RemoteGroupID: %s", rule.remoteGroupId());
            counter++;
        }
    }

    private String json(KubevirtSecurityGroup sg) {
        return prettyJson(new ObjectMapper(),
                jsonForEntity(sg, KubevirtSecurityGroup.class).toString());
    }
}
