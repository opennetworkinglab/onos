/*
 * Copyright 2014-present Open Networking Foundation
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

import org.onlab.metrics.MetricsService;
import org.onlab.util.SharedExecutors;
import org.onlab.util.SharedScheduledExecutors;
import org.onlab.util.Tools;
import org.onosproject.app.ApplicationIdStore;
import org.onosproject.app.ApplicationService;
import org.onosproject.cfg.ComponentConfigService;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.core.IdBlockStore;
import org.onosproject.core.IdGenerator;
import org.onosproject.core.Version;
import org.onosproject.core.VersionService;
import org.onosproject.event.EventDeliveryService;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Dictionary;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onosproject.net.OsgiPropertyConstants.CALCULATE_PERFORMANCE_CHECK;
import static org.onosproject.net.OsgiPropertyConstants.CALCULATE_PERFORMANCE_CHECK_DEFAULT;
import static org.onosproject.net.OsgiPropertyConstants.MAX_EVENT_TIME_LIMIT;
import static org.onosproject.net.OsgiPropertyConstants.MAX_EVENT_TIME_LIMIT_DEFAULT;
import static org.onosproject.net.OsgiPropertyConstants.SHARED_THREAD_POOL_SIZE;
import static org.onosproject.net.OsgiPropertyConstants.SHARED_THREAD_POOL_SIZE_DEFAULT;
import static org.onosproject.security.AppGuard.checkPermission;
import static org.onosproject.security.AppPermission.Type.APP_READ;
import static org.onosproject.security.AppPermission.Type.APP_WRITE;

/**
 * Core service implementation.
 */
@Component(
        immediate = true,
        service = CoreService.class,
        property = {
                SHARED_THREAD_POOL_SIZE + ":Integer=" + SHARED_THREAD_POOL_SIZE_DEFAULT,
                MAX_EVENT_TIME_LIMIT + ":Integer=" + MAX_EVENT_TIME_LIMIT_DEFAULT,
                CALCULATE_PERFORMANCE_CHECK + ":Boolean=" + CALCULATE_PERFORMANCE_CHECK_DEFAULT
        }
)
public class CoreManager implements CoreService {

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected VersionService versionService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected ApplicationIdStore applicationIdStore;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected IdBlockStore idBlockStore;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected ApplicationService appService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected ComponentConfigService cfgService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected EventDeliveryService eventDeliveryService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected MetricsService metricsService;

    /** Configure shared pool maximum size. */
    private int sharedThreadPoolSize = SHARED_THREAD_POOL_SIZE_DEFAULT;

    /** Maximum number of millis an event sink has to process an event. */
    private int maxEventTimeLimit = MAX_EVENT_TIME_LIMIT_DEFAULT;

    /** Enable queue performance check on shared pool. */
    private boolean sharedThreadPerformanceCheck = CALCULATE_PERFORMANCE_CHECK_DEFAULT;


    @Activate
    protected void activate() {
        registerApplication(CORE_APP_NAME);
        cfgService.registerProperties(getClass());
        log.info("ONOS starting up on Java version {}, JVM version {}",
            System.getProperty("java.version"),
            System.getProperty("java.vm.version"));
    }

    @Deactivate
    protected void deactivate() {
        cfgService.unregisterProperties(getClass(), false);
        SharedExecutors.shutdown();
        SharedScheduledExecutors.shutdown();
    }

    @Override
    public Version version() {
        checkPermission(APP_READ);
        return versionService.version();
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
        checkPermission(APP_WRITE);
        checkNotNull(name, "Application ID cannot be null");
        return applicationIdStore.registerApplication(name);
    }

    @Override
    public ApplicationId registerApplication(String name, Runnable preDeactivate) {
        checkPermission(APP_WRITE);
        ApplicationId id = registerApplication(name);
        appService.registerDeactivateHook(id, preDeactivate);
        return id;
    }

    @Override
    public IdGenerator getIdGenerator(String topic) {
        checkPermission(APP_READ);
        IdBlockAllocator allocator = new StoreBasedIdBlockAllocator(topic, idBlockStore);
        return new BlockAllocatorBasedIdGenerator(allocator);
    }

    @Modified
    protected void modified(ComponentContext context) {
        Dictionary<?, ?> properties = context.getProperties();
        Integer poolSize = Tools.getIntegerProperty(properties, SHARED_THREAD_POOL_SIZE);

        if (poolSize != null && poolSize > 1) {
            sharedThreadPoolSize = poolSize;
            SharedExecutors.setPoolSize(sharedThreadPoolSize);
        } else if (poolSize != null) {
            log.warn("sharedThreadPoolSize must be greater than 1");
        }

        Integer timeLimit = Tools.getIntegerProperty(properties, MAX_EVENT_TIME_LIMIT);
        if (timeLimit != null && timeLimit >= 0) {
            maxEventTimeLimit = timeLimit;
            eventDeliveryService.setDispatchTimeLimit(maxEventTimeLimit);
        } else if (timeLimit != null) {
            log.warn("maxEventTimeLimit must be greater than or equal to 0");
        }

        Boolean performanceCheck = Tools.isPropertyEnabled(properties, CALCULATE_PERFORMANCE_CHECK);
        if (performanceCheck != null) {
            sharedThreadPerformanceCheck = performanceCheck;
            SharedExecutors.setMetricsService(sharedThreadPerformanceCheck ? metricsService : null);
        }

        log.info("Settings: sharedThreadPoolSize={}, maxEventTimeLimit={}, sharedThreadPerformanceCheck={}",
                 sharedThreadPoolSize, maxEventTimeLimit, sharedThreadPerformanceCheck);
    }
}
