/*
 * Copyright 2015-present Open Networking Foundation
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
import org.onosproject.cfg.ComponentConfigService;
import org.onosproject.cfg.ConfigProperty;
import org.onosproject.ui.RequestHandler;
import org.onosproject.ui.UiMessageHandler;
import org.onosproject.ui.table.TableModel;
import org.onosproject.ui.table.TableRequestHandler;

import java.util.Collection;

/**
 * Message handler for component configuration view related messages.
 */
public class SettingsViewMessageHandler extends UiMessageHandler {

    private static final String DATA_REQUEST = "settingDataRequest";
    private static final String DATA_RESPONSE = "settingDataResponse";
    private static final String SETTINGS = "settings";

    private static final String COMPONENT = "component";
    private static final String FQ_COMPONENT = "fqComponent";
    private static final String PROP = "prop";
    private static final String ID = "id";
    private static final String TYPE = "type";
    private static final String VALUE = "value";
    private static final String DEFAULT = "defValue";
    private static final String DESC = "desc";

    private static final char DOT = '.';

    private static final String[] COL_IDS = {
            COMPONENT, FQ_COMPONENT, PROP, ID, TYPE, VALUE, DEFAULT, DESC
    };

    @Override
    protected Collection<RequestHandler> createRequestHandlers() {
        return ImmutableSet.of(new SettingsRequest());
    }

    // handler for host table requests
    private final class SettingsRequest extends TableRequestHandler {
        private static final String NO_ROWS_MESSAGE = "No settings found";

        private SettingsRequest() {
            super(DATA_REQUEST, DATA_RESPONSE, SETTINGS);
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
        protected String defaultColumnId() {
            return COMPONENT;
        }

        @Override
        protected void populateTable(TableModel tm, ObjectNode payload) {
            ComponentConfigService ccs = get(ComponentConfigService.class);
            for (String component : ccs.getComponentNames()) {
                for (ConfigProperty prop : ccs.getProperties(component)) {
                    populateRow(tm.addRow(), component, prop);
                }
            }
        }

        private void populateRow(TableModel.Row row, String fqComp,
                                 ConfigProperty prop) {
            String simple = simpleName(fqComp);

            row.cell(ID, simple + DELIM + prop.name())
                    .cell(FQ_COMPONENT, fqComp)
                    .cell(COMPONENT, simple)
                    .cell(PROP, prop.name())
                    .cell(TYPE, typeName(prop))
                    .cell(VALUE, prop.value())
                    .cell(DEFAULT, prop.defaultValue())
                    .cell(DESC, prop.description());
        }

        // return just simple class name: "a.b.c.MyClass" -> "MyClass"
        private String simpleName(String component) {
            int lastDot = component.lastIndexOf(DOT);
            return lastDot < 0 ? component : component.substring(lastDot + 1);
        }

        private String typeName(ConfigProperty prop) {
            return prop.type().toString().toLowerCase();
        }

        private static final String DELIM = "::";
    }
}
