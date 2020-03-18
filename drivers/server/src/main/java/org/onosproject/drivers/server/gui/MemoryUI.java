/*
 * Copyright 2020-present Open Networking Foundation
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

package org.onosproject.drivers.server.gui;

import org.onosproject.ui.UiExtension;
import org.onosproject.ui.UiExtensionService;
import org.onosproject.ui.UiMessageHandlerFactory;
import org.onosproject.ui.UiView;

import com.google.common.collect.ImmutableList;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static org.onosproject.ui.UiView.Category.NETWORK;
import static org.onosproject.ui.GlyphConstants.ENDSTATION;

/**
 * Mechanism to stream main memory data to the GUI.
 */
@Component(immediate = true, service = MemoryUI.class)
public class MemoryUI {

    private final Logger log = LoggerFactory.getLogger(getClass());

    /**
     * GUI Information.
     */
    private static final String MEMORY_ID = "memory";
    private static final String MEMORY_TEXT = "Servers-Memory";
    private static final String RES_PATH = "gui";
    private static final ClassLoader CL = MemoryUI.class.getClassLoader();

    // Factory for UI message handlers
    private final UiMessageHandlerFactory messageHandlerFactory =
            () -> ImmutableList.of(new MemoryViewMessageHandler());

    // List of application views
    private final List<UiView> views = ImmutableList.of(
            new UiView(NETWORK, MEMORY_ID, MEMORY_TEXT, ENDSTATION)
    );

    // Application UI extension
    private final UiExtension uiExtension =
            new UiExtension.Builder(CL, views)
                    .messageHandlerFactory(messageHandlerFactory)
                    .resourcePath(RES_PATH)
                    .build();

    /**
     * Interact with ONOS.
     */
    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected UiExtensionService uiExtensionService;

    @Activate
    protected void activate() {
        uiExtensionService.register(uiExtension);
        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        uiExtensionService.unregister(uiExtension);
        log.info("Stopped");
    }

}
