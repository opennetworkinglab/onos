/*
 * Copyright 2015 Open Networking Laboratory
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

import com.google.common.collect.ImmutableMap;

import java.util.Map;
import java.util.Set;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.ImmutableMap.copyOf;

/**
 * Default implementation of extensible driver.
 */
public class DefaultDriver implements Driver {

    private final String name;

    private final String manufacturer;
    private final String hwVersion;
    private final String swVersion;

    private final Map<Class<? extends Behaviour>, Class<? extends Behaviour>> behaviours;
    private final Map<String, String> properties;


    /**
     * Creates a driver with the specified name.
     *
     * @param name         driver name
     * @param manufacturer device manufacturer
     * @param hwVersion    device hardware version
     * @param swVersion    device software version
     * @param behaviours   device behaviour classes
     * @param properties   properties for configuration of device behaviour classes
     */
    public DefaultDriver(String name, String manufacturer,
                         String hwVersion, String swVersion,
                         Map<Class<? extends Behaviour>, Class<? extends Behaviour>> behaviours,
                         Map<String, String> properties) {
        this.name = checkNotNull(name, "Name cannot be null");
        this.manufacturer = checkNotNull(manufacturer, "Manufacturer cannot be null");
        this.hwVersion = checkNotNull(hwVersion, "HW version cannot be null");
        this.swVersion = checkNotNull(swVersion, "SW version cannot be null");
        this.behaviours = copyOf(checkNotNull(behaviours, "Behaviours cannot be null"));
        this.properties = copyOf(checkNotNull(properties, "Properties cannot be null"));
    }

    /**
     * Merges the two drivers while giving preference to this driver when
     * dealing with conflicts.
     *
     * @param other other driver
     * @return new driver
     */
    DefaultDriver merge(DefaultDriver other) {
        // Merge the behaviours.
        ImmutableMap.Builder<Class<? extends Behaviour>, Class<? extends Behaviour>>
                behaviours = ImmutableMap.builder();
        behaviours.putAll(other.behaviours).putAll(this.behaviours);

        // Merge the properties.
        ImmutableMap.Builder<String, String> properties = ImmutableMap.builder();
        properties.putAll(other.properties).putAll(this.properties);

        return new DefaultDriver(name, manufacturer, hwVersion, swVersion,
                                 behaviours.build(), properties.build());
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
    public Set<Class<? extends Behaviour>> behaviours() {
        return behaviours.keySet();
    }

    @Override
    public boolean hasBehaviour(Class<? extends Behaviour> behaviourClass) {
        return behaviours.containsKey(behaviourClass);
    }

    /**
     * Creates an instance of behaviour primed with the specified driver data.
     *
     * @param data           driver data context
     * @param behaviourClass driver behaviour class
     * @param handler        indicates behaviour is intended for handler context
     * @param <T>            type of behaviour
     * @return behaviour instance
     */
    public <T extends Behaviour> T createBehaviour(DriverData data,
                                                   Class<T> behaviourClass,
                                                   boolean handler) {
        checkArgument(handler || !HandlerBehaviour.class.isAssignableFrom(behaviourClass),
                      "{} is applicable only to handler context", behaviourClass.getName());

        // Locate the implementation of the requested behaviour.
        Class<? extends Behaviour> implementation = behaviours.get(behaviourClass);
        checkArgument(implementation != null, "{} not supported", behaviourClass.getName());

        // Create an instance of the behaviour and apply data as its context.
        T behaviour = createBehaviour(behaviourClass, implementation);
        behaviour.setData(data);
        return behaviour;
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
    public String toString() {
        return toStringHelper(this)
                .add("name", name)
                .add("manufacturer", manufacturer)
                .add("hwVersion", hwVersion)
                .add("swVersion", swVersion)
                .add("behaviours", behaviours)
                .add("properties", properties)
                .toString();
    }

}
