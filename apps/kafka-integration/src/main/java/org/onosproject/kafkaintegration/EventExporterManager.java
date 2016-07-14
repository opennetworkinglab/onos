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
package org.onosproject.kafkaintegration;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Map;
import java.util.UUID;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.kafkaintegration.api.EventExporterService;
import org.onosproject.kafkaintegration.api.dto.EventSubscriber;
import org.onosproject.kafkaintegration.api.dto.EventSubscriberGroupId;
import org.onosproject.kafkaintegration.errors.InvalidApplicationException;
import org.onosproject.kafkaintegration.errors.InvalidGroupIdException;
import org.onosproject.kafkaintegration.errors.UnsupportedEventException;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.link.LinkService;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.Serializer;
import org.onosproject.store.service.StorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of Event Exporter Service.
 *
 */
@Component(immediate = true)
@Service
public class EventExporterManager implements EventExporterService {

    private final Logger log = LoggerFactory.getLogger(getClass());

    // Stores the currently registered applications for event export service.
    // Map of Appname to groupId
    private Map<ApplicationId, EventSubscriberGroupId> registeredApps;

    private static final String REGISTERED_APPS = "registered-applications";

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected LinkService linkService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected StorageService storageService;

    private static final String NOT_YET_SUPPORTED = "Not yet supported.";

    private ApplicationId appId;

    @Activate
    protected void activate() {
        appId = coreService
                .registerApplication("org.onosproject.kafkaintegration");

        registeredApps = storageService
                .<ApplicationId, EventSubscriberGroupId>consistentMapBuilder()
                .withName(REGISTERED_APPS)
                .withSerializer(Serializer.using(KryoNamespaces.API,
                                                 EventSubscriberGroupId.class,
                                                 UUID.class))
                .build().asJavaMap();

        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        log.info("Stopped");
    }

    @Override
    public EventSubscriberGroupId registerListener(String appName) {

        // TODO: Remove it once ONOS provides a mechanism for external apps
        // to register with the core service. See Jira - 4409
        ApplicationId externalAppId = coreService.registerApplication(appName);

        return registeredApps.computeIfAbsent(externalAppId,
                                              (key) -> new EventSubscriberGroupId(UUID
                                                      .randomUUID()));

    }

    @Override
    public void unregisterListener(String appName) {
        ApplicationId externalAppId =
                checkNotNull(coreService.getAppId(appName));
        registeredApps.remove(externalAppId);
    }

    @Override
    public void subscribe(EventSubscriber subscriber)
            throws UnsupportedEventException, InvalidGroupIdException,
            InvalidApplicationException {

        throw new UnsupportedOperationException(NOT_YET_SUPPORTED);
    }

    @Override
    public void unsubscribe(EventSubscriber subscriber)
            throws InvalidGroupIdException, InvalidApplicationException {

        throw new UnsupportedOperationException(NOT_YET_SUPPORTED);
    }
}
