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

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableSet;
import org.onosproject.app.ApplicationAdminService;
import org.onosproject.app.ApplicationService;
import org.onosproject.app.ApplicationState;
import org.onosproject.core.Application;
import org.onosproject.core.ApplicationId;
import org.onosproject.ui.RequestHandler;
import org.onosproject.ui.UiMessageHandler;
import org.onosproject.ui.table.TableModel;
import org.onosproject.ui.table.TableRequestHandler;

import java.util.Collection;

import static org.onosproject.app.ApplicationState.ACTIVE;

/**
 * Message handler for application view related messages.
 */
public class ApplicationViewMessageHandler extends UiMessageHandler {

    private static final String APP_DATA_REQ = "appDataRequest";
    private static final String APP_DATA_RESP = "appDataResponse";
    private static final String APPS = "apps";

    private static final String APP_MGMT_REQ = "appManagementRequest";

    private static final String STATE = "state";
    private static final String STATE_IID = "_iconid_state";
    private static final String ID = "id";
    private static final String VERSION = "version";
    private static final String ORIGIN = "origin";
    private static final String DESC = "desc";

    private static final String ICON_ID_ACTIVE = "active";
    private static final String ICON_ID_INACTIVE = "appInactive";

    private static final String[] COL_IDS = {
            STATE, STATE_IID, ID, VERSION, ORIGIN, DESC
    };

    @Override
    protected Collection<RequestHandler> createRequestHandlers() {
        return ImmutableSet.of(
                new AppDataRequest(),
                new AppMgmtRequest()
        );
    }

    // handler for application table requests
    private final class AppDataRequest extends TableRequestHandler {
        private AppDataRequest() {
            super(APP_DATA_REQ, APP_DATA_RESP, APPS);
        }

        @Override
        protected String[] getColumnIds() {
            return COL_IDS;
        }

        @Override
        protected void populateTable(TableModel tm, ObjectNode payload) {
            ApplicationService as = get(ApplicationService.class);
            for (Application app : as.getApplications()) {
                populateRow(tm.addRow(), app, as);
            }
        }

        private void populateRow(TableModel.Row row, Application app,
                                 ApplicationService as) {
            ApplicationId id = app.id();
            ApplicationState state = as.getState(id);
            String iconId = state == ACTIVE ? ICON_ID_ACTIVE : ICON_ID_INACTIVE;

            row.cell(STATE, state)
                .cell(STATE_IID, iconId)
                .cell(ID, id.name())
                .cell(VERSION, app.version())
                .cell(ORIGIN, app.origin())
                .cell(DESC, app.description());
        }
    }

    // handler for application management control button actions
    private final class AppMgmtRequest extends RequestHandler {
        private AppMgmtRequest() {
            super(APP_MGMT_REQ);
        }

        @Override
        public void process(long sid, ObjectNode payload) {
            String action = string(payload, "action");
            String name = string(payload, "name");
            if (action != null && name != null) {
                ApplicationAdminService service = get(ApplicationAdminService.class);
                ApplicationId appId = service.getId(name);
                if (action.equals("activate")) {
                    service.activate(appId);
                } else if (action.equals("deactivate")) {
                    service.deactivate(appId);
                } else if (action.equals("uninstall")) {
                    service.uninstall(appId);
                }
                chain(APP_DATA_REQ, sid, payload);
            }
        }
    }
}
