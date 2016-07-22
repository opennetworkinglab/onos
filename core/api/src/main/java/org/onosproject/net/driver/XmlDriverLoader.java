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

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.XMLConfiguration;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
    private static final String EXTENDS = "[@extends]";
    private static final String MFG = "[@manufacturer]";
    private static final String HW = "[@hwVersion]";
    private static final String SW = "[@swVersion]";
    private static final String API = "[@api]";
    private static final String IMPL = "[@impl]";

    private final ClassLoader classLoader;
    private final BehaviourClassResolver resolver;

    private Map<String, Driver> drivers = Maps.newHashMap();

    /**
     * Creates a new driver loader capable of loading drivers from the supplied
     * class loader.
     *
     * @param classLoader class loader to use
     * @deprecated since 1.7.0 (Hummingbird)
     */
    @Deprecated
    public XmlDriverLoader(ClassLoader classLoader) {
        this(classLoader, null);
    }

    /**
     * Creates a new driver loader capable of loading drivers from the supplied
     * class loader.
     *
     * @param classLoader class loader to use
     * @param resolver    behaviour class resolver
     */
    public XmlDriverLoader(ClassLoader classLoader, BehaviourClassResolver resolver) {
        this.classLoader = classLoader;
        this.resolver = resolver;
    }

    /**
     * Loads the specified drivers resource as an XML stream and parses it to
     * produce a ready-to-register driver provider.
     *
     * @param driversStream stream containing the drivers definitions
     * @param resolver      driver resolver
     * @return driver provider
     * @throws java.io.IOException if issues are encountered reading the stream
     *                             or parsing the driver definitions within
     */
    public DefaultDriverProvider loadDrivers(InputStream driversStream,
                                             DriverResolver resolver) throws IOException {
        try {
            XMLConfiguration cfg = new XMLConfiguration();
            cfg.setRootElementName(DRIVERS);
            cfg.setAttributeSplittingDisabled(true);

            cfg.load(driversStream);
            return loadDrivers(cfg, resolver);
        } catch (ConfigurationException e) {
            throw new IOException("Unable to load drivers", e);
        }
    }

    /**
     * Loads a driver provider from the supplied hierarchical configuration.
     *
     * @param driversCfg hierarchical configuration containing the drivers definitions
     * @param resolver   driver resolver
     * @return driver provider
     */
    public DefaultDriverProvider loadDrivers(HierarchicalConfiguration driversCfg,
                                             DriverResolver resolver) {
        DefaultDriverProvider provider = new DefaultDriverProvider();
        for (HierarchicalConfiguration cfg : driversCfg.configurationsAt(DRIVER)) {
            DefaultDriver driver = loadDriver(cfg, resolver);
            drivers.put(driver.name(), driver);
            provider.addDriver(driver);
        }
        drivers.clear();
        return provider;
    }

    /**
     * Loads a driver from the supplied hierarchical configuration.
     *
     * @param driverCfg hierarchical configuration containing the driver definition
     * @param resolver  driver resolver
     * @return driver
     */
    public DefaultDriver loadDriver(HierarchicalConfiguration driverCfg,
                                    DriverResolver resolver) {
        String name = driverCfg.getString(NAME);
        String parentsString = driverCfg.getString(EXTENDS, "");
        List<Driver> parents = Lists.newArrayList();
        if (!parentsString.equals("")) {
            List<String> parentsNames;
            if (parentsString.contains(",")) {
                parentsNames = Arrays.asList(parentsString.replace(" ", "").split(","));
            } else {
                parentsNames = Lists.newArrayList(parentsString);
            }
            parents = parentsNames.stream().map(parent -> (parent != null) ?
                    resolve(parent, resolver) : null).collect(Collectors.toList());
        }
        String manufacturer = driverCfg.getString(MFG, "");
        String hwVersion = driverCfg.getString(HW, "");
        String swVersion = driverCfg.getString(SW, "");
        return new DefaultDriver(name, parents, manufacturer, hwVersion, swVersion,
                                 parseBehaviours(driverCfg),
                                 parseProperties(driverCfg));
    }

    // Resolves the driver by name locally at first and then using the specified resolver.
    private Driver resolve(String parentName, DriverResolver resolver) {
        Driver driver = drivers.get(parentName);
        return driver != null ? driver :
                (resolver != null ? resolver.getDriver(parentName) : null);
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
            if (resolver != null) {
                Class<? extends Behaviour> cls = resolver.getBehaviourClass(className);
                if (cls != null) {
                    return cls;
                }
            }
            throw new IllegalArgumentException("Unable to resolve class " + className, e);
        }
    }

}
