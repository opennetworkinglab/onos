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

import org.onosproject.net.Annotations;

import java.util.Map;
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
     *
     * @return set of device driver behaviours
     */
    Set<Class<? extends Behaviour>> behaviours();

    /**
     * Indicates whether or not the driver supports the specified class
     * of behaviour.
     *
     * @param behaviourClass behaviour class
     * @return true if behaviour is supported
     */
    boolean hasBehaviour(Class<? extends Behaviour> behaviourClass);

    /**
     * Creates an instance of behaviour primed with the specified driver data.
     *
     * @param data           driver data context
     * @param behaviourClass driver behaviour class
     * @param handler        indicates behaviour is intended for handler context
     * @param <T>            type of behaviour
     * @return behaviour instance
     */
    <T extends Behaviour> T createBehaviour(DriverData data, Class<T> behaviourClass,
                                            boolean handler);

    /**
     * Returns the set of annotations as map of key/value properties.
     *
     * @return map of properties
     */
    Map<String, String> properties();

}
