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
import org.onosproject.yang.model.YangModule;
import org.onosproject.yang.model.YangModuleId;
import org.onosproject.yang.runtime.YangModelRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collection;

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
    private static final String REVISION = "revision";
    private static final String MODEL_ID = "modelId";

    private static final String SOURCE = "source";

    private static final String[] COL_IDS = {
            ID, REVISION, MODEL_ID
    };

    private final Logger log = LoggerFactory.getLogger(getClass());

    private YangModelRegistry modelRegistry;


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
                for (YangModuleId id : model.getYangModulesId()) {
                    populateRow(tm.addRow(), modelId(model), id);
                }
            }
        }

        private void populateRow(TableModel.Row row, String modelId,
                                 YangModuleId moduleId) {
            row.cell(ID, moduleId.moduleName())
                    .cell(REVISION, moduleId.revision())
                    .cell(MODEL_ID, modelId);
        }
    }


    // handler for selected model detail requests (selected table row)
    private final class DetailRequestHandler extends RequestHandler {
        private DetailRequestHandler() {
            super(DETAILS_REQ);
        }

        @Override
        public void process(ObjectNode payload) {
            String name = string(payload, ID);
            String modelId = string(payload, MODEL_ID);
            YangModule module = getModule(modelId, name);

            ObjectNode data = objectNode();
            data.put(ID, name);
            if (module != null) {
                data.put(REVISION, module.getYangModuleId().revision());
                data.put(MODEL_ID, modelId);

                ArrayNode source = arrayNode();
                data.set(SOURCE, source);

                addSource(source, module.getYangSource());
            }

            ObjectNode rootNode = objectNode();
            rootNode.set(DETAILS, data);
            sendMessage(DETAILS_RESP, rootNode);
        }

        private void addSource(ArrayNode source, InputStream yangSource) {
            try (InputStreamReader isr = new InputStreamReader(yangSource);
                 BufferedReader br = new BufferedReader(isr)) {
                String line;
                while ((line = br.readLine()) != null) {
                    source.add(line);
                }

            } catch (IOException e) {
                log.warn("Unable to read YANG source", e);
            }
        }
    }


    private YangModule getModule(String modelId, String name) {
        int nid = Integer.parseInt(modelId.substring(2));
        log.info("Got {}; {}", modelId, nid);
        YangModel model = modelRegistry.getModels().stream()
                .filter(m -> modelId(m).equals(modelId))
                .findFirst().orElse(null);
        if (model != null) {
            log.info("Got model");
            return model.getYangModules().stream()
                    .filter(m -> m.getYangModuleId().moduleName().contentEquals(name))
                    .findFirst().orElse(null);
        }
        return null;
    }

    private String modelId(YangModel m) {
        return "YM" + Math.abs(m.hashCode());
    }
}
