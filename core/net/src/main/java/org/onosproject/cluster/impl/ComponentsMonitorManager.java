/*
 * Copyright 2016-present Open Networking Foundation
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

import com.google.common.collect.Lists;
import org.osgi.service.component.runtime.ServiceComponentRuntime;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.apache.karaf.features.Feature;
import org.apache.karaf.features.FeaturesService;
import org.onosproject.cluster.ClusterAdminService;
import org.onosproject.cluster.ComponentsMonitorService;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.runtime.dto.ComponentConfigurationDTO;
import org.osgi.service.component.runtime.dto.ComponentDescriptionDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static org.onlab.util.Tools.groupedThreads;

/**
 * Monitors the system to make sure that all bundles and their components
 * are properly activated and keeps the cluster node service appropriately
 * updated.
 */
@Component(immediate = true, service = { ComponentsMonitorService.class })
public class ComponentsMonitorManager implements ComponentsMonitorService {

    private Logger log = LoggerFactory.getLogger(getClass());

    private static final long STARTUP_PERIOD = 2500;
    private static final long ACTIVE_PERIOD = 60000;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected FeaturesService featuresService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected ServiceComponentRuntime scrService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected ClusterAdminService clusterAdminService;

    private BundleContext bundleContext;

    private ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(
            groupedThreads("components-monitor", "%d", log));
    private ScheduledFuture<?> poller;
    private boolean pollerBackedOff;

    @Activate
    protected void activate(ComponentContext context) {
        bundleContext = context.getBundleContext();
        poller = executor.scheduleAtFixedRate(
                this::checkStartedState, STARTUP_PERIOD, STARTUP_PERIOD, TimeUnit.MILLISECONDS);
        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        poller.cancel(false);
        executor.shutdownNow();
        log.info("Stopped");
    }

    /**
     * Increases the rate at which {@link #checkStartedState()} is called once the node has been fully started.
     */
    private void backoffPoller() {
        if (!pollerBackedOff) {
            poller.cancel(false);
            poller = executor.scheduleAtFixedRate(
                    this::checkStartedState, ACTIVE_PERIOD, ACTIVE_PERIOD, TimeUnit.MILLISECONDS);
            pollerBackedOff = true;
        }
    }

    /**
     * Decreases the rate at which {@link #checkStartedState()} is called when the node becomes unready.
     */
    private void revertPoller() {
        if (pollerBackedOff) {
            poller.cancel(false);
            poller = executor.scheduleAtFixedRate(
                    this::checkStartedState, STARTUP_PERIOD, STARTUP_PERIOD, TimeUnit.MILLISECONDS);
            pollerBackedOff = false;
        }
    }

    /**
     * Checks whether all components are active and marks the node READY if so.
     */
    private void checkStartedState() {
        boolean isFullyStarted = isFullyStarted();
        clusterAdminService.markFullyStarted(isFullyStarted);

        // If the node is fully started, decrease the rate at which we poll component states.
        // Otherwise, increase the rate at which we poll component states until the node becomes ready.
        if (isFullyStarted) {
            backoffPoller();
        } else {
            revertPoller();
        }
    }

    /**
     * Scans the system to make sure that all bundles and their components
     * are fully started.
     *
     * @return true if all bundles and their components are active
     */
    private boolean isFullyStarted() {
        try {
            for (Feature feature : featuresService.listInstalledFeatures()) {
                if (needToCheck(feature) && !isFullyStarted(feature)) {
                    return false;
                }
            }
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    private boolean needToCheck(Feature feature) {
        // We only need to check core ONOS features, not external ones.
        return feature.getId().startsWith("onos-") &&
               !feature.getId().contains("thirdparty");
    }

    @Override
    public boolean isFullyStarted(List<String> featureStrings) {
        List<Feature> features = Lists.newArrayList();
        for (String featureString : featureStrings) {
            try {
                features.add(featuresService.getFeature(featureString));
            } catch (Exception e) {
                log.debug("Feature {} not found", featureString);
                return false;
            }
        }
        return features.stream().allMatch(this::isFullyStarted);
    }

    private boolean isFullyStarted(Feature feature) {
        try {
            return feature.getBundles().stream()
                .map(info -> bundleContext.getBundle(info.getLocation()))
                .allMatch(bundle -> bundle != null && isFullyStarted(bundle));
        } catch (NullPointerException npe) {
            // FIXME: Remove this catch block when Felix fixes the bug
            // Due to a bug in the Felix implementation, this can throw an NPE.
            // Catch the error and do something sensible with it.
            return false;
        }
    }

    private boolean isFullyStarted(Bundle bundle) {
        for (ComponentDescriptionDTO component : scrService.getComponentDescriptionDTOs(bundle)) {
            if (scrService.isComponentEnabled(component)) {
                for (ComponentConfigurationDTO config : scrService.getComponentConfigurationDTOs(component)) {
                    if (config.state != ComponentConfigurationDTO.ACTIVE) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

}
