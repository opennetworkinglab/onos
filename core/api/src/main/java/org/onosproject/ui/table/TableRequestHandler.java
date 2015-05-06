/*
 * Copyright 2015 Open Networking Laboratory
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

package org.onosproject.ui.table;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onosproject.ui.JsonUtils;
import org.onosproject.ui.RequestHandler;

/**
 * Message handler specifically for table views.
 */
public abstract class TableRequestHandler extends RequestHandler {

    private final String respType;
    private final String nodeName;

    /**
     * Constructs a table request handler for a specific table view. When
     * table requests come in, the handler will generate the appropriate
     * table rows, sort them according the the request sort parameters, and
     * send back the response to the client.
     *
     * @param reqType   type of the request event
     * @param respType  type of the response event
     * @param nodeName  name of JSON node holding row data
     */
    public TableRequestHandler(String reqType, String respType, String nodeName) {
        super(reqType);
        this.respType = respType;
        this.nodeName = nodeName;
    }

    @Override
    public void process(long sid, ObjectNode payload) {
        TableModel tm = createTableModel();
        populateTable(tm, payload);

        String sortCol = JsonUtils.string(payload, "sortCol", defaultColumnId());
        String sortDir = JsonUtils.string(payload, "sortDir", "asc");
        tm.sort(sortCol, TableModel.sortDir(sortDir));

        ObjectNode rootNode = MAPPER.createObjectNode();
        rootNode.set(nodeName, TableUtils.generateArrayNode(tm));
        sendMessage(respType, 0, rootNode);
    }

    /**
     * Creates the table model (devoid of data) using {@link #getColumnIds()}
     * to initialize it, ready to be populated.
     * <p>
     * This default implementation returns a table model with default
     * formatters and comparators for all columns.
     *
     * @return an empty table model
     */
    protected TableModel createTableModel() {
        return new TableModel(getColumnIds());
    }

    /**
     * Returns the default column ID to be used when one is not supplied in
     * the payload as the column on which to sort.
     * <p>
     * This default implementation returns "id".
     *
     * @return default sort column identifier
     */
    protected String defaultColumnId() {
        return "id";
    }

    /**
     * Subclasses should return the array of column IDs with which
     * to initialize their table model.
     *
     * @return the column IDs
     */
    protected abstract String[] getColumnIds();

    /**
     * Subclasses should populate the table model by adding
     * {@link TableModel.Row rows}.
     * <pre>
     *     tm.addRow()
     *         .cell(COL_ONE, ...)
     *         .cell(COL_TWO, ...)
     *         ... ;
     * </pre>
     * The request payload is provided in case there are request filtering
     * parameters (other than sort column and sort direction) that are required
     * to generate the appropriate data.
     *
     * @param tm the table model
     * @param payload request payload
     */
    protected abstract void populateTable(TableModel tm, ObjectNode payload);
}
