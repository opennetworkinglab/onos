/*
 * Copyright 2017-present Open Networking Laboratory
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
 * Entity capable of resolving a driver using the primordial information of
 * the manufacturer, hardware name/version and software version.
 */
public interface DriverPrimordialResolver {

    /**
     * Returns the driver that matches the specified primordial device
     * discovery information.
     *
     * @param mfr device manufacturer
     * @param hw  device hardware name/version
     * @param sw  device software version
     * @return driver or null of no matching one is found
     */
    Driver getDriver(String mfr, String hw, String sw);

}
