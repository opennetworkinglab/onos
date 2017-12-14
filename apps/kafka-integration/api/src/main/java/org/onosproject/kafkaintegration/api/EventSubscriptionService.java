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
package org.onosproject.kafkaintegration.api;

import java.util.List;

import org.onosproject.kafkaintegration.api.dto.EventSubscriber;
import org.onosproject.kafkaintegration.api.dto.OnosEvent.Type;
import org.onosproject.kafkaintegration.api.dto.RegistrationResponse;
import org.onosproject.kafkaintegration.errors.InvalidApplicationException;
import org.onosproject.kafkaintegration.errors.InvalidGroupIdException;

import com.google.common.annotations.Beta;

/**
 * APIs for subscribing to Onos Event Messages.
 */
@Beta
public interface EventSubscriptionService {

    /**
     * Registers the external application to receive events generated in ONOS.
     *
     * @param appName Application Name
     * @return Registration Response DTO.
     */
    RegistrationResponse registerListener(String appName);

    /**
     * Removes the Registered Listener.
     *
     * @param appName Application Name
     */
    void unregisterListener(String appName);

    /**
     * Allows registered listener to subscribe for a specific event type.
     *
     * @param subscriber Subscription data containing the event type
     * @throws InvalidGroupIdException if the subscriber group is duplicate
     * @throws InvalidApplicationException if the application has not been registered
     */
    void subscribe(EventSubscriber subscriber)
            throws InvalidGroupIdException, InvalidApplicationException;

    /**
     * Allows the registered listener to unsubscribe for a specific event.
     *
     * @param subscriber Subscription data containing the event type
     * @throws InvalidGroupIdException if the subscriber group does not exist
     * @throws InvalidApplicationException if the application has not been registered
     */
    void unsubscribe(EventSubscriber subscriber)
            throws InvalidGroupIdException, InvalidApplicationException;

    /**
     * Returns the event subscriber for various event types.
     *
     * @param type ONOS event type.
     * @return List of event subscribers
     */
    List<EventSubscriber> getEventSubscribers(Type type);
}
