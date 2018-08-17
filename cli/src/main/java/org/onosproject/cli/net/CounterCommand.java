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

import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.store.service.AtomicCounter;
import org.onosproject.store.service.StorageService;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Command to display the current value of a atomic counter.
 */
@Service
@Command(scope = "onos", name = "counter",
        description = "Displays the current value of a atomic counter")
public class CounterCommand extends AbstractShellCommand {

    @Argument(index = 0, name = "counterName", description = "Counter Name",
            required = true, multiValued = false)
    String name = null;

    @Override
    protected void doExecute() {
        StorageService storageService = get(StorageService.class);
        AtomicCounter counter = storageService.getAtomicCounter(name);

        if (outputJson()) {
            ObjectMapper mapper = new ObjectMapper();
            ObjectNode counterJsonNode = mapper.createObjectNode();
            counterJsonNode.put("value", counter.get());
            print("%s", counterJsonNode);
        } else {
            print("%d", counter.get());
        }
    }
}
