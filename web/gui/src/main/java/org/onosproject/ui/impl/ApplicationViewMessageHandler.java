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
import org.onosproject.app.ApplicationService;
import org.onosproject.app.ApplicationState;
import org.onosproject.core.Application;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.onosproject.app.ApplicationState.ACTIVE;

/**
 * Message handler for application view related messages.
 */
public class ApplicationViewMessageHandler extends AbstractTabularViewMessageHandler {

    /**
     * Creates a new message handler for the application messages.
     */
    protected ApplicationViewMessageHandler() {
        super(ImmutableSet.of("appDataRequest"));
    }

    @Override
    public void process(ObjectNode message) {
        ObjectNode payload = payload(message);
        String sortCol = string(payload, "sortCol", "id");
        String sortDir = string(payload, "sortDir", "asc");

        ApplicationService service = get(ApplicationService.class);
        TableRow[] rows = generateTableRows(service);
        RowComparator rc =
                new RowComparator(sortCol, RowComparator.direction(sortDir));
        Arrays.sort(rows, rc);
        ArrayNode applications = generateArrayNode(rows);
        ObjectNode rootNode = mapper.createObjectNode();
        rootNode.set("apps", applications);

        connection().sendMessage("appDataResponse", 0, rootNode);
    }

    private TableRow[] generateTableRows(ApplicationService service) {
        List<TableRow> list = service.getApplications().stream()
                .map(application -> new ApplicationTableRow(service, application))
                .collect(Collectors.toList());
        return list.toArray(new TableRow[list.size()]);
    }

    /**
     * TableRow implementation for {@link org.onosproject.core.Application applications}.
     */
    private static class ApplicationTableRow extends AbstractTableRow {

        private static final String STATE = "state";
        private static final String STATE_IID = "_iconid_state";
        private static final String ID = "id";
        private static final String VERSION = "version";
        private static final String ORIGIN = "origin";
        private static final String DESC = "desc";

        private static final String[] COL_IDS = {
                STATE, STATE_IID, ID, VERSION, ORIGIN, DESC
        };

        private static final String ICON_ID_ACTIVE = "appActive";
        private static final String ICON_ID_INACTIVE = "appInactive";


        public ApplicationTableRow(ApplicationService service, Application app) {
            ApplicationState state = service.getState(app.id());
            String iconId = state == ACTIVE ? ICON_ID_ACTIVE : ICON_ID_INACTIVE;

            add(STATE, state.toString());
            add(STATE_IID, iconId);
            add(ID, app.id().name());
            add(VERSION, app.version().toString());
            add(ORIGIN, app.origin());
            add(DESC, app.description());
        }

        @Override
        protected String[] columnIds() {
            return COL_IDS;
        }
    }

}
