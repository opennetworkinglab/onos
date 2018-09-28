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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.ui.UiExtension;
import org.onosproject.ui.UiExtensionService;

import java.util.List;

/**
 * Lists all registered UI views.
 */
@Service
@Command(scope = "onos", name = "ui-views",
        description = "Lists all registered UI views")
public class UiViewListCommand extends AbstractShellCommand {

    private static final String FMT = "id=%s, category=%s, label=%s, icon=%s";

    @Override
    protected void doExecute() {
        UiExtensionService service = get(UiExtensionService.class);
        if (outputJson()) {
            print("%s", json(service.getExtensions()));
        } else {
            service.getExtensions().forEach(ext -> ext.views()
                    .forEach(v -> print(FMT, v.id(), v.category(),
                                        v.label(), v.iconId())));
        }
    }

    private JsonNode json(List<UiExtension> extensions) {
        ObjectMapper mapper = new ObjectMapper();
        ArrayNode node = mapper.createArrayNode();
        extensions.forEach(ext -> ext.views()
                .forEach(v -> node.add(mapper.createObjectNode()
                                               .put("id", v.id())
                                               .put("category", v.category().toString())
                                               .put("label", v.label())
                                               .put("icon", v.iconId()))));
        return node;
    }

}
