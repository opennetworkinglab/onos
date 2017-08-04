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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Message handler for driver matrix view related messages.
 */
public class DriverViewMessageHandler extends UiMessageHandler {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private static final int ONE = 1;
    private static final String DRIVER_DATA_REQUEST = "driverDataRequest";
    private static final String DRIVER_DATA_RESPONSE = "driverDataResponse";

    private static final String DRIVERS = "drivers";
    private static final String BEHAVIOURS = "behaviours";
    private static final String MATRIX = "matrix";

    private static final Comparator<? super Class<? extends Behaviour>> BEHAVIOUR_BY_NAME =
            Comparator.comparing(Class::getSimpleName);
    private static final Comparator<? super Driver> DRIVER_BY_NAME =
            Comparator.comparing(Driver::name);


    @Override
    protected Collection<RequestHandler> createRequestHandlers() {
        return ImmutableSet.of(
                new DataRequestHandler()
                // TODO: for row selection, produce data for detail panel
//                new DetailRequestHandler()
        );
    }

    // handler for driver matrix requests
    private final class DataRequestHandler extends RequestHandler {

        private DataRequestHandler() {
            super(DRIVER_DATA_REQUEST);
        }

        @Override
        public void process(ObjectNode payload) {
            DriverService driverService = get(DriverService.class);

            List<Driver> drivers = new ArrayList<>(driverService.getDrivers());
            drivers = orderDrivers(drivers);

            // Produce a union of all behaviours (and order them)
            List<Class<? extends Behaviour>> behaviours = orderBehaviours(drivers);

            // Produce a JSON structure and send it
            sendMessage(DRIVER_DATA_RESPONSE, driversJson(drivers, behaviours));
        }

        private List<Driver> orderDrivers(List<Driver> drivers) {
            // For now order by alphanumeric name of the driver
            drivers.sort(DRIVER_BY_NAME);
            return drivers;
        }

        private List<Class<? extends Behaviour>> orderBehaviours(List<Driver> drivers) {
            // first, produce a set-union of all behaviours from all drivers...
            Set<Class<? extends Behaviour>> allBehaviours = new HashSet<>();
            drivers.forEach(d -> allBehaviours.addAll(d.behaviours()));

            // for now, order by alphanumeric name of the abstract behaviour simple name
            List<Class<? extends Behaviour>> ordered = new ArrayList<>(allBehaviours);
            ordered.sort(BEHAVIOUR_BY_NAME);
            return ordered;
        }

        private ObjectNode driversJson(List<Driver> drivers,
                                       List<Class<? extends Behaviour>> behaviours) {
            ObjectNode root = objectNode();
            addBehaviours(root, behaviours);
            addDrivers(root, drivers);
            addMatrixCells(root, drivers);
            return root;
        }

        private void addBehaviours(ObjectNode root,
                                   List<Class<? extends Behaviour>> behaviours) {
            ArrayNode array = arrayNode();
            root.set(BEHAVIOURS, array);
            behaviours.forEach(b -> array.add(b.getSimpleName()));
        }

        private void addDrivers(ObjectNode root, List<Driver> drivers) {
            ArrayNode array = arrayNode();
            root.set(DRIVERS, array);
            drivers.forEach(d -> array.add(d.name()));
        }

        private Set<Driver> findLineage(Driver driver) {
            ImmutableSet.Builder<Driver> lineage = ImmutableSet.builder();
            lineage.add(driver);
            List<Driver> parents = driver.parents();
            if (parents != null) {
                parents.forEach(p -> lineage.addAll(findLineage(p)));
            }
            return lineage.build();
        }

        private void addMatrixCells(ObjectNode root, List<Driver> drivers) {
            ObjectNode matrix = objectNode();
            root.set(MATRIX, matrix);

            drivers.forEach(driver -> {
                ObjectNode dnode = objectNode();
                matrix.set(driver.name(), dnode);
                Set<Driver> lineage = findLineage(driver);
                lineage.forEach(d -> d.behaviours().forEach(b -> {
                    // TODO: can put a payload here, rather than a '1' marker
                    dnode.put(b.getSimpleName(), ONE);
                }));
            });
        }
    }
}
