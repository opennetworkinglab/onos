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

package org.onosproject.incubator.component.impl;

import org.apache.felix.scr.ScrService;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onosproject.core.ApplicationId;
import org.onosproject.incubator.component.ComponentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.onlab.util.Tools.groupedThreads;

/**
 * Manages OSGi components.
 */
@Service
@Component(immediate = true)
public class ComponentManager implements ComponentService {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private static final int POLLING_PERIOD_MS = 500;

    private static final int NUM_THREADS = 1;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ScrService scrService;

    private Set<String> components;

    private ScheduledExecutorService executor;

    @Activate
    private void activate() {
        components = Collections.newSetFromMap(new ConcurrentHashMap<>());

        executor = Executors.newScheduledThreadPool(NUM_THREADS,
                groupedThreads("onos/component", "%d", log));

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

    private void enableComponent(String name) {
        org.apache.felix.scr.Component[] components = scrService.getComponents(name);

        if (components == null || components.length == 0) {
            return;
        }

        org.apache.felix.scr.Component component = components[0];

        if (component.getState() == org.apache.felix.scr.Component.STATE_DISABLED) {
            log.info("Enabling component {}", name);
            component.enable();
        }
    }

    private void disableComponent(String name) {
        org.apache.felix.scr.Component[] components = scrService.getComponents(name);

        if (components == null || components.length == 0) {
            return;
        }

        log.info("Disabling component {}", name);

        components[0].disable();
    }
}
