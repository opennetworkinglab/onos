/*
 * Copyright 2015-present Open Networking Foundation
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

package org.onosproject.component.impl;

import org.onosproject.component.ComponentService;
import org.onosproject.core.ApplicationId;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.runtime.ServiceComponentRuntime;
import org.osgi.service.component.runtime.dto.ComponentDescriptionDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static java.util.concurrent.Executors.newScheduledThreadPool;
import static org.onlab.util.Tools.groupedThreads;

/**
 * Manages OSGi components.
 */
@Component(immediate = true, service = ComponentService.class)
public class ComponentManager implements ComponentService {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private static final long TIMEOUT = 3000;
    private static final int POLLING_PERIOD_MS = 500;
    private static final int NUM_THREADS = 1;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected ServiceComponentRuntime scrService;

    private Set<String> components;

    private ScheduledExecutorService executor;

    @Activate
    private void activate() {
        components = Collections.newSetFromMap(new ConcurrentHashMap<>());

        executor = newScheduledThreadPool(NUM_THREADS, groupedThreads("onos/component", "%d", log));
        executor.scheduleAtFixedRate(() -> components.forEach(this::enableComponent),
                                     0, POLLING_PERIOD_MS, TimeUnit.MILLISECONDS);
        log.info("Started");
    }

    @Deactivate
    private void deactivate() {
        executor.shutdownNow();
        log.info("Stopped");
    }

    @Override
    public void activate(ApplicationId appId, String name) {
        components.add(name);
        enableComponent(name);
    }

    @Override
    public void deactivate(ApplicationId appId, String name) {
        components.remove(name);
        disableComponent(name);
    }

    private ComponentDescriptionDTO getComponent(String name) {
        for (ComponentDescriptionDTO component : scrService.getComponentDescriptionDTOs()) {
            if (component.name.equals(name)) {
                return component;
            }
        }
        return null;
    }

    private void enableComponent(String name) {
        ComponentDescriptionDTO component = getComponent(name);
        if (component != null && !scrService.isComponentEnabled(component)) {
            log.info("Enabling component {}", name);
            try {
                scrService.enableComponent(component).timeout(TIMEOUT);
            } catch (Exception e) {
                throw new IllegalStateException("Unable to start component " + name, e);
            }
        }
    }

    private void disableComponent(String name) {
        ComponentDescriptionDTO component = getComponent(name);
        if (component != null && scrService.isComponentEnabled(component)) {
            log.info("Disabling component {}", name);
            try {
                scrService.disableComponent(component).timeout(TIMEOUT);
            } catch (Exception e) {
                throw new IllegalStateException("Unable to start component " + name, e);
            }
        }
    }
}
