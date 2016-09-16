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
package org.onosproject.cli.net;

import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.net.region.Region;
import org.onosproject.net.region.RegionService;
import org.onosproject.ui.UiTopoLayoutService;
import org.onosproject.ui.model.topo.UiTopoLayout;
import org.onosproject.ui.model.topo.UiTopoLayoutId;

import static org.onosproject.net.region.RegionId.regionId;
import static org.onosproject.ui.model.topo.UiTopoLayoutId.layoutId;

/**
 * Creates a new UI layout.
 */
@Command(scope = "onos", name = "layout-add",
        description = "Creates a new UI layout")
public class LayoutAddCommand extends AbstractShellCommand {

    private static final String FMT = "id=%s, name=%s, type=%s";
    private static final String FMT_MASTER = "  master=%s";

    @Argument(index = 0, name = "id", description = "Layout ID",
            required = true, multiValued = false)
    String id = null;

    @Argument(index = 1, name = "id", description = "Region ID (optional)",
            required = false, multiValued = false)
    String regionId = null;

    @Argument(index = 2, name = "id", description = "Parent layout ID (optional)",
            required = false, multiValued = false)
    String parentId = null;

    private RegionService regionService;

    @Override
    protected void execute() {
        UiTopoLayoutService service = get(UiTopoLayoutService.class);
        RegionService regionService = get(RegionService.class);

        Region region = regionId == null ? null : regionService.getRegion(regionId(regionId));
        UiTopoLayoutId pid = parentId == null ? UiTopoLayoutId.DEFAULT_ID : layoutId(parentId);

        UiTopoLayout layout = new UiTopoLayout(layoutId(id), region, pid);
        service.addLayout(layout);
    }
}
