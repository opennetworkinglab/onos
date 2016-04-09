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

import java.util.Map;

import org.apache.karaf.shell.commands.Command;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.store.service.StorageAdminService;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Command to list the various counters in the system.
 */
@Command(scope = "onos", name = "counters",
        description = "Lists information about atomic counters in the system")
public class CountersListCommand extends AbstractShellCommand {

    private static final String FMT = "name=%s value=%d";

    @Override
    protected void execute() {
        StorageAdminService storageAdminService = get(StorageAdminService.class);
        Map<String, Long> counters = storageAdminService.getCounters();
        if (outputJson()) {
            ObjectMapper mapper = new ObjectMapper();
            ObjectNode jsonCounters = mapper.createObjectNode();
            counters.forEach((k, v) -> jsonCounters.put(k, v));
            print("%s", jsonCounters);
        } else {
            counters.keySet().stream().sorted().forEach(name -> print(FMT, name, counters.get(name)));
        }
    }
}
