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

import org.onosproject.net.Annotations;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Representation of a specific family of device drivers. Behaviour configuration
 * data is stored using {@link org.onosproject.net.Annotations}.
 */
public interface Driver extends Annotations {

    /**
     * Returns the driver name. This is expected to be a reverse-DNS,
     * Java package-like name.
     *
     * @return driver name
     */
    String name();

    /**
     * Returns the parent driver from which this driver inherits behaviours
     * and properties.
     *
     * @return parent driver; null if driver has no parent
     * @deprecated 1.5.0 Falcon Release
     */
    @Deprecated
    Driver parent();

    /**
     * Returns all the parent drivers from which this driver inherits behaviours
     * and properties.
     *
     * @return list of parent drivers
     */
    List<Driver> parents();

    /**
     * Returns the device manufacturer name.
     *
     * @return manufacturer name
     */
    String manufacturer();

    /**
     * Returns the device hardware version.
     *
     * @return hardware version
     */
    String hwVersion();

    /**
     * Returns the device software version.
     *
     * @return software version
     */
    String swVersion();

    /**
     * Returns the set of behaviours supported by this driver.
     * It reflects behaviours of only this driver and not its parent.
     *
     * @return set of device driver behaviours
     */
    Set<Class<? extends Behaviour>> behaviours();

    /**
     * Returns the implementation class for the specified behaviour.
     * It reflects behaviours of only this driver and not its parent.
     *
     * @param behaviour behaviour interface
     * @return implementation class
     */
    Class<? extends Behaviour> implementation(Class<? extends Behaviour> behaviour);

    /**
     * Indicates whether or not the driver, or any of its parents, support
     * the specified class of behaviour.
     *
     * @param behaviourClass behaviour class
     * @return true if behaviour is supported
     */
    boolean hasBehaviour(Class<? extends Behaviour> behaviourClass);

    /**
     * Creates an instance of behaviour primed with the specified driver data.
     * If the current driver does not support the specified behaviour and the
     * driver has parent, the request is delegated to the parent driver.
     *
     * @param data           driver data context
     * @param behaviourClass driver behaviour class
     * @param <T>            type of behaviour
     * @return behaviour instance
     */
    <T extends Behaviour> T createBehaviour(DriverData data, Class<T> behaviourClass);

    /**
     * Creates an instance of behaviour primed with the specified driver handler.
     * If the current driver does not support the specified behaviour and the
     * driver has parent, the request is delegated to the parent driver.
     *
     * @param handler        driver handler context
     * @param behaviourClass driver behaviour class
     * @param <T>            type of behaviour
     * @return behaviour instance
     */
    <T extends Behaviour> T createBehaviour(DriverHandler handler, Class<T> behaviourClass);

    /**
     * Returns the set of annotations as map of key/value properties.
     *
     * @return map of properties
     */
    Map<String, String> properties();

    /**
     * Gets the value of given property name.
     * If the driver does not define the property, a BFS will be performed to search its ancestors.
     *
     * @param name property name
     * @return the value of the property,
     *         or null if the property is not defined in this driver nor in any of its ancestors
     */
    String getProperty(String name);

    /**
     * Gets the value of given property name.
     * If the driver does not define the property, a BFS will be performed to search its ancestors.
     *
     * @param name property name
     * @param defaultValue to use if the property is not defined in this driver nor in any of its ancestors
     * @return the value of the property,
     *         or {@code defaultValue} if the property is not defined in this driver nor in any of its ancestors
     */
    default String getProperty(String name, String defaultValue) {
        return Optional.ofNullable(getProperty(name)).orElse(defaultValue);
    }

    /**
     * Merges the specified driver behaviours and properties into this one,
     * giving preference to the other driver when dealing with conflicts.
     *
     * @param other other driver
     * @return merged driver
     */
    Driver merge(Driver other);

}
