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
package org.onosproject.vtnweb.gui;

import static com.google.common.collect.ImmutableList.of;
import static org.onosproject.ui.UiView.Category.NETWORK;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.List;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onosproject.ui.UiExtension;
import org.onosproject.ui.UiExtensionService;
import org.onosproject.ui.UiMessageHandlerFactory;
import org.onosproject.ui.UiView;
import org.slf4j.Logger;

import com.google.common.collect.ImmutableList;

/**
 * service function chain gui.
 */
@Component(immediate = true, enabled = true)
@Service(value = SfcUiExtensionManager.class)
public class SfcUiExtensionManager {
    private final Logger log = getLogger(getClass());

    private static final ClassLoader CL =
            SfcUiExtensionManager.class.getClassLoader();
    private static final String GUI = "gui";

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
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
