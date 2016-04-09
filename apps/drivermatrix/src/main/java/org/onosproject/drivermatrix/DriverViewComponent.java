/*
 * Copyright 2016-present Open Networking Laboratory
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
package org.onosproject.drivermatrix;

import com.google.common.collect.ImmutableList;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onosproject.ui.UiExtension;
import org.onosproject.ui.UiExtensionService;
import org.onosproject.ui.UiMessageHandlerFactory;
import org.onosproject.ui.UiView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Registers driver matrix view.
 */
@Component(immediate = true)
public class DriverViewComponent {

    private static final String VIEW_ID = "driverMatrix";
    private static final String VIEW_TEXT = "Driver Matrix";
    private static final String NAV_ICON = "nav_drivers";

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected UiExtensionService uiExtensionService;

    // List of application views
    private final List<UiView> uiViews = ImmutableList.of(
            new UiView(UiView.Category.PLATFORM, VIEW_ID, VIEW_TEXT, NAV_ICON)
    );

    // Factory for UI message handlers
    private final UiMessageHandlerFactory messageHandlerFactory =
            () -> ImmutableList.of(
                    new DriverViewMessageHandler()
            );

    // Application UI extension
    protected UiExtension extension =
            new UiExtension.Builder(getClass().getClassLoader(), uiViews)
                    .resourcePath(VIEW_ID)
                    .messageHandlerFactory(messageHandlerFactory)
                    .build();

    @Activate
    protected void activate() {
        uiExtensionService.register(extension);
        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        uiExtensionService.unregister(extension);
        log.info("Stopped");
    }

}
