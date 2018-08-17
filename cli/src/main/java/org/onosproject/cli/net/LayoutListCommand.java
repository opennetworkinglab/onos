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
import org.onosproject.ui.UiTopoLayoutService;
import org.onosproject.ui.model.topo.UiTopoLayout;
import org.onosproject.ui.model.topo.UiTopoLayoutId;
import org.onosproject.utils.Comparators;

import java.util.Collections;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;

/**
 * List layout details.
 */
@Service
@Command(scope = "onos", name = "layouts",
        description = "List layout details")
public class LayoutListCommand extends AbstractShellCommand {

    private static final String FMT = "id=%s, bgref=%s, region=%s, parent=%s";

    @Argument(index = 0, name = "id", description = "Layout ID",
            required = false, multiValued = false)
    String id = null;

    private UiTopoLayoutService layoutService;

    @Override
    protected void doExecute() {
        layoutService = get(UiTopoLayoutService.class);
        if (id == null) {
            for (UiTopoLayout layout : getSortedLayouts(layoutService)) {
                printLayout(layout);
            }
        } else {
            UiTopoLayout layout = layoutService.getLayout(UiTopoLayoutId.layoutId(id));
            if (layout == null) {
                error("No such region %s", id);
            } else {
                printLayout(layout);
            }
        }
    }

    private List<UiTopoLayout> getSortedLayouts(UiTopoLayoutService service) {
        List<UiTopoLayout> layouts = newArrayList(service.getLayouts());
        Collections.sort(layouts, Comparators.LAYOUT_COMPARATOR);
        return layouts;
    }

    private void printLayout(UiTopoLayout layout) {
        String map = layout.geomap();
        String spr = layout.sprites();
        String bgRef = ".";
        if (map != null) {
            bgRef = "@" + map;
        } else if (spr != null) {
            bgRef = "+" + spr;
        }

        String pid = layout.parent() != null ? layout.parent().id() : "(none)";

        print(FMT, layout.id(), bgRef, layout.regionId(), pid);
    }
}
