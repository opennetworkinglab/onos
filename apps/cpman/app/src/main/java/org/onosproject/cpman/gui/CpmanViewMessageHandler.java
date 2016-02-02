/*
 * Copyright 2016 Open Networking Laboratory
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
package org.onosproject.cpman.gui;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableSet;
import org.onosproject.ui.RequestHandler;
import org.onosproject.ui.UiMessageHandler;

import java.util.Collection;
import java.util.Random;

/**
 * CpmanViewMessageHandler class implementation.
 */
public class CpmanViewMessageHandler extends UiMessageHandler {

    private static final String CPMAN_DATA_REQ = "cpmanDataRequest";
    private static final String CPMAN_DATA_RESP = "cpmanDataResponse";

    private static final String RANDOM = "random";

    @Override
    protected Collection<RequestHandler> createRequestHandlers() {
        return ImmutableSet.of(
                new CpmanDataRequestHandler()
        );
    }

    // handler for sample data requests
    private final class CpmanDataRequestHandler extends RequestHandler {

        private CpmanDataRequestHandler() {
            super(CPMAN_DATA_REQ);
        }

        @Override
        public void process(long sid, ObjectNode payload) {
            ObjectNode result = objectNode();
            Random random = new Random();
            result.put(RANDOM, random.nextInt(50) + 1);

            sendMessage(CPMAN_DATA_RESP, 0, result);
        }
    }
}
