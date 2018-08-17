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
package org.onosproject.cli.net;

import com.google.common.collect.ListMultimap;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.api.action.Option;
import org.onlab.osgi.ServiceNotFoundException;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.net.flowobjective.FilteringObjQueueKey;
import org.onosproject.net.flowobjective.FlowObjectiveService;
import org.onosproject.net.flowobjective.ForwardingObjQueueKey;
import org.onosproject.net.flowobjective.NextObjQueueKey;
import org.onosproject.net.flowobjective.Objective;

import java.util.Map;

/**
 * Displays flow objective that are waiting for the completion of previous objective with the same key.
 */
@Service
@Command(scope = "onos", name = "obj-queues",
        description = "Display flow objective queues")
public class FlowObjectiveQueueListCommand extends AbstractShellCommand {

    @Option(name = "-s", aliases = "--size",
            description = "Print queue size only",
            required = false, multiValued = false)
    private boolean sizeOnly = false;

    @Option(name = "-c", aliases = "--cache",
            description = "Print cache",
            required = false, multiValued = false)
    private boolean cache = false;

    @Override
    protected void doExecute() {
        try {
            FlowObjectiveService service = get(FlowObjectiveService.class);
            ListMultimap<FilteringObjQueueKey, Objective> filtObjQueue = service.getFilteringObjQueue();
            ListMultimap<ForwardingObjQueueKey, Objective> fwdObjQueue = service.getForwardingObjQueue();
            ListMultimap<NextObjQueueKey, Objective> nextObjQueue = service.getNextObjQueue();
            Map<FilteringObjQueueKey, Objective> filtObjQueueHead = service.getFilteringObjQueueHead();
            Map<ForwardingObjQueueKey, Objective> fwdObjQueueHead = service.getForwardingObjQueueHead();
            Map<NextObjQueueKey, Objective> nextObjQueueHead = service.getNextObjQueueHead();

            if (cache) {
                printMap("Filtering objective cache", filtObjQueueHead, sizeOnly);
                printMap("Forwarding objective cache", fwdObjQueueHead, sizeOnly);
                printMap("Next objective cache", nextObjQueueHead, sizeOnly);
            } else {
                printMap("Filtering objective queue", filtObjQueue.asMap(), sizeOnly);
                printMap("Forwarding objective queue", fwdObjQueue.asMap(), sizeOnly);
                printMap("Next objective queue", nextObjQueue.asMap(), sizeOnly);
            }
        } catch (ServiceNotFoundException e) {
            print("FlowObjectiveService unavailable");
        }
    }

    @SuppressWarnings("unchecked")
    private void printMap(String mapName, Map map, boolean sizeOnly) {
        print("%s size = %d", mapName, map.size());
        if (!sizeOnly) {
            map.forEach((k, v) -> print("%s -> %s", k, v));
        }
    }
}
