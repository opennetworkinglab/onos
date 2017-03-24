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
 */

package org.onosproject.yang.gui;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableSet;
import org.onlab.osgi.ServiceDirectory;
import org.onosproject.ui.RequestHandler;
import org.onosproject.ui.UiConnection;
import org.onosproject.ui.UiMessageHandler;
import org.onosproject.ui.table.TableModel;
import org.onosproject.ui.table.TableRequestHandler;
import org.onosproject.yang.model.YangModel;
import org.onosproject.yang.model.YangModuleId;
import org.onosproject.yang.runtime.YangModelRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Set;

/**
 * ONOS UI YANG Models message handler.
 */
public class YangModelMessageHandler extends UiMessageHandler {

    private static final String TABLE_REQ = "yangModelDataRequest";
    private static final String TABLE_RESP = "yangModelDataResponse";
    private static final String MODELS = "yangModels";

    private static final String DETAILS_REQ = "yangModelDetailsRequest";
    private static final String DETAILS_RESP = "yangModelDetailsResponse";
    private static final String DETAILS = "details";

    // Table Column IDs
    private static final String ID = "id";
    private static final String MODULES = "modules";
    // TODO: fill out table columns as needed

    private static final String[] COL_IDS = {
            ID, MODULES
    };

    private final Logger log = LoggerFactory.getLogger(getClass());

    private YangModelRegistry modelRegistry;
    // TODO: fill out other fields as necessary


    // ===============-=-=-=-=-=-==================-=-=-=-=-=-=-===========

    @Override
    public void init(UiConnection connection, ServiceDirectory directory) {
        super.init(connection, directory);
        modelRegistry = directory.get(YangModelRegistry.class);
    }

    @Override
    public void destroy() {
        super.destroy();
    }

    @Override
    protected Collection<RequestHandler> createRequestHandlers() {
        return ImmutableSet.of(
                new TableDataHandler(),
                new DetailRequestHandler()
        );
    }

    // Handler for table requests
    private final class TableDataHandler extends TableRequestHandler {
        private static final String NO_ROWS_MESSAGE = "No YANG Models found";

        private TableDataHandler() {
            super(TABLE_REQ, TABLE_RESP, MODELS);
        }

        @Override
        protected String[] getColumnIds() {
            return COL_IDS;
        }

        @Override
        protected String noRowsMessage(ObjectNode payload) {
            return NO_ROWS_MESSAGE;
        }

        @Override
        protected void populateTable(TableModel tm, ObjectNode payload) {
            for (YangModel model : modelRegistry.getModels()) {
                populateRow(tm.addRow(), model.getYangModulesId());
            }
        }

        private void populateRow(TableModel.Row row, Set<YangModuleId> moduleIds) {
            StringBuilder sb = new StringBuilder();
            moduleIds.forEach(i -> sb.append(", ").append(i.moduleName())
                    .append("(").append(i.revision()).append(")"));
            row.cell(ID, moduleIds.hashCode()).cell(MODULES, sb.toString().substring(2));
        }
    }


    // handler for selected model detail requests (selected table row)
    private final class DetailRequestHandler extends RequestHandler {
        private DetailRequestHandler() {
            super(DETAILS_REQ);
        }

        @Override
        public void process(ObjectNode payload) {
            String id = string(payload, ID);
            YangModel model = getModel(id);

            ObjectNode data = objectNode();
            data.put(ID, id);

            if (model != null) {
                ArrayNode modules = arrayNode();
                model.getYangModulesId().forEach(mid -> {
                    ObjectNode module = objectNode();
                    module.put("name", mid.moduleName());
                    module.put("revision", mid.revision());
                    modules.add(module);
                });
                data.set(MODULES, modules);
            }

            ObjectNode rootNode = objectNode();
            rootNode.set(DETAILS, data);
            sendMessage(DETAILS_RESP, rootNode);
        }
    }

    private YangModel getModel(String id) {
        int nid = Integer.parseInt(id);
        return modelRegistry.getModels().stream()
                .filter(m -> m.getYangModulesId().hashCode() == nid)
                .findFirst().orElse(null);
    }
}
