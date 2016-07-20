/*
 * Copyright 2014-2016 Open Networking Laboratory
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

package org.onosproject.drivers.bmv2;

import org.apache.felix.scr.annotations.Component;
import org.onosproject.net.driver.AbstractDriverLoader;

/**
 * Loader for BMv2 drivers from xml file.
 */
@Component(immediate = true)
public class Bmv2DriversLoader extends AbstractDriverLoader {

    private static final String DRIVERS_XML = "/bmv2-drivers.xml";

    public Bmv2DriversLoader() {
        super(DRIVERS_XML);
    }
}