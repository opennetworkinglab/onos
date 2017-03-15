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

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract bootstrapper for loading and registering driver definitions that
 * are independent from the default driver definitions.
 */
@Component
public abstract class AbstractIndependentDriverLoader {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private DriverProvider provider;
    private final String path;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DriverAdminService driverAdminService;

    /**
     * Creates a new loader for resource with the specified path.
     *
     * @param path drivers definition XML resource path
     */
    protected AbstractIndependentDriverLoader(String path) {
        this.path = path;
    }

    @Activate
    protected void activate() {
        try {
            provider = new XmlDriverLoader(getClass().getClassLoader(), driverAdminService)
                    .loadDrivers(getClass().getResourceAsStream(path), driverAdminService);
            driverAdminService.registerProvider(provider);
        } catch (Exception e) {
            log.error("Unable to load {} driver definitions", path, e);
        }
        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        driverAdminService.unregisterProvider(provider);
        log.info("Stopped");
    }

}
