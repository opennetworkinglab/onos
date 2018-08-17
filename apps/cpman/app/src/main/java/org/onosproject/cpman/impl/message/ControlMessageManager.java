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
package org.onosproject.cpman.impl.message;

import org.onosproject.cpman.ControlMessage;
import org.onosproject.cpman.message.ControlMessageAdminService;
import org.onosproject.cpman.message.ControlMessageEvent;
import org.onosproject.cpman.message.ControlMessageListener;
import org.onosproject.cpman.message.ControlMessageProvider;
import org.onosproject.cpman.message.ControlMessageProviderRegistry;
import org.onosproject.cpman.message.ControlMessageProviderService;
import org.onosproject.cpman.message.ControlMessageService;
import org.onosproject.cpman.message.ControlMessageStore;
import org.onosproject.cpman.message.ControlMessageStoreDelegate;
import org.onosproject.net.DeviceId;
import org.onosproject.net.provider.AbstractListenerProviderRegistry;
import org.onosproject.net.provider.AbstractProviderService;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;

import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Provides implementation of the control message SB &amp; NB APIs.
 */
@Component(immediate = true, service = { ControlMessageService.class, ControlMessageAdminService.class,
        ControlMessageProviderRegistry.class })
public class ControlMessageManager
        extends AbstractListenerProviderRegistry<ControlMessageEvent, ControlMessageListener,
        ControlMessageProvider, ControlMessageProviderService>
        implements ControlMessageService, ControlMessageAdminService,
        ControlMessageProviderRegistry {

    private static final String DEVICE_ID_NULL = "Device ID cannot be null";

    private final Logger log = getLogger(getClass());

    private final ControlMessageStoreDelegate delegate = new InternalStoreDelegate();

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected ControlMessageStore store;

    @Activate
    public void activate() {
        store.setDelegate(delegate);
        eventDispatcher.addSink(ControlMessageEvent.class, listenerRegistry);

        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        store.unsetDelegate(delegate);
        eventDispatcher.removeSink(ControlMessageEvent.class);

        log.info("Stopped");
    }

    @Override
    protected ControlMessageProviderService createProviderService(ControlMessageProvider provider) {
        return new InternalControlMessageProviderService(provider);
    }

    private class InternalControlMessageProviderService
            extends AbstractProviderService<ControlMessageProvider>
            implements ControlMessageProviderService {
        InternalControlMessageProviderService(ControlMessageProvider provider) {
            super(provider);
        }

        @Override
        public void updateStatsInfo(DeviceId deviceId, Set<ControlMessage> controlMessages) {
            checkNotNull(deviceId, DEVICE_ID_NULL);
            checkValidity();

            ControlMessageEvent event =
                    store.updateStatsInfo(this.provider().id(), deviceId, controlMessages);

            post(event);
        }
    }

    private class InternalStoreDelegate implements ControlMessageStoreDelegate {
        @Override
        public void notify(ControlMessageEvent event) {
            post(event);
        }
    }
}
