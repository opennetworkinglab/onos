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
 *
 * <pre>
 * layout-add {layout-id} {bg-ref} \
 *   [ {region-id} {parent-layout-id} {scale} {offset-x} {offset-y} ]
 * </pre>
 * Note that if you want to skip a parameter, but set later parameters,
 * use dot (".") as a placeholder for null. For example, no associated region
 * or parent layout, but setting the scale and offset for the root layout...
 * <pre>
 * layout-add root @bayareaGEO . . 1.2 0.0 -4.0
 * </pre>
 */
@Command(scope = "onos", name = "layout-add",
        description = "Adds a new UI layout.")
public class LayoutAddCommand extends AbstractShellCommand {

    private static final char CODE_GEO = '@';
    private static final char CODE_GRID = '+';

    private static final String NULL_TOKEN = ".";
    private static final String ROOT = "root";

    private static final double DEFAULT_SCALE = 1.0;
    private static final double DEFAULT_OFFSET = 0.0;


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

    @Argument(index = 4, name = "scale", description = "Zoom scale (optional; default 1.0)",
            required = false, multiValued = false)
    String zoomScale = null;

    @Argument(index = 5, name = "offx", description = "Zoom offset-X (optional; default 0.0)",
            required = false, multiValued = false)
    String zoomOffsetX = null;

    @Argument(index = 6, name = "offy", description = "Zoom offset-Y (optional; default 0.0)",
            required = false, multiValued = false)
    String zoomOffsetY = null;

    private RegionService regionService;

    @Override
    protected void execute() {
        UiTopoLayoutService service = get(UiTopoLayoutService.class);
        RegionService regionService = get(RegionService.class);

        UiTopoLayout layout;

        if (ROOT.equals(id)) {
            layout = service.getRootLayout();
            setAppropriateBackground(layout);
            setZoomParameters(layout);
            return;
        }

        // Otherwise, it is a user-defined layout...

        Region region = nullToken(regionId) ? null : regionService.getRegion(regionId(regionId));
        UiTopoLayoutId pid = nullToken(parentId) ? UiTopoLayoutId.DEFAULT_ID : layoutId(parentId);

        layout = new UiTopoLayout(layoutId(id)).region(region).parent(pid);

        setAppropriateBackground(layout);
        setZoomParameters(layout);
        service.addLayout(layout);
    }

    private boolean nullToken(String token) {
        return token == null || token.equals(NULL_TOKEN);
    }

    private void setAppropriateBackground(UiTopoLayout layout) {
        /*
         * A note about the format of bgref.. it should be one of:
         *    "."               - signifies no background
         *    "@{map-id}"       - signifies geo background (map)
         *    "+{sprite-id}"    - signifies grid background (sprite)
         *
         *    For example, ".", "@bayareaGEO", "+segmentRouting"
         */
        char type = backgroundRef.charAt(0);

        if (type == CODE_GEO) {
            // GEO (map) reference
            layout.geomap(backgroundRef.substring(1));

        } else if (type == CODE_GRID) {
            // Grid (sprite) reference
            layout.sprites(backgroundRef.substring(1));
        }
        // simply ignore null token (".")
    }

    private double parseDouble(String s, double def) {
        if (nullToken(s)) {
            return def;
        }

        double result;
        try {
            result = Double.parseDouble(s);
        } catch (NumberFormatException e) {
            result = def;
        }

        return result;
    }

    private void setZoomParameters(UiTopoLayout layout) {
        double scale = parseDouble(zoomScale, DEFAULT_SCALE);
        double offsetX = parseDouble(zoomOffsetX, DEFAULT_OFFSET);
        double offsetY = parseDouble(zoomOffsetY, DEFAULT_OFFSET);

        layout.scale(scale).offsetX(offsetX).offsetY(offsetY);
    }
}
