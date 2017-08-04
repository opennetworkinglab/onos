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

/**
 * Representation of a facet of behaviour that can be used to interact
 * with an entity (in context of {@link org.onosproject.net.driver.DriverHandler}).
 */
public interface HandlerBehaviour extends Behaviour {

    /**
     * Returns the driver handler context on which this behaviour operates.
     *
     * @return driver handler context
     */
    DriverHandler handler();

    /**
     * Sets the driver handler context for this behaviour.
     *
     * @param handler driver handler
     */
    void setHandler(DriverHandler handler);

}
