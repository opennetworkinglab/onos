/*
 * Copyright 2014 Open Networking Laboratory
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
package org.onosproject.core.impl;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Modified;
import org.onlab.util.SharedExecutors;
import org.onlab.util.Tools;
import org.onosproject.cfg.ComponentConfigService;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.ApplicationIdStore;
import org.onosproject.core.CoreService;
import org.onosproject.core.IdBlockStore;
import org.onosproject.core.IdGenerator;
import org.onosproject.core.Version;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.File;
import java.util.Dictionary;
import java.util.List;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Strings.isNullOrEmpty;

/**
 * Core service implementation.
 */
@Component(immediate = true)
@Service
public class CoreManager implements CoreService {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private static final File VERSION_FILE = new File("../VERSION");
    private static Version version = Version.version("1.2.0-SNAPSHOT");

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ApplicationIdStore applicationIdStore;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected IdBlockStore idBlockStore;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ComponentConfigService cfgService;

    @Property(name = "sharedThreadPoolSize", intValue = SharedExecutors.DEFAULT_THREAD_SIZE,
            label = "Configure shared pool maximum size ")
    private int sharedThreadPoolSize = SharedExecutors.DEFAULT_THREAD_SIZE;

    @Activate
    public void activate() {
        cfgService.registerProperties(getClass());
        List<String> versionLines = Tools.slurp(VERSION_FILE);
        if (versionLines != null && !versionLines.isEmpty()) {
            version = Version.version(versionLines.get(0));
        }
    }

    @Deactivate
    public void deactivate() {
        cfgService.unregisterProperties(getClass(), false);
    }

    @Override
    public Version version() {
        return version;
    }

    @Override
    public Set<ApplicationId> getAppIds() {
        return applicationIdStore.getAppIds();
    }

    @Override
    public ApplicationId getAppId(Short id) {
        return applicationIdStore.getAppId(id);
    }

    @Override
    public ApplicationId getAppId(String name) {
        return applicationIdStore.getAppId(name);
    }


    @Override
    public ApplicationId registerApplication(String name) {
        checkNotNull(name, "Application ID cannot be null");
        return applicationIdStore.registerApplication(name);
    }

    @Override
    public IdGenerator getIdGenerator(String topic) {
        IdBlockAllocator allocator = new StoreBasedIdBlockAllocator(topic, idBlockStore);
        return new BlockAllocatorBasedIdGenerator(allocator);
    }


    @Modified
    public void modified(ComponentContext context) {
        Dictionary<?, ?> properties = context.getProperties();
        Integer sharedThreadPoolSizeConfig =
                getIntegerProperty(properties, "sharedThreadPoolSize");
        if (sharedThreadPoolSizeConfig == null) {
            log.info("Shared Pool Size is not configured, default value is {}",
                    sharedThreadPoolSize);
        } else {
            if (sharedThreadPoolSizeConfig > 0) {
                sharedThreadPoolSize = sharedThreadPoolSizeConfig;
                SharedExecutors.setPoolSize(sharedThreadPoolSize);
                log.info("Configured. Shared Pool Size is configured to {}",
                        sharedThreadPoolSize);
            } else {
                log.warn("Shared Pool Size size must be greater than 0");
            }
        }
    }



    /**
     * Get Integer property from the propertyName
     * Return null if propertyName is not found.
     *
     * @param properties properties to be looked up
     * @param propertyName the name of the property to look up
     * @return value when the propertyName is defined or return null
     */
    private static Integer getIntegerProperty(Dictionary<?, ?> properties,
                                              String propertyName) {
        Integer value = null;
        try {
            String s = (String) properties.get(propertyName);
            value = isNullOrEmpty(s) ? value : Integer.parseInt(s.trim());
        } catch (NumberFormatException | ClassCastException e) {
            value = null;
        }
        return value;
    }


}
