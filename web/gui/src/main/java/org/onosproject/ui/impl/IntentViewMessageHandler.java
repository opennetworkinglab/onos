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
import org.onosproject.core.ApplicationId;
import org.onosproject.net.intent.Intent;
import org.onosproject.net.intent.IntentService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Message handler for intent view related messages.
 */
public class IntentViewMessageHandler extends AbstractTabularViewMessageHandler {

    /**
     * Creates a new message handler for the intent messages.
     */
    protected IntentViewMessageHandler() {
        super(ImmutableSet.of("intentDataRequest"));
    }

    @Override
    public void process(ObjectNode message) {
        ObjectNode payload = payload(message);
        String sortCol = string(payload, "sortCol", "appId");
        String sortDir = string(payload, "sortDir", "asc");

        IntentService service = get(IntentService.class);
        TableRow[] rows = generateTableRows(service);
        RowComparator rc =
                new RowComparator(sortCol, RowComparator.direction(sortDir));
        Arrays.sort(rows, rc);
        ArrayNode intents = generateArrayNode(rows);
        ObjectNode rootNode = mapper.createObjectNode();
        rootNode.set("intents", intents);

        connection().sendMessage("intentDataResponse", 0, rootNode);
    }

    private TableRow[] generateTableRows(IntentService service) {
        List<TableRow> list = new ArrayList<>();
        for (Intent intent : service.getIntents()) {
            list.add(new IntentTableRow(intent));
        }
        return list.toArray(new TableRow[list.size()]);
    }

    /**
     * TableRow implementation for {@link Intent intents}.
     */
    private static class IntentTableRow extends AbstractTableRow {

        private static final String APP_ID = "appId";
        private static final String KEY = "key";
        private static final String TYPE = "type";
        private static final String PRIORITY = "priority";

        private static final String[] COL_IDS = {
                APP_ID, KEY, TYPE, PRIORITY
        };

        public IntentTableRow(Intent i) {
            ApplicationId appid = i.appId();

            add(APP_ID, String.valueOf(appid.id()) + " : " + appid.name());
            add(KEY, i.key().toString());
            add(TYPE, i.getClass().getSimpleName());
            add(PRIORITY, Integer.toString(i.priority()));
        }

        @Override
        protected String[] columnIds() {
            return COL_IDS;
        }
    }

}
