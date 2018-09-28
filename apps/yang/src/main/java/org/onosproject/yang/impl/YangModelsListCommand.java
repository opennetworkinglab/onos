/*
 * Copyright 2017-present Open Networking Foundation
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
package org.onosproject.yang.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.yang.model.YangModel;
import org.onosproject.yang.model.YangModule;
import org.onosproject.yang.runtime.YangModelRegistry;

/**
 * Lists registered YANG models.
 */
@Service
@Command(scope = "onos", name = "models",
        description = "Lists registered YANG models")
public class YangModelsListCommand extends AbstractShellCommand {


    private static final String FORMAT = "modelId=%s moduleName=%s moduleRevision=%s";
    private static final String MODULE_NAME = "moduleName";
    private static final String MODULE_REVISION = "moduleRevision";

    @Override
    protected void doExecute() {
        YangModelRegistry service = get(YangModelRegistry.class);

        if (outputJson()) {
            print("%s", json(service));
        } else {
            for (YangModel model : service.getModels()) {
                for (YangModule module : model.getYangModules()) {
                    print(FORMAT, model.getYangModelId(),
                          module.getYangModuleId().moduleName(),
                          module.getYangModuleId().revision());
                }
            }
        }
    }

    // Produces JSON structure.
    private JsonNode json(YangModelRegistry service) {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode result = mapper.createObjectNode();
        for (YangModel model : service.getModels()) {
            ArrayNode modelNode = mapper.createArrayNode();
            result.set(model.getYangModelId(), modelNode);
            for (YangModule module : model.getYangModules()) {
                ObjectNode moduleNode = mapper.createObjectNode();
                modelNode.add(moduleNode);
                moduleNode.put(MODULE_NAME, module.getYangModuleId().moduleName());
                moduleNode.put(MODULE_REVISION, module.getYangModuleId().revision());
            }
        }
        return result;
    }

}
