/*
 * Copyright 2015-present Open Networking Laboratory
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
 * Representation of a facet of behaviour that can be used to talk about
 * an entity (in context of {@link DriverData}) or to an entity (in context of
 * {@link DriverHandler}).
 */
public interface Behaviour {

    /**
     * Returns the driver data context.
     *
     * @return driver data
     */
    DriverData data();

    /**
     * Sets the driver data context on this this behaviour should operate.
     *
     * @param data driver data
     */
    void setData(DriverData data);

}
