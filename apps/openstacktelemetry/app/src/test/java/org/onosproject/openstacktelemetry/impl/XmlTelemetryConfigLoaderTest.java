/*
 * Copyright 2018-present Open Networking Foundation
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
package org.onosproject.openstacktelemetry.impl;

import org.junit.Test;
import org.onosproject.openstacktelemetry.api.TelemetryConfigProvider;
import org.onosproject.openstacktelemetry.api.config.TelemetryConfig;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Tests of the XML configuration loader implementation.
 */
public class XmlTelemetryConfigLoaderTest {

    @Test
    public void basics() throws IOException {
        XmlTelemetryConfigLoader loader = new XmlTelemetryConfigLoader();
        InputStream stream = getClass().getResourceAsStream("configs.singleInheritance.xml");
        TelemetryConfigProvider provider = loader.loadTelemetryConfigs(stream);

        assertEquals("incorrect config count", 2, provider.getTelemetryConfigs().size());

        TelemetryConfig config = getConfig(provider, "foo.1");

        assertEquals("incorrect config name", "foo.1", config.name());
        assertEquals("incorrect config type", "grpc", config.type().name().toLowerCase());
        assertEquals("incorrect config mfg", "Circus", config.manufacturer());
        assertEquals("incorrect config sw", "2.2", config.swVersion());

        assertEquals("incorrect config properties", 2, config.properties().size());
        assertTrue("incorrect config property", config.properties().containsKey("p1"));
    }

    @Test
    public void multipleDrivers() throws IOException {
        XmlTelemetryConfigLoader loader = new XmlTelemetryConfigLoader();
        InputStream stream = getClass().getResourceAsStream("configs.multipleInheritance.xml");
        TelemetryConfigProvider provider = loader.loadTelemetryConfigs(stream);

        TelemetryConfig config1 = getConfig(provider, "foo.1");

        assertEquals("incorrect config mfg", "Circus", config1.manufacturer());
        assertEquals("incorrect config sw", "2.2", config1.swVersion());

        assertEquals("incorrect config type", "grpc", config1.type().name().toLowerCase());
        assertEquals("incorrect config properties", 3, config1.properties().size());
        assertTrue("incorrect config property", config1.properties().containsKey("p0"));
        assertTrue("incorrect config property", config1.properties().containsKey("p1"));
        assertTrue("incorrect config property", config1.properties().containsKey("p2"));

        TelemetryConfig config2 = getConfig(provider, "foo.2");
        assertEquals("incorrect config type", "grpc", config2.type().name().toLowerCase());
        assertEquals("incorrect config mfg", "Big Top OEM", config2.manufacturer());
        assertEquals("incorrect config sw", "2.2", config2.swVersion());

        assertEquals("incorrect config properties", 4, config2.properties().size());
        assertTrue("incorrect config property", config2.properties().containsKey("p0"));
        assertTrue("incorrect config property", config2.properties().containsKey("p1"));
        assertTrue("incorrect config property", config2.properties().containsKey("p2"));
        assertTrue("incorrect config property", config2.properties().containsKey("p3"));
    }

    private TelemetryConfig getConfig(TelemetryConfigProvider provider, String name) {
        Iterator<TelemetryConfig> iterator = provider.getTelemetryConfigs().iterator();
        TelemetryConfig config;
        do {
            config = iterator.next();
        } while (!name.equals(config.name()));
        return config;
    }
}
