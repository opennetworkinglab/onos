/*
 * Copyright 2015-present Open Networking Foundation
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
package org.onosproject.ui.impl.cli;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.ui.UiPreferencesService;

/**
 * Lists all UI user preferences.
 */
@Service
@Command(scope = "onos", name = "ui-prefs",
        description = "Lists all UI user preferences")
public class UiPreferencesListCommand extends AbstractShellCommand {

    @Override
    protected void doExecute() {
        UiPreferencesService service = get(UiPreferencesService.class);
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode root = mapper.createObjectNode();

        service.getUserNames().forEach(user -> {
            ObjectNode prefs = mapper.createObjectNode();
            root.set(user, prefs);
            service.getPreferences(user).forEach(prefs::set);
        });

        print("%s", root);
    }
}
