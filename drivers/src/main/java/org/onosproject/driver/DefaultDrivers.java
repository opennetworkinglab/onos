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
package org.onosproject.driver;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onosproject.net.driver.DefaultDriverProviderService;
import org.onosproject.net.driver.DriverAdminService;
import org.onosproject.net.driver.DriverProvider;
import org.onosproject.net.driver.XmlDriverLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;

/**
 * Bootstrap for built in drivers.
 */
@Service
@Component(immediate = false)
public class DefaultDrivers implements DefaultDriverProviderService {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private static final String DRIVERS_XML = "/onos-drivers.xml";

    private DriverProvider provider;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DriverAdminService driverAdminService;

    @Activate
    protected void activate() {
        ClassLoader classLoader = getClass().getClassLoader();
        try {
            InputStream stream = classLoader.getResourceAsStream(DRIVERS_XML);
            provider = new XmlDriverLoader(classLoader)
                    .loadDrivers(stream, driverAdminService);
            driverAdminService.registerProvider(provider);
        } catch (Exception e) {
            log.error("Unable to load default drivers", e);
        }
        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        driverAdminService.unregisterProvider(provider);
        log.info("Stopped");
    }

}
