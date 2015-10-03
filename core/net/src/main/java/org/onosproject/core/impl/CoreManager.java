/*
 * Copyright 2014-2015 Open Networking Laboratory
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
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.util.SharedExecutors;
import org.onosproject.cfg.ComponentConfigService;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.ApplicationIdStore;
import org.onosproject.core.CoreService;
import org.onosproject.core.IdBlockStore;
import org.onosproject.core.IdGenerator;
import org.onosproject.core.Version;
import org.onosproject.event.EventDeliveryService;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Dictionary;
import java.util.List;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Strings.isNullOrEmpty;
import static org.onosproject.security.AppGuard.checkPermission;
import static org.onosproject.security.AppPermission.Type.*;



/**
 * Core service implementation.
 */
@Component(immediate = true)
@Service
public class CoreManager implements CoreService {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private static final File VERSION_FILE = new File("../VERSION");
    private static Version version = Version.version("1.4.0-SNAPSHOT");

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ApplicationIdStore applicationIdStore;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected IdBlockStore idBlockStore;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ComponentConfigService cfgService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected EventDeliveryService eventDeliveryService;

    private static final int DEFAULT_POOL_SIZE = 30;
    @Property(name = "sharedThreadPoolSize", intValue = DEFAULT_POOL_SIZE,
            label = "Configure shared pool maximum size ")
    private int sharedThreadPoolSize = DEFAULT_POOL_SIZE;

    private static final int DEFAULT_EVENT_TIME = 2000;
    @Property(name = "maxEventTimeLimit", intValue = DEFAULT_EVENT_TIME,
            label = "Maximum number of millis an event sink has to process an event")
    private int maxEventTimeLimit = DEFAULT_EVENT_TIME;

    @Activate
    public void activate() {
        registerApplication(CORE_APP_NAME);
        cfgService.registerProperties(getClass());
        try {
            Path path = Paths.get(VERSION_FILE.getPath());
            List<String> versionLines = Files.readAllLines(path);
            if (versionLines != null && !versionLines.isEmpty()) {
                version = Version.version(versionLines.get(0));
            }
        } catch (IOException e) {
            // version file not found, using default
            log.trace("Version file not found", e);
        }
    }

    @Deactivate
    public void deactivate() {
        cfgService.unregisterProperties(getClass(), false);
        SharedExecutors.shutdown();
    }

    @Override
    public Version version() {
        checkPermission(APP_READ);

        return version;
    }

    @Override
    public Set<ApplicationId> getAppIds() {
        checkPermission(APP_READ);

        return applicationIdStore.getAppIds();
    }

    @Override
    public ApplicationId getAppId(Short id) {
        checkPermission(APP_READ);

        return applicationIdStore.getAppId(id);
    }

    @Override
    public ApplicationId getAppId(String name) {
        checkPermission(APP_READ);

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
        Integer poolSize = getIntegerProperty(properties, "sharedThreadPoolSize");

        if (poolSize != null && poolSize > 1) {
            sharedThreadPoolSize = poolSize;
            SharedExecutors.setPoolSize(sharedThreadPoolSize);
        } else if (poolSize != null) {
            log.warn("sharedThreadPoolSize must be greater than 1");
        }

        Integer timeLimit = getIntegerProperty(properties, "maxEventTimeLimit");
        if (timeLimit != null && timeLimit > 1) {
            maxEventTimeLimit = timeLimit;
            eventDeliveryService.setDispatchTimeLimit(maxEventTimeLimit);
        } else if (timeLimit != null) {
            log.warn("maxEventTimeLimit must be greater than 1");
        }

        log.info("Settings: sharedThreadPoolSize={}, maxEventTimeLimit={}",
                 sharedThreadPoolSize, maxEventTimeLimit);
    }


    /**
     * Get Integer property from the propertyName
     * Return null if propertyName is not found.
     *
     * @param properties   properties to be looked up
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
