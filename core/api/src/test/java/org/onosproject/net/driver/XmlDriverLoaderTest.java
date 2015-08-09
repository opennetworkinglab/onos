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

import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.onosproject.net.driver.DefaultDriverDataTest.DEVICE_ID;

/**
 * Tests of the XML driver loader implementation.
 */
public class XmlDriverLoaderTest {

    @Test
    public void basics() throws IOException {
        XmlDriverLoader loader = new XmlDriverLoader(getClass().getClassLoader());
        InputStream stream = getClass().getResourceAsStream("drivers.1.xml");
        DriverProvider provider = loader.loadDrivers(stream, null);
        System.out.println(provider);
        assertEquals("incorrect driver count", 2, provider.getDrivers().size());

        Iterator<Driver> iterator = provider.getDrivers().iterator();
        Driver driver = iterator.next();
        if (!driver.name().equals("foo.1")) {
            driver = iterator.next();
        }

        assertEquals("incorrect driver name", "foo.1", driver.name());
        assertEquals("incorrect driver mfg", "Circus", driver.manufacturer());
        assertEquals("incorrect driver hw", "1.2a", driver.hwVersion());
        assertEquals("incorrect driver sw", "2.2", driver.swVersion());

        assertEquals("incorrect driver behaviours", 1, driver.behaviours().size());
        assertTrue("incorrect driver behaviour", driver.hasBehaviour(TestBehaviour.class));

        assertEquals("incorrect driver properties", 2, driver.properties().size());
        assertTrue("incorrect driver property", driver.properties().containsKey("p1"));
    }

    @Test(expected = IOException.class)
    public void badXML() throws IOException {
        XmlDriverLoader loader = new XmlDriverLoader(getClass().getClassLoader());
        loader.loadDrivers(getClass().getResourceAsStream("drivers.bad.xml"), null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void noClass() throws IOException {
        XmlDriverLoader loader = new XmlDriverLoader(getClass().getClassLoader());
        loader.loadDrivers(getClass().getResourceAsStream("drivers.noclass.xml"), null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void noConstructor() throws IOException {
        XmlDriverLoader loader = new XmlDriverLoader(getClass().getClassLoader());
        InputStream stream = getClass().getResourceAsStream("drivers.noconstructor.xml");
        DriverProvider provider = loader.loadDrivers(stream, null);
        Driver driver = provider.getDrivers().iterator().next();
        driver.createBehaviour(new DefaultDriverData(driver, DEVICE_ID), TestBehaviour.class);
    }

}