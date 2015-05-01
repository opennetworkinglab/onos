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
import org.onosproject.ui.RequestHandler;

import java.util.Arrays;

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
        RowComparator rc = TableUtils.createRowComparator(payload, defaultColId());
        TableRow[] rows = generateTableRows(payload);
        Arrays.sort(rows, rc);
        ObjectNode rootNode = MAPPER.createObjectNode();
        rootNode.set(nodeName, TableUtils.generateArrayNode(rows));
        sendMessage(respType, 0, rootNode);
    }

    /**
     * Returns the default column ID, when one is not supplied in the payload
     * defining the column on which to sort. This implementation returns "id".
     *
     * @return default sort column id
     */
    protected String defaultColId() {
        return "id";
    }

    /**
     * Subclasses should generate table rows for their specific table instance.
     *
     * @param payload provided in case custom parameters are present
     * @return generated table rows
     */
    protected abstract TableRow[] generateTableRows(ObjectNode payload);
}
