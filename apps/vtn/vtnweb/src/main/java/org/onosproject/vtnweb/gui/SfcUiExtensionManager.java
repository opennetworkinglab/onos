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
package org.onosproject.vtnweb.gui;

import com.google.common.collect.ImmutableList;
import org.onosproject.ui.UiExtension;
import org.onosproject.ui.UiExtensionService;
import org.onosproject.ui.UiMessageHandlerFactory;
import org.onosproject.ui.UiView;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;

import java.util.List;

import static com.google.common.collect.ImmutableList.of;
import static org.onosproject.ui.UiView.Category.NETWORK;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * service function chain gui.
 */
@Component(immediate = true, service = SfcUiExtensionManager.class)
public class SfcUiExtensionManager {
    private final Logger log = getLogger(getClass());

    private static final ClassLoader CL =
            SfcUiExtensionManager.class.getClassLoader();
    private static final String GUI = "gui";

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected UiExtensionService uiExtensionService;

    // service function chain extension
    private final UiExtension sfc = createSfcExtension();

    // Creates service function chain UI extension
    private UiExtension createSfcExtension() {
        List<UiView> coreViews = of(
                //TODO add a new type of icon for sfc
                new UiView(NETWORK, "sfc", "SFC", "nav_sfcs")
        );

        UiMessageHandlerFactory messageHandlerFactory =
                () -> ImmutableList.of(
                        new SfcViewMessageHandler()
                );

        return new UiExtension.Builder(CL, coreViews)
                .messageHandlerFactory(messageHandlerFactory)
                .resourcePath(GUI)
                .build();
    }

    @Activate
    public void activate() {
        uiExtensionService.register(sfc);
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        uiExtensionService.unregister(sfc);
        log.info("Stopped");
    }
}
