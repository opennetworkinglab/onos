/*
 * Copyright 2015-present Open Networking Laboratory
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

import static com.google.common.base.Strings.isNullOrEmpty;
import static org.onosproject.app.ApplicationState.ACTIVE;

/**
 * Message handler for application view related messages.
 */
public class ApplicationViewMessageHandler extends UiMessageHandler {

    private static final String APP_DATA_REQ = "appDataRequest";
    private static final String APP_DATA_RESP = "appDataResponse";
    private static final String APPS = "apps";

    private static final String APP_MGMT_REQ = "appManagementRequest";

    private static final String APP_DETAILS_REQ = "appDetailsRequest";
    private static final String APP_DETAILS_RESP = "appDetailsResponse";
    private static final String DETAILS = "details";

    private static final String STATE = "state";
    private static final String STATE_IID = "_iconid_state";
    private static final String ID = "id";
    private static final String ICON = "icon";
    private static final String VERSION = "version";
    private static final String CATEGORY = "category";
    private static final String ORIGIN = "origin";
    private static final String TITLE = "title";
    private static final String DESC = "desc";
    private static final String URL = "url";
    private static final String README = "readme";
    private static final String ROLE = "role";
    private static final String REQUIRED_APPS = "required_apps";
    private static final String FEATURES = "features";
    private static final String PERMISSIONS = "permissions";

    private static final String ICON_ID_ACTIVE = "active";
    private static final String ICON_ID_INACTIVE = "appInactive";

    private static final String[] COL_IDS = {
            STATE, STATE_IID, ID, ICON, VERSION, CATEGORY, ORIGIN, TITLE, DESC,
            URL, README, ROLE, REQUIRED_APPS, FEATURES, PERMISSIONS
    };

    @Override
    protected Collection<RequestHandler> createRequestHandlers() {
        return ImmutableSet.of(
                new AppDataRequest(),
                new AppMgmtRequest(),
                new DetailRequestHandler()
        );
    }

    // handler for application table requests
    private final class AppDataRequest extends TableRequestHandler {
        private static final String NO_ROWS_MESSAGE = "No applications found";

        private AppDataRequest() {
            super(APP_DATA_REQ, APP_DATA_RESP, APPS);
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
                    .cell(ICON, id.name())
                    .cell(VERSION, app.version())
                    .cell(CATEGORY, app.category())
                    .cell(ORIGIN, app.origin())
                    .cell(TITLE, app.title())
                    .cell(DESC, app.description())
                    .cell(URL, app.url());
        }
    }

    // handler for application management control button actions
    private final class AppMgmtRequest extends RequestHandler {
        private AppMgmtRequest() {
            super(APP_MGMT_REQ);
        }

        @Override
        public void process(ObjectNode payload) {
            String action = string(payload, "action");
            String name = string(payload, "name");

            if (action != null && name != null) {
                ApplicationAdminService service = get(ApplicationAdminService.class);
                ApplicationId appId = service.getId(name);
                switch (action) {
                    case "activate":
                        service.activate(appId);
                        break;
                    case "deactivate":
                        service.deactivate(appId);
                        break;
                    case "uninstall":
                        service.uninstall(appId);
                        break;
                    default:
                        break;
                }
                chain(APP_DATA_REQ, payload);
            }
        }
    }

    // handler for selected application detail requests
    private final class DetailRequestHandler extends RequestHandler {
        private DetailRequestHandler() {
            super(APP_DETAILS_REQ);
        }

        @Override
        public void process(ObjectNode payload) {
            String id = string(payload, ID);
            ApplicationService as = get(ApplicationService.class);

            // If the ID was not specified in the payload, use the name of the
            // most recently uploaded app.
            if (isNullOrEmpty(id)) {
                id = ApplicationResource.lastInstalledAppName;
            }

            ApplicationId appId = as.getId(id);
            ApplicationState state = as.getState(appId);
            Application app = as.getApplication(appId);
            ObjectNode data = objectNode();

            data.put(STATE, state.toString());
            data.put(ID, appId.name());
            data.put(VERSION, app.version().toString());
            data.put(ROLE, app.role().toString());
            data.put(CATEGORY, app.category());
            data.put(TITLE, app.title());
            data.put(ORIGIN, app.origin());
            data.put(README, app.readme());
            data.put(DESC, app.description());
            data.put(URL, app.url());

            // process required applications
            ArrayNode requiredApps = arrayNode();
            app.requiredApps().forEach(requiredApps::add);

            data.set(REQUIRED_APPS, requiredApps);

            // process features
            ArrayNode features = arrayNode();
            app.features().forEach(features::add);

            data.set(FEATURES, features);

            // process permissions
            ArrayNode permissions = arrayNode();
            app.permissions().forEach(p -> permissions.add(p.getName()));

            data.set(PERMISSIONS, permissions);

            ObjectNode rootNode = objectNode();
            rootNode.set(DETAILS, data);
            sendMessage(APP_DETAILS_RESP, rootNode);
        }

    }
}
