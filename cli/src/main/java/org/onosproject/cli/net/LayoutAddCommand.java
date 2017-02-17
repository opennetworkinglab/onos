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
 * Add a new UI layout.
 */
@Command(scope = "onos", name = "layout-add",
        description = "Adds a new UI layout.")
public class LayoutAddCommand extends AbstractShellCommand {

    private static final char CODE_GEO = '@';
    private static final char CODE_GRID = '+';
    private static final String ROOT = "root";

    @Argument(index = 0, name = "id", description = "Layout ID",
            required = true, multiValued = false)
    String id = null;

    @Argument(index = 1, name = "bgref", description = "Background Ref",
            required = true, multiValued = false)
    String backgroundRef = null;

    @Argument(index = 2, name = "rid", description = "Region ID (optional)",
            required = false, multiValued = false)
    String regionId = null;

    @Argument(index = 3, name = "plid", description = "Parent layout ID (optional)",
            required = false, multiValued = false)
    String parentId = null;

    private RegionService regionService;

    @Override
    protected void execute() {
        UiTopoLayoutService service = get(UiTopoLayoutService.class);
        RegionService regionService = get(RegionService.class);

        if (ROOT.equals(id)) {
            // set the background for the root layout
            setAppropriateBackground(service.getRootLayout(), backgroundRef);
            return;
        }

        Region region = regionId == null ? null : regionService.getRegion(regionId(regionId));
        UiTopoLayoutId pid = parentId == null ? UiTopoLayoutId.DEFAULT_ID : layoutId(parentId);

        UiTopoLayout layout = new UiTopoLayout(layoutId(id)).region(region).parent(pid);
        setAppropriateBackground(layout, backgroundRef);
        service.addLayout(layout);
    }

    private void setAppropriateBackground(UiTopoLayout layout, String bgRef) {
        /*
         * A note about the format of bgref.. it should be one of:
         *    "."               - signifies no background
         *    "@{map-id}"       - signifies geo background (map)
         *    "+{sprite-id}"    - signifies grid background (sprite)
         *
         *    For example, "!", "@bayareaGEO", "+segmentRouting"
         */
        char type = bgRef.charAt(0);

        if (type == CODE_GEO) {
            // GEO (map) reference
            layout.geomap(bgRef.substring(1));

        } else if (type == CODE_GRID) {
            // Grid (sprite) reference
            layout.sprites(bgRef.substring(1));
        }
    }
}
