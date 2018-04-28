/*
 * Copyright 2018-present Open Networking Foundation
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
package org.onosproject.layout;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableSet;
import org.onlab.osgi.ServiceDirectory;
import org.onosproject.ui.RequestHandler;
import org.onosproject.ui.UiConnection;
import org.onosproject.ui.UiMessageHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;

/**
 * ONOS UI Layout Topology-Overlay message handler.
 */
public class LayoutOverlayMessageHandler extends UiMessageHandler {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private static final String DO_LAYOUT = "doLayout";
    private static final String TYPE = "type";

    RoleBasedLayoutManager layoutManager;

    @Override
    public void init(UiConnection connection, ServiceDirectory directory) {
        super.init(connection, directory);
        layoutManager = directory.get(RoleBasedLayoutManager.class);
    }

    @Override
    protected Collection<RequestHandler> createRequestHandlers() {
        return ImmutableSet.of(
                new LayoutHandler()
        );
    }

    private final class LayoutHandler extends RequestHandler {

        public LayoutHandler() {
            super(DO_LAYOUT);
        }

        @Override
        public void process(ObjectNode payload) {
            String algorithm = string(payload, TYPE);
            switch (algorithm) {
                case "access":
                    layoutManager.layout(new AccessNetworkLayout());
                    break;
                default:
                    layoutManager.layout(new DefaultForceLayout());
                    break;
            }
        }
    }

}