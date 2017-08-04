/*
 * Copyright 2016-present Open Networking Foundation
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
import org.onosproject.ui.RequestHandler;
import org.onosproject.ui.UiMessageHandler;
import org.onosproject.ui.UiPreferencesService;

import java.util.Collection;

import static com.google.common.base.Strings.isNullOrEmpty;

/**
 * Message handler for intercepting user preferences messages.
 */
class UserPreferencesMessageHandler extends UiMessageHandler {

    private static final String UPDATE_PREFS_REQ = "updatePrefReq";
    private static final String KEY = "key";
    private static final String VALUE = "value";

    @Override
    protected Collection<RequestHandler> createRequestHandlers() {
        return ImmutableSet.of(new UpdatePreferencesRequest());
    }

    private final class UpdatePreferencesRequest extends RequestHandler {
        private UpdatePreferencesRequest() {
            super(UPDATE_PREFS_REQ);
        }

        @Override
        public void process(ObjectNode payload) {
            if (!isNullOrEmpty(connection().userName())) {
                UiPreferencesService service = get(UiPreferencesService.class);
                service.setPreference(connection().userName(),
                        payload.get(KEY).asText(),
                        (ObjectNode) payload.get(VALUE));
            }
        }
    }
}
