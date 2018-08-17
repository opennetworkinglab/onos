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

import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.net.intent.IntentExtensionService;

import java.util.OptionalInt;

/**
 * Lists the inventory of intents and their states.
 */
@Service
@Command(scope = "onos", name = "intent-compilers",
        description = "Lists the mapping from intent type to compiler component")
public class IntentListCompilers extends AbstractShellCommand {

    @Override
    protected void doExecute() {
        IntentExtensionService service = get(IntentExtensionService.class);
        OptionalInt length = service.getCompilers().keySet().stream()
                .mapToInt(s -> s.getName().length())
                .max();
        if (length.isPresent()) {
            service.getCompilers().entrySet().forEach(e -> {
                print("%-" + length.getAsInt() + "s\t%s",
                      e.getKey().getName(),
                      e.getValue().getClass().getName());
            });
        } else {
            print("There are no compilers registered.");
        }

    }
}
