/*
 * Copyright 2017-present Open Networking Laboratory
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
 *
 */

package org.onosproject.ui.impl.topo.cli;

import org.apache.karaf.shell.commands.Command;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.ui.UiExtensionService;
import org.onosproject.ui.UiTopoMapFactory;

/**
 * CLI command to list the registered geographic maps, that may be used
 * by the topology view.
 */
@Command(scope = "onos", name = "ui-geo-map-list",
        description = "Lists available geographic maps for topology view")
public class UiGeoMapListCommand extends AbstractShellCommand {

    @Override
    protected void execute() {
        UiExtensionService uxs = get(UiExtensionService.class);
        uxs.getExtensions().forEach(ext -> {
            UiTopoMapFactory mapFactory = ext.topoMapFactory();
            if (mapFactory != null) {
                mapFactory.geoMaps().forEach(m -> print("%s", m));
            }
        });
    }
}
