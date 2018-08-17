/*
 * Copyright 2016-present Open Networking Foundation
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

import java.util.Map;

import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.store.service.StorageAdminService;
import org.onosproject.store.service.WorkQueueStats;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Command to list stats for all work queues in the system.
 */
@Service
@Command(scope = "onos", name = "queues",
        description = "Lists information about work queues in the system")
public class QueuesListCommand extends AbstractShellCommand {

    private static final String FMT = "name=%s pending=%d inProgress=%d, completed=%d";

    @Override
    protected void doExecute() {
        StorageAdminService storageAdminService = get(StorageAdminService.class);
        Map<String, WorkQueueStats> queueStats = storageAdminService.getQueueStats();
        if (outputJson()) {
            ObjectMapper mapper = new ObjectMapper();
            ObjectNode jsonQueues = mapper.createObjectNode();
            queueStats.forEach((k, v) -> {
                ObjectNode jsonStats = jsonQueues.putObject(k);
                jsonStats.put("pending", v.totalPending());
                jsonStats.put("inProgress", v.totalInProgress());
                jsonStats.put("completed", v.totalCompleted());
            });
            print("%s", jsonQueues);
        } else {
            queueStats.forEach((name, stats) ->
            print(FMT, name, stats.totalPending(), stats.totalInProgress(), stats.totalCompleted()));
        }
    }
}
