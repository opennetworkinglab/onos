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

import org.onlab.osgi.DefaultServiceDirectory;
import org.onlab.osgi.ServiceDirectory;

import static com.google.common.base.MoreObjects.toStringHelper;

/**
 * Default implementation of driver handler.
 */
public class DefaultDriverHandler implements DriverHandler {

    private final DriverData data;

    // Reference to service directory to provide run-time context.
    protected static ServiceDirectory serviceDirectory = new DefaultServiceDirectory();

    /**
     * Creates new driver handler with the attached driver data.
     *
     * @param data driver data to attach
     */
    public DefaultDriverHandler(DriverData data) {
        this.data = data;
    }

    @Override
    public Driver driver() {
        return data.driver();
    }

    @Override
    public DriverData data() {
        return data;
    }

    @Override
    public <T extends Behaviour> T behaviour(Class<T> behaviourClass) {
        return data.driver().createBehaviour(this, behaviourClass);
    }

    @Override
    public <T> T get(Class<T> serviceClass) {
        return serviceDirectory.get(serviceClass);
    }

    @Override
    public String toString() {
        return toStringHelper(this).add("data", data).toString();
    }

}
