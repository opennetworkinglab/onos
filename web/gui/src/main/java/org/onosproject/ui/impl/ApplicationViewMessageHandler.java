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
import org.onosproject.ui.table.AbstractTableRow;
import org.onosproject.ui.table.TableRequestHandler;
import org.onosproject.ui.table.TableRow;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

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

    @Override
    protected Collection<RequestHandler> getHandlers() {
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
        protected TableRow[] generateTableRows(ObjectNode payload) {
            ApplicationService service = get(ApplicationService.class);
            List<TableRow> list = service.getApplications().stream()
                    .map(application -> new ApplicationTableRow(service, application))
                    .collect(Collectors.toList());
            return list.toArray(new TableRow[list.size()]);
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

    /**
     * TableRow implementation for
     * {@link org.onosproject.core.Application applications}.
     */
    private static class ApplicationTableRow extends AbstractTableRow {

        private static final String[] COL_IDS = {
                STATE, STATE_IID, ID, VERSION, ORIGIN, DESC
        };

        public ApplicationTableRow(ApplicationService service, Application app) {
            ApplicationState state = service.getState(app.id());
            String iconId = state == ACTIVE ? ICON_ID_ACTIVE : ICON_ID_INACTIVE;

            add(STATE, state);
            add(STATE_IID, iconId);
            add(ID, app.id().name());
            add(VERSION, app.version());
            add(ORIGIN, app.origin());
            add(DESC, app.description());
        }

        @Override
        protected String[] columnIds() {
            return COL_IDS;
        }
    }

}
