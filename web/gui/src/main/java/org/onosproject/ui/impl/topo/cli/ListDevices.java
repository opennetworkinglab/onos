/*
 * Copyright 2016-present Open Networking Laboratory
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

package org.onosproject.ui.impl.topo.cli;

import org.apache.karaf.shell.commands.Command;
import org.onosproject.ui.impl.topo.model.UiSharedTopologyModel;

/**
 * CLI command to list the UiDevices stored in the ModelCache.
 */
@Command(scope = "onos", name = "ui-cache-devices",
        description = "Lists UiDevices in the Model Cache")
public class ListDevices extends AbstractElementCommand {

    @Override
    protected void execute() {
        UiSharedTopologyModel model = get(UiSharedTopologyModel.class);
        sorted(model.getDevices()).forEach(d -> print("%s", d));
    }
}
