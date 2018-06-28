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

package org.onosproject.drivers.server.gui;

import org.onosproject.ui.UiExtension;
import org.onosproject.ui.UiExtensionService;
import org.onosproject.ui.UiMessageHandlerFactory;
import org.onosproject.ui.UiView;

import com.google.common.collect.ImmutableList;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static org.onosproject.ui.UiView.Category.NETWORK;
import static org.onosproject.ui.GlyphConstants.ENDSTATION;

/**
 * Mechanism to stream CPU data to the GUI.
 */
@Component(immediate = true, enabled = true)
@Service(value = CpuUI.class)
public class CpuUI {

    private final Logger log = LoggerFactory.getLogger(getClass());

    /**
     * GUI Information.
     */
    private static final String CPU_ID = "cpu";
    private static final String CPU_TEXT = "Servers-CPU";
    private static final String RES_PATH = "gui";
    private static final ClassLoader CL = CpuUI.class.getClassLoader();

    // Factory for UI message handlers
    private final UiMessageHandlerFactory messageHandlerFactory =
            () -> ImmutableList.of(new CpuViewMessageHandler());

    // List of application views
    private final List<UiView> views = ImmutableList.of(
            new UiView(NETWORK, CPU_ID, CPU_TEXT, ENDSTATION)
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
    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
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
