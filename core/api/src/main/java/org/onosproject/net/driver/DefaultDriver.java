/*
 * Copyright 2015-present Open Networking Foundation
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
package org.onosproject.net.driver;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.ImmutableMap.copyOf;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Default implementation of extensible driver.
 */
public class DefaultDriver implements Driver {

    private final Logger log = getLogger(getClass());

    private final String name;
    private final List<Driver> parents;

    private final String manufacturer;
    private final String hwVersion;
    private final String swVersion;

    private final Map<Class<? extends Behaviour>, Class<? extends Behaviour>> behaviours;
    private final Map<String, String> properties;

    /**
     * Creates a driver with the specified name.
     *
     * @param name         driver name
     * @param parents      optional parent drivers
     * @param manufacturer device manufacturer
     * @param hwVersion    device hardware version
     * @param swVersion    device software version
     * @param behaviours   device behaviour classes
     * @param properties   properties for configuration of device behaviour classes
     */
    public DefaultDriver(String name, List<Driver> parents, String manufacturer,
                         String hwVersion, String swVersion,
                         Map<Class<? extends Behaviour>, Class<? extends Behaviour>> behaviours,
                         Map<String, String> properties) {
        this.name = checkNotNull(name, "Name cannot be null");
        this.parents = parents == null ? ImmutableList.of() : ImmutableList.copyOf(parents);
        this.manufacturer = checkNotNull(manufacturer, "Manufacturer cannot be null");
        this.hwVersion = checkNotNull(hwVersion, "HW version cannot be null");
        this.swVersion = checkNotNull(swVersion, "SW version cannot be null");
        this.behaviours = copyOf(checkNotNull(behaviours, "Behaviours cannot be null"));
        this.properties = copyOf(checkNotNull(properties, "Properties cannot be null"));
    }

    @Override
    public Driver merge(Driver other) {
        checkArgument(parents == null || Objects.equals(parent(), other.parent()),
                      "Parent drivers are not the same");

        // Merge the behaviours.
        Map<Class<? extends Behaviour>, Class<? extends Behaviour>>
                behaviours = Maps.newHashMap();
        behaviours.putAll(this.behaviours);
        other.behaviours().forEach(b -> behaviours.put(b, other.implementation(b)));

        // Merge the properties.
        ImmutableMap.Builder<String, String> properties = ImmutableMap.builder();
        properties.putAll(other.properties());

        // remove duplicated properties from this driver and merge
        this.properties().entrySet().stream()
                .filter(e -> !other.properties().containsKey(e.getKey()))
                .forEach(properties::put);

        List<Driver> completeParents = new ArrayList<>();

        if (parents != null) {
            parents.forEach(parent -> other.parents().forEach(otherParent -> {
                if (otherParent.name().equals(parent.name())) {
                    completeParents.add(parent.merge(otherParent));
                } else if (!completeParents.contains(otherParent)) {
                    completeParents.add(otherParent);
                } else if (!completeParents.contains(parent)) {
                    completeParents.add(parent);
                }
            }));
        }
        return new DefaultDriver(name, !completeParents.isEmpty() ? completeParents : other.parents(),
                                 manufacturer, hwVersion, swVersion,
                                 ImmutableMap.copyOf(behaviours), properties.build());
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public String manufacturer() {
        return manufacturer;
    }

    @Override
    public String hwVersion() {
        return hwVersion;
    }

    @Override
    public String swVersion() {
        return swVersion;
    }

    @Override
    public Driver parent() {
        return parents.isEmpty() ? null : parents.get(0);
    }

    @Override
    public List<Driver> parents() {
        return parents;
    }

    @Override
    public Set<Class<? extends Behaviour>> behaviours() {
        return behaviours.keySet();
    }

    @Override
    public Class<? extends Behaviour> implementation(Class<? extends Behaviour> behaviour) {
        return behaviours.get(behaviour);
    }

    @Override
    public boolean hasBehaviour(Class<? extends Behaviour> behaviourClass) {
        return behaviours.containsKey(behaviourClass) ||
                (parents != null && parents.stream()
                        .filter(parent -> parent.hasBehaviour(behaviourClass)).count() > 0);
    }

    @Override
    public <T extends Behaviour> T createBehaviour(DriverData data,
                                                   Class<T> behaviourClass) {
        T behaviour = createBehaviour(data, null, behaviourClass);
        if (behaviour != null) {
            return behaviour;
        } else if (parents != null) {
            for (Driver parent : Lists.reverse(parents)) {
                try {
                    return parent.createBehaviour(data, behaviourClass);
                } catch (IllegalArgumentException e) {
                    log.debug("Parent {} does not support behaviour {}", parent, behaviourClass);
                }
            }
        }
        throw new IllegalArgumentException(behaviourClass.getName() + " not supported");
    }

    @Override
    public <T extends Behaviour> T createBehaviour(DriverHandler handler,
                                                   Class<T> behaviourClass) {
        T behaviour = createBehaviour(handler.data(), handler, behaviourClass);
        if (behaviour != null) {
            return behaviour;
        } else if (parents != null && !parents.isEmpty()) {
            for (Driver parent : Lists.reverse(parents)) {
                try {
                    return parent.createBehaviour(handler, behaviourClass);
                } catch (IllegalArgumentException e) {
                    log.debug("Parent {} does not support behaviour {}", parent, behaviourClass);
                }
            }
        }
        throw new IllegalArgumentException(behaviourClass.getName() + " not supported");
    }

    // Creates an instance of behaviour primed with the specified driver data.
    private <T extends Behaviour> T createBehaviour(DriverData data, DriverHandler handler,
                                                    Class<T> behaviourClass) {
        //checkArgument(handler != null || !HandlerBehaviour.class.isAssignableFrom(behaviourClass),
        //              "{} is applicable only to handler context", behaviourClass.getName());

        // Locate the implementation of the requested behaviour.
        Class<? extends Behaviour> implementation = behaviours.get(behaviourClass);
        if (implementation != null) {
            // Create an instance of the behaviour and apply data as its context.
            T behaviour = createBehaviour(behaviourClass, implementation);
            behaviour.setData(data);

            // If this is a handler behaviour, also apply handler as its context.
            if (handler != null) {
                ((HandlerBehaviour) behaviour).setHandler(handler);
            }
            return behaviour;
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private <T extends Behaviour> T createBehaviour(Class<T> behaviourClass,
                                                    Class<? extends Behaviour> implementation) {
        try {
            return (T) implementation.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            // TODO: add a specific unchecked exception
            throw new IllegalArgumentException("Unable to create behaviour", e);
        }
    }

    @Override
    public Set<String> keys() {
        return properties.keySet();
    }

    @Override
    public String value(String key) {
        return properties.get(key);
    }

    @Override
    public Map<String, String> properties() {
        return properties;
    }

    @Override
    public String getProperty(String name) {
        Queue<Driver> queue = new LinkedList<>();
        queue.add(this);
        while (!queue.isEmpty()) {
            Driver driver = queue.remove();
            String property = driver.properties().get(name);
            if (property != null) {
                return property;
            } else if (driver.parents() != null) {
                queue.addAll(driver.parents());
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("name", name)
                .add("parents", parents)
                .add("manufacturer", manufacturer)
                .add("hwVersion", hwVersion)
                .add("swVersion", swVersion)
                .add("behaviours", behaviours)
                .add("properties", properties)
                .toString();
    }

    @Override
    public boolean equals(Object driverToBeCompared) {
        if (this == driverToBeCompared) {
            return true;
        }
        if (driverToBeCompared == null || getClass() != driverToBeCompared.getClass()) {
            return false;
        }

        DefaultDriver driver = (DefaultDriver) driverToBeCompared;

        return name.equals(driver.name());

    }

    @Override
    public int hashCode() {
        return Objects.hashCode(name);
    }
}
