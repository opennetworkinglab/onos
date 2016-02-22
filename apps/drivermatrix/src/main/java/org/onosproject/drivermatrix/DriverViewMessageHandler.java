/*
 * Copyright 2016 Open Networking Laboratory
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

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableSet;
import org.onosproject.net.driver.Behaviour;
import org.onosproject.net.driver.Driver;
import org.onosproject.net.driver.DriverService;
import org.onosproject.ui.RequestHandler;
import org.onosproject.ui.UiMessageHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Message handler for device view related messages.
 */
public class DriverViewMessageHandler extends UiMessageHandler {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private static final String DRIVER_DATA_REQUEST = "driverDataRequest";
    private static final String DRIVER_DATA_RESPONSE = "driverDataResponse";

    private static final String DRIVERS = "drivers";
    private static final String BEHAVIOURS = "behaviours";

    private static final Comparator<? super Class<? extends Behaviour>> BEHAVIOUR_BY_NAME =
            (o1, o2) -> o1.getSimpleName().compareTo(o2.getSimpleName());
    private static final Comparator<? super Driver> DRIVER_BY_NAME =
            (o1, o2) -> o1.name().compareTo(o2.name());


    @Override
    protected Collection<RequestHandler> createRequestHandlers() {
        return ImmutableSet.of(
                new DataRequestHandler()
//                new DetailRequestHandler()
        );
    }

    // handler for device table requests
    private final class DataRequestHandler extends RequestHandler {

        private DataRequestHandler() {
            super(DRIVER_DATA_REQUEST);
        }

        @Override
        public void process(long sid, ObjectNode payload) {
            // Search for drivers producing two artifacts:
            // 1) list of abstract behaviours as column listing
            // 2) sparse matrix of drivers-to-concrete behaviours

            DriverService driverService = get(DriverService.class);

            // Collect all behaviours for all drivers
            Map<Driver, Set<Class<? extends Behaviour>>> driverBehaviours = new HashMap<>();
            driverService.getDrivers().forEach(d -> driverBehaviours.put(d, d.behaviours()));

            // Order all drivers
            List<Driver> drivers = orderDrivers(driverBehaviours.keySet());

            // Produce a union of all behaviours (and order them)
            List<Class<? extends Behaviour>> behaviours = orderBehaviours(driverBehaviours.values());

            // Produce a JSON structure and send it
            sendMessage(DRIVER_DATA_RESPONSE, 0, driversJson(driverBehaviours, drivers, behaviours));
        }

        private List<Driver> orderDrivers(Set<Driver> drivers) {
            // For now order by alphanumeric name of the driver
            List<Driver> ordered = new ArrayList<>(drivers);
            ordered.sort(DRIVER_BY_NAME);
            return ordered;
        }

        private List<Class<? extends Behaviour>>
        orderBehaviours(Collection<Set<Class<? extends Behaviour>>> behaviours) {
            // For now order by alphanumeric name of the abstract behaviour simple name
            Set<Class<? extends Behaviour>> allBehaviours = new HashSet<>();
            behaviours.forEach(allBehaviours::addAll);
            List<Class<? extends Behaviour>> ordered = new ArrayList<>(allBehaviours);
            ordered.sort(BEHAVIOUR_BY_NAME);
            return ordered;
        }

        private ObjectNode driversJson(Map<Driver, Set<Class<? extends Behaviour>>> driverBehaviours,
                                       List<Driver> drivers,
                                       List<Class<? extends Behaviour>> behaviours) {
            ObjectNode root = objectNode();
            addBehaviours(root, behaviours);
            addDrivers(root, drivers);
            addRelationships(root, drivers, behaviours, driverBehaviours);
            return root;
        }

        private void addBehaviours(ObjectNode root, List<Class<? extends Behaviour>> behaviours) {
            ArrayNode array = arrayNode();
            root.set(BEHAVIOURS, array);
            behaviours.forEach(b -> array.add(b.getSimpleName()));
        }

        private void addDrivers(ObjectNode root, List<Driver> drivers) {
            ArrayNode array = arrayNode();
            root.set(DRIVERS, array);
            drivers.forEach(d -> array.add(d.name()));
        }

        private void addRelationships(ObjectNode root,
                                      List<Driver> drivers, List<Class<? extends Behaviour>> behaviours,
                                      Map<Driver, Set<Class<? extends Behaviour>>> driverBehaviours) {
        }
    }

}
