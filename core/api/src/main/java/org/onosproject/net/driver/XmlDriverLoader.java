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

import com.google.common.collect.ImmutableMap;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.XMLConfiguration;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

/**
 * Utility capable of reading driver configuration XML resources and producing
 * a device driver provider as a result.
 * <p>
 * The drivers stream structure is as follows:
 * </p>
 * <pre>
 *     &lt;drivers&gt;
 *         &lt;driver name=“...” [manufacturer="..." hwVersion="..." swVersion="..."]&gt;
 *             &lt;behaviour api="..." impl="..."/&gt;
 *             ...
 *             [&lt;property name=“key”&gt;value&lt;/key&gt;]
 *             ...
 *         &lt;/driver&gt;
 *         ...
 *     &lt;/drivers&gt;
 * </pre>
 */
public class XmlDriverLoader {

    private static final String DRIVERS = "drivers";
    private static final String DRIVER = "driver";

    private static final String BEHAVIOUR = "behaviour";
    private static final String PROPERTY = "property";

    private static final String NAME = "[@name]";
    private static final String MFG = "[@manufacturer]";
    private static final String HW = "[@hwVersion]";
    private static final String SW = "[@swVersion]";
    private static final String API = "[@api]";
    private static final String IMPL = "[@impl]";

    private final ClassLoader classLoader;

    /**
     * Creates a new driver loader capable of loading drivers from the supplied
     * class loader.
     *
     * @param classLoader class loader to use
     */
    public XmlDriverLoader(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    /**
     * Loads the specified drivers resource as an XML stream and parses it to
     * produce a ready-to-register driver provider.
     *
     * @param driversStream stream containing the drivers definitions
     * @return driver provider
     * @throws java.io.IOException if issues are encountered reading the stream
     *                             or parsing the driver definitions within
     */
    public DefaultDriverProvider loadDrivers(InputStream driversStream) throws IOException {
        try {
            XMLConfiguration cfg = new XMLConfiguration();
            cfg.setRootElementName(DRIVERS);
            cfg.setAttributeSplittingDisabled(true);

            cfg.load(driversStream);
            return loadDrivers(cfg);
        } catch (ConfigurationException e) {
            throw new IOException("Unable to load drivers", e);
        }
    }

    /**
     * Loads a driver provider from the supplied hierarchical configuration.
     *
     * @param driversCfg hierarchical configuration containing the drivers definitions
     * @return driver provider
     */
    public DefaultDriverProvider loadDrivers(HierarchicalConfiguration driversCfg) {
        DefaultDriverProvider provider = new DefaultDriverProvider();
        for (HierarchicalConfiguration cfg : driversCfg.configurationsAt(DRIVER)) {
            provider.addDriver(loadDriver(cfg));
        }
        return provider;
    }

    /**
     * Loads a driver from the supplied hierarchical configuration.
     *
     * @param driverCfg hierarchical configuration containing the driver definition
     * @return driver
     */
    public DefaultDriver loadDriver(HierarchicalConfiguration driverCfg) {
        String name = driverCfg.getString(NAME);
        String manufacturer = driverCfg.getString(MFG, "");
        String hwVersion = driverCfg.getString(HW, "");
        String swVersion = driverCfg.getString(SW, "");

        return new DefaultDriver(name, manufacturer, hwVersion, swVersion,
                                 parseBehaviours(driverCfg),
                                 parseProperties(driverCfg));
    }

    // Parses the behaviours section.
    private Map<Class<? extends Behaviour>, Class<? extends Behaviour>>
    parseBehaviours(HierarchicalConfiguration driverCfg) {
        ImmutableMap.Builder<Class<? extends Behaviour>,
                Class<? extends Behaviour>> behaviours = ImmutableMap.builder();
        for (HierarchicalConfiguration b : driverCfg.configurationsAt(BEHAVIOUR)) {
            behaviours.put(getClass(b.getString(API)), getClass(b.getString(IMPL)));
        }
        return behaviours.build();
    }

    // Parses the properties section.
    private Map<String, String> parseProperties(HierarchicalConfiguration driverCfg) {
        ImmutableMap.Builder<String, String> properties = ImmutableMap.builder();
        for (HierarchicalConfiguration b : driverCfg.configurationsAt(PROPERTY)) {
            properties.put(b.getString(NAME), (String) b.getRootNode().getValue());
        }
        return properties.build();
    }

    @SuppressWarnings("unchecked")
    private Class<? extends Behaviour> getClass(String className) {
        try {
            return (Class<? extends Behaviour>) classLoader.loadClass(className);
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException("Unable to load class " + className, e);
        }
    }

}
