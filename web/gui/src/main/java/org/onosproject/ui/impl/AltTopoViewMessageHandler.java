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
 *
 */

package org.onosproject.ui.impl;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableSet;
import org.onlab.osgi.ServiceDirectory;
import org.onosproject.core.CoreService;
import org.onosproject.ui.RequestHandler;
import org.onosproject.ui.UiConnection;
import org.onosproject.ui.UiMessageHandler;
import org.onosproject.ui.impl.topo.TopoUiEvent;
import org.onosproject.ui.impl.topo.TopoUiListener;
import org.onosproject.ui.impl.topo.TopoUiModelService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Facility for handling inbound messages from the topology view, and
 * generating outbound messages for the same.
 */
public class AltTopoViewMessageHandler extends UiMessageHandler {

    private static final String TOPO_START = "topoStart";
    private static final String TOPO_STOP = "topoStop";

    private final Logger log = LoggerFactory.getLogger(getClass());

    protected ServiceDirectory directory;
    protected TopoUiModelService modelService;

    private TopoUiListener modelListener;
    private String version;

    private boolean topoActive = false;

    @Override
    public void init(UiConnection connection, ServiceDirectory directory) {
        super.init(connection, directory);
        this.directory = checkNotNull(directory, "Directory cannot be null");
        modelService = directory.get(TopoUiModelService.class);

        modelListener = new ModelListener();
        version = getVersion();
    }


    private String getVersion() {
        String ver = directory.get(CoreService.class).version().toString();
        return ver.replace(".SNAPSHOT", "*").replaceFirst("~.*$", "");
    }


    @Override
    protected Collection<RequestHandler> getHandlers() {
        return ImmutableSet.of(
                new TopoStart(),
                new TopoStop()
        );
    }

    // =====================================================================
    // Request Handlers for (topo view) events from the UI...

    private final class TopoStart extends RequestHandler {
        private TopoStart() {
            super(TOPO_START);
        }

        @Override
        public void process(long sid, ObjectNode payload) {
            topoActive = true;
            modelService.addListener(modelListener);
            sendMessages(modelService.getInitialState());
            log.debug(TOPO_START);
        }
    }

    private final class TopoStop extends RequestHandler {
        private TopoStop() {
            super(TOPO_STOP);
        }

        @Override
        public void process(long sid, ObjectNode payload) {
            topoActive = false;
            modelService.removeListener(modelListener);
            log.debug(TOPO_STOP);
        }
    }

    // =====================================================================
    // Private Helper Methods...

    private void sendMessages(Collection<ObjectNode> messages) {
        if (topoActive) {
            UiConnection connection = connection();
            if (connection != null) {
                messages.forEach(connection::sendMessage);
            }
        }
    }

    // =====================================================================
    // Our listener for model events so we can push changes out to the UI...

    private class ModelListener implements TopoUiListener {
        @Override
        public void event(TopoUiEvent event) {
            log.debug("Handle Event: {}", event);
            // TODO: handle event
        }
    }
}
