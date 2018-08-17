/**
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
package org.onosproject.kafkaintegration.impl;

import com.google.common.collect.ImmutableList;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.kafkaintegration.api.EventSubscriptionService;
import org.onosproject.kafkaintegration.api.KafkaConfigService;
import org.onosproject.kafkaintegration.api.dto.DefaultEventSubscriber;
import org.onosproject.kafkaintegration.api.dto.EventSubscriber;
import org.onosproject.kafkaintegration.api.dto.EventSubscriberGroupId;
import org.onosproject.kafkaintegration.api.dto.OnosEvent;
import org.onosproject.kafkaintegration.api.dto.OnosEvent.Type;
import org.onosproject.kafkaintegration.api.dto.RegistrationResponse;
import org.onosproject.kafkaintegration.errors.InvalidApplicationException;
import org.onosproject.kafkaintegration.errors.InvalidGroupIdException;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.Serializer;
import org.onosproject.store.service.StorageService;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Implementation of Event Subscription Manager.
 *
 */
@Component(immediate = true, service = EventSubscriptionService.class)
public class EventSubscriptionManager implements EventSubscriptionService {

    private final Logger log = LoggerFactory.getLogger(getClass());

    // Stores the currently registered applications for event export service.
    private Map<ApplicationId, EventSubscriberGroupId> registeredApps;

    private Map<Type, List<EventSubscriber>> subscriptions;

    private static final String REGISTERED_APPS = "registered-applications";

    private static final String SUBSCRIBED_APPS = "event-subscriptions";

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected StorageService storageService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected KafkaConfigService kafkaConfigService;

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

        subscriptions = storageService
                .<Type, List<EventSubscriber>>consistentMapBuilder()
                .withName(SUBSCRIBED_APPS)
                .withSerializer(Serializer
                        .using(KryoNamespaces.API, EventSubscriber.class,
                               OnosEvent.class, OnosEvent.Type.class,
                               DefaultEventSubscriber.class,
                               EventSubscriberGroupId.class, UUID.class))
                .build().asJavaMap();

        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        log.info("Stopped");
    }

    @Override
    public RegistrationResponse registerListener(String appName) {

        // TODO: Remove it once ONOS provides a mechanism for external apps
        // to register with the core service. See Jira - 4409
        ApplicationId externalAppId = coreService.registerApplication(appName);

        EventSubscriberGroupId id =
                registeredApps.computeIfAbsent(externalAppId,
                                               (key) -> new EventSubscriberGroupId(UUID
                                                       .randomUUID()));

        RegistrationResponse response = new RegistrationResponse(id,
                            kafkaConfigService.getConfigParams().getIpAddress(),
                            kafkaConfigService.getConfigParams().getPort());

        return response;
    }

    @Override
    public void unregisterListener(String appName) {
        ApplicationId externalAppId =
                checkNotNull(coreService.getAppId(appName));
        registeredApps.remove(externalAppId);
    }

    @Override
    public void subscribe(EventSubscriber subscriber)
            throws InvalidGroupIdException, InvalidApplicationException {

        checkNotNull(subscriber);

        if (!registeredApplication(subscriber.appName())) {
            throw new InvalidApplicationException("Application is not "
                    + "registered to make this request.");
        }

        if (!validGroupId(subscriber.subscriberGroupId(),
                          subscriber.appName())) {
            throw new InvalidGroupIdException("Incorrect group id in the request");
        }

        // update internal state
        List<EventSubscriber> subscriptionList =
                subscriptions.get(subscriber.eventType());
        if (subscriptionList == null) {
            subscriptionList = new ArrayList<EventSubscriber>();
        }
        subscriptionList.add(subscriber);
        subscriptions.put(subscriber.eventType(), subscriptionList);

        log.info("Subscription for {} event by {} successful",
                 subscriber.eventType(), subscriber.appName());
    }

    /**
     * Checks if the application has registered.
     *
     * @param appName application name
     * @return true if application has registered
     */
    private boolean registeredApplication(String appName) {

        checkNotNull(appName);
        ApplicationId appId = checkNotNull(coreService.getAppId(appName));
        if (registeredApps.containsKey(appId)) {
            return true;
        }

        log.debug("{} is not registered", appName);
        return false;
    }

    /**
     * Checks if the group id is valid for this registered application.
     *
     * @param groupId GroupId assigned to the subscriber
     * @param appName Registered Application name
     * @return true if valid groupId and false otherwise
     */
    private boolean validGroupId(EventSubscriberGroupId groupId,
                                 String appName) {

        checkNotNull(groupId);

        ApplicationId appId = coreService.getAppId(appName);
        EventSubscriberGroupId registeredGroupId = registeredApps.get(appId);
        if (registeredGroupId.equals(groupId)) {
            return true;
        }

        return false;
    }

    @Override
    public void unsubscribe(EventSubscriber subscriber)
            throws InvalidGroupIdException, InvalidApplicationException {

        checkNotNull(subscriber);

        if (!registeredApplication(subscriber.appName())) {
            throw new InvalidApplicationException("Application is not "
                    + "registered to make this request.");
        }

        if (!validGroupId(subscriber.subscriberGroupId(),
                          subscriber.appName())) {
            throw new InvalidGroupIdException("Incorrect group id in the request");
        }

        if (!eventSubscribed(subscriber)) {
            log.error("No subscription to {} was found",
                      subscriber.eventType());
            return;
        }

        // update internal state.
        List<EventSubscriber> subscribers =
                subscriptions.get(subscriber.eventType());

        subscribers.remove(subscriber);
        subscriptions.put(subscriber.eventType(), subscribers);

        log.info("Unsubscribed {} for {} events", subscriber.appName(),
                 subscriber.eventType());
    }

    @Override
    public List<EventSubscriber> getEventSubscribers(Type type) {
        return subscriptions.getOrDefault(type, ImmutableList.of());
    }

    /**
     * Checks if the subscriber has already subscribed to the requested event
     * type.
     *
     * @param subscriber the subscriber to a specific ONOS event
     * @return true if subscriber has subscribed to the ONOS event
     */
    private boolean eventSubscribed(EventSubscriber subscriber) {

        List<EventSubscriber> subscriberList =
                subscriptions.get(subscriber.eventType());

        if (subscriberList == null) {
            return false;
        }

        return subscriberList.contains(subscriber);
    }

}
