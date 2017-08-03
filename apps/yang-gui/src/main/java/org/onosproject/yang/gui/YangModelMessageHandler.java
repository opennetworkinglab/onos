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
import org.onosproject.yang.YangClassLoaderRegistry;
import org.onosproject.yang.model.DefaultYangModuleId;
import org.onosproject.yang.model.ModelException;
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
    private YangClassLoaderRegistry classLoaderRegistry;


    // ===============-=-=-=-=-=-==================-=-=-=-=-=-=-===========

    @Override
    public void init(UiConnection connection, ServiceDirectory directory) {
        super.init(connection, directory);
        modelRegistry = directory.get(YangModelRegistry.class);
        classLoaderRegistry = directory.get(YangClassLoaderRegistry.class);
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
                    populateRow(tm.addRow(), model.getYangModelId(), id);
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
            String modelId = string(payload, MODEL_ID);
            String moduleName = string(payload, ID);
            String revision = string(payload, REVISION);
            YangModule module = getModule(modelId, new DefaultYangModuleId(moduleName, revision));

            ObjectNode data = objectNode();
            data.put(MODEL_ID, modelId);
            data.put(ID, moduleName);
            data.put(REVISION, revision);

            if (module != null) {
                ArrayNode source = arrayNode();
                data.set(SOURCE, source);
                addSource(source, getSource(modelId, module));
            }

            ObjectNode rootNode = objectNode();
            rootNode.set(DETAILS, data);
            sendMessage(DETAILS_RESP, rootNode);
        }

        // FIXME: Hack to properly resolve the YANG source resource
        private InputStream getSource(String modelId, YangModule module) {
            try {
                return module.getYangSource(); // trigger exception
            } catch (ModelException e) {
                // Strip the YANG source file base-name and then use it to access
                // the corresponding resource in the correct run-time context.
                String msg = e.getMessage();
                int i = msg.lastIndexOf('/');
                String baseName = i > 0 ? msg.substring(i) : msg;
                ClassLoader loader = classLoaderRegistry.getClassLoader(modelId);
                return loader == null ? null :
                        loader.getResourceAsStream("/yang/resources" + baseName);
            }
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

    private YangModule getModule(String modelId, DefaultYangModuleId moduleId) {
        YangModel model = modelRegistry.getModel(modelId);
        return model != null ? model.getYangModule(moduleId) : null;
    }

}
