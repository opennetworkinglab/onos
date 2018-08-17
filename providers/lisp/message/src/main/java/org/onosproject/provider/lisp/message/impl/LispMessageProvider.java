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
package org.onosproject.provider.lisp.message.impl;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.onlab.metrics.MetricsService;
import org.onosproject.lisp.ctl.LispController;
import org.onosproject.lisp.ctl.LispMessageListener;
import org.onosproject.lisp.ctl.LispRouterId;
import org.onosproject.lisp.ctl.LispRouterListener;
import org.onosproject.lisp.msg.protocols.LispMessage;
import org.onosproject.net.provider.AbstractProvider;
import org.onosproject.net.provider.ProviderId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provider which uses an LISP controller to detect device.
 */
@Component(immediate = true)
public class LispMessageProvider extends AbstractProvider {

    private static final Logger log = LoggerFactory.getLogger(LispMessageProvider.class);

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected LispController controller;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected MetricsService metricsService;

    private static final String SCHEME_NAME = "lisp";
    private static final String MESSAGE_PROVIDER_PACKAGE =
                                        "org.onosproject.lisp.provider.message";

    private final InternalDeviceProvider routerListener = new InternalDeviceProvider();

    private final InternalControlMessageListener messageListener =
                                            new InternalControlMessageListener();

    /**
     * Creates a LISP device provider with the supplier identifier.
     */
    public LispMessageProvider() {
        super(new ProviderId(SCHEME_NAME, MESSAGE_PROVIDER_PACKAGE));
    }

    @Activate
    public void activate() {

        // listens all LISP router related events
        controller.addRouterListener(routerListener);

        // listens all LISP control messages
        controller.addMessageListener(messageListener);

        attachRouters();

        log.info("Started");
    }

    @Deactivate
    public void deactivate() {

        detachRouters();

        // stops listening all LISP router related events
        controller.removeRouterListener(routerListener);

        // stops listening all LISP control messages
        controller.removeMessageListener(messageListener);

        log.info("Stopped");
    }

    /**
     * Attaches all discovered LISP router to listen the router events.
     */
    private void attachRouters() {

    }

    /**
     * Detaches all LISP routers from listener.
     */
    private void detachRouters() {

    }

    /**
     * A routerListener for LISP router agent.
     */
    private class InternalDeviceProvider implements LispRouterListener {

        @Override
        public void routerAdded(LispRouterId routerId) {
        }

        @Override
        public void routerRemoved(LispRouterId routerId) {
        }

        @Override
        public void routerChanged(LispRouterId routerId) {
        }
    }

    /**
     * A routerListener for all LISP control messages.
     */
    private class InternalControlMessageListener implements LispMessageListener {

        @Override
        public void handleIncomingMessage(LispRouterId routerId, LispMessage msg) {
        }

        @Override
        public void handleOutgoingMessage(LispRouterId routerId, LispMessage msg) {
        }
    }
}
