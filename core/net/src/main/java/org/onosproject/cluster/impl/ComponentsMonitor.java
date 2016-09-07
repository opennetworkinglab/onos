/*
 * Copyright 2016-present Open Networking Laboratory
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

package org.onosproject.cluster.impl;

import org.apache.felix.scr.Component;
import org.apache.felix.scr.ScrService;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.karaf.features.Feature;
import org.apache.karaf.features.FeaturesService;
import org.onlab.util.SharedScheduledExecutors;
import org.onosproject.cluster.ClusterAdminService;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Monitors the system to make sure that all bundles and their components
 * are properly activated and keeps the cluster node service appropriately
 * updated.
 */
@org.apache.felix.scr.annotations.Component(immediate = true)
public class ComponentsMonitor {

    private Logger log = LoggerFactory.getLogger(getClass());

    private static final long PERIOD = 2500;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected FeaturesService featuresService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ScrService scrService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ClusterAdminService clusterAdminService;

    private BundleContext bundleContext;
    private ScheduledFuture<?> poller;

    @Activate
    protected void activate(ComponentContext context) {
        bundleContext = context.getBundleContext();
        poller = SharedScheduledExecutors.getSingleThreadExecutor()
                .scheduleAtFixedRate(this::checkStartedState, PERIOD,
                                     PERIOD, TimeUnit.MILLISECONDS);
        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        poller.cancel(false);
        log.info("Stopped");
    }

    private void checkStartedState() {
        clusterAdminService.markFullyStarted(isFullyStarted());
    }

    /**
     * Scans the system to make sure that all bundles and their components
     * are fully started.
     *
     * @return true if all bundles and their components are active
     */
    private boolean isFullyStarted() {
        for (Feature feature : featuresService.listInstalledFeatures()) {
            if (!isFullyStarted(feature)) {
                return false;
            }
        }
        return true;
    }

    private boolean isFullyStarted(Feature feature) {
        try {
            return feature.getBundles().stream()
                    .map(info -> bundleContext.getBundle(info.getLocation()))
                    .allMatch(this::isFullyStarted);
        } catch (NullPointerException npe) {
            // FIXME: Remove this catch block when Felix fixes the bug
            // Due to a bug in the Felix implementation, this can throw an NPE.
            // Catch the error and do something sensible with it.
            return false;
        }
    }

    private boolean isFullyStarted(Bundle bundle) {
        Component[] components = scrService.getComponents(bundle);
        if (components != null) {
            for (Component component : components) {
                if (!isFullyStarted(component)) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean isFullyStarted(Component component) {
        int state = component.getState();
        return state == Component.STATE_ACTIVE || state == Component.STATE_DISABLED ||
                (state == Component.STATE_REGISTERED && !component.isImmediate());
    }

}
