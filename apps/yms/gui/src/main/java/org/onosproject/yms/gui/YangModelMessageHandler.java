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

package org.onosproject.yms.gui;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableSet;
import org.onlab.osgi.ServiceDirectory;
import org.onosproject.ui.RequestHandler;
import org.onosproject.ui.UiConnection;
import org.onosproject.ui.UiMessageHandler;
import org.onosproject.ui.table.TableModel;
import org.onosproject.ui.table.TableRequestHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;

/**
 * ONOS UI Yang Models message handler.
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
    private static final String TYPE = "type";
    // TODO: fill out table columns as needed

    private static final String[] COL_IDS = {
            ID, TYPE
    };

    private final Logger log = LoggerFactory.getLogger(getClass());

//    private YmsService ymsService;
    // TODO: fill out other fields as necessary


    // ===============-=-=-=-=-=-==================-=-=-=-=-=-=-===========

    @Override
    public void init(UiConnection connection, ServiceDirectory directory) {
        super.init(connection, directory);
//        ymsService = directory.get(YmsService.class);
        // TODO: addListeners(); ???
    }

    @Override
    public void destroy() {
        // TODO: removeListeners(); ???
        super.destroy();
        // NOTE: if no listeners are required, this method can be removed
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
            // TODO: use ymsService(?) to iterate over list of models...
            for (int k = 0; k < 5; k++) {
                populateRow(tm.addRow(), k);
            }
        }

        // TODO: obviously, this should be adapted to arrange YANG model data
        //       into the appropriate table columns
        private void populateRow(TableModel.Row row, int k) {
            row.cell(ID, k)
                    .cell(TYPE, "ymtype-" + k);
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

            // TODO: retrieve the appropriate model from ymsService and create
            //       a detail record to send back to the client.

            ObjectNode data = objectNode();

            data.put(ID, id);
            data.put(TYPE, "some-type");
            data.put("todo", "fill out with appropriate date attributes");

            ObjectNode rootNode = objectNode();
            rootNode.set(DETAILS, data);

            sendMessage(DETAILS_RESP, rootNode);
        }
    }
}
