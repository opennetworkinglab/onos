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

package org.onosproject.ui.impl;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableSet;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.Link;
import org.onosproject.net.link.LinkService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Message handler for link view related messages.
 */
public class LinkViewMessageHandler extends AbstractTabularViewMessageHandler {

    /**
     * Creates a new message handler for the link messages.
     */
    protected LinkViewMessageHandler() {
        super(ImmutableSet.of("linkDataRequest"));
    }

    @Override
    public void process(ObjectNode message) {
        ObjectNode payload = payload(message);
        String sortCol = string(payload, "sortCol", "src");
        String sortDir = string(payload, "sortDir", "asc");

        LinkService service = get(LinkService.class);
        TableRow[] rows = generateTableRows(service);
        RowComparator rc =
                new RowComparator(sortCol, RowComparator.direction(sortDir));
        Arrays.sort(rows, rc);
        ArrayNode links = generateArrayNode(rows);
        ObjectNode rootNode = mapper.createObjectNode();
        rootNode.set("links", links);

        connection().sendMessage("linkDataResponse", 0, rootNode);
    }

    private TableRow[] generateTableRows(LinkService service) {
        List<TableRow> list = new ArrayList<>();
        for (Link link : service.getLinks()) {
            list.add(new LinkTableRow(link));
        }
        return list.toArray(new TableRow[list.size()]);
    }

    /**
     * TableRow implementation for {@link Link links}.
     */
    private static class LinkTableRow extends AbstractTableRow {

        private static final String SOURCE = "src";
        private static final String DEST = "dst";
        private static final String TYPE = "type";
        private static final String STATE = "state";
        private static final String DURABLE = "durable";

        private static final String[] COL_IDS = {
                SOURCE, DEST, TYPE, STATE, DURABLE
        };

        public LinkTableRow(Link l) {
            ConnectPoint src = l.src();
            ConnectPoint dst = l.dst();

            add(SOURCE, src.elementId().toString() + "/" + src.port().toString());
            add(DEST, dst.elementId().toString() + "/" + dst.port().toString());
            add(TYPE, l.type().toString());
            add(STATE, l.state().toString());
            add(DURABLE, Boolean.toString(l.isDurable()));
        }

        @Override
        protected String[] columnIds() {
            return COL_IDS;
        }
    }

}
