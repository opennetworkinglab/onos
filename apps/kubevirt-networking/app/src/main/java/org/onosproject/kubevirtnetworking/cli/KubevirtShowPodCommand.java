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
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.client.utils.Serialization;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.kubevirtnetworking.api.KubevirtPodService;

import java.io.IOException;
import java.util.List;

import static org.onosproject.kubevirtnetworking.util.KubevirtNetworkingUtil.prettyJson;

/**
 * Show a detailed POD info.
 */
@Service
@Command(scope = "onos", name = "kubevirt-pod",
        description = "Displays a POD details")
public class KubevirtShowPodCommand extends AbstractShellCommand {

    @Option(name = "--name",
            description = "Filter POD by specific name", multiValued = true)
    @Completion(KubevirtPodCompleter.class)
    private List<String> names;

    @Override
    protected void doExecute() throws Exception {
        KubevirtPodService service = get(KubevirtPodService.class);

        if (names == null || names.size() == 0) {
            print("Need to specify at least one POD name using --name option.");
            return;
        }

        for (String name : names) {
            Pod pod = service.pods().stream().filter(p -> p.getMetadata().getName().equals(name))
                    .findAny().orElse(null);
            if (pod == null) {
                print("Unable to find %s", name);
                continue;
            }

            if (outputJson()) {
                print("%s", json(pod));
            } else {
                printPod(pod);
            }
        }
    }

    private void printPod(Pod pod) {
        print("Name: %s", pod.getMetadata().getName());
        print("  Status: %s", pod.getStatus().getPhase());
        print("  Node Name: %s", pod.getSpec().getNodeName());
        print("  Namespace: %s", pod.getMetadata().getNamespace());
        print("  IP address: %s", pod.getStatus().getPodIP());

        int counter = 1;
        for (Container container : pod.getSpec().getContainers()) {
            print("  Container #%d:", counter);
            print("    Name: %s", container.getName());
            print("    Image: %s", container.getImage());
            print("    Pull Policy: %s", container.getImagePullPolicy());
            print("    Commands: %s", container.getCommand());
            print("    Args: %s", container.getArgs());
            counter++;
        }
    }

    private String json(Pod pod) {
        try {
            ObjectNode result = (ObjectNode)
                    new ObjectMapper().readTree(Serialization.asJson(pod));
            return prettyJson(new ObjectMapper(), result.toString());
        } catch (IOException e) {
            log.warn("Failed to parse POD's JSON string.");
            return "";
        }
    }
}
