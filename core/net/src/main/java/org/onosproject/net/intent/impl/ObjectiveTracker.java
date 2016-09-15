/*
 * Copyright 2014-present Open Networking Laboratory
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
package org.onosproject.net.intent.impl;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.SetMultimap;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.Service;
import org.onosproject.event.Event;
import org.onosproject.net.DeviceId;
import org.onosproject.net.ElementId;
import org.onosproject.net.HostId;
import org.onosproject.net.Link;
import org.onosproject.net.LinkKey;
import org.onosproject.net.NetworkResource;
import org.onosproject.net.PortNumber;
import org.onosproject.net.device.DeviceEvent;
import org.onosproject.net.device.DeviceListener;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.host.HostEvent;
import org.onosproject.net.host.HostListener;
import org.onosproject.net.host.HostService;
import org.onosproject.net.intent.Intent;
import org.onosproject.net.intent.IntentData;
import org.onosproject.net.intent.IntentService;
import org.onosproject.net.intent.Key;
import org.onosproject.net.intent.WorkPartitionEvent;
import org.onosproject.net.intent.WorkPartitionEventListener;
import org.onosproject.net.intent.WorkPartitionService;
import org.onosproject.net.link.LinkEvent;
import org.onosproject.net.resource.ResourceEvent;
import org.onosproject.net.resource.ResourceListener;
import org.onosproject.net.resource.ResourceService;
import org.onosproject.net.topology.TopologyEvent;
import org.onosproject.net.topology.TopologyListener;
import org.onosproject.net.topology.TopologyService;
import org.slf4j.Logger;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Multimaps.synchronizedSetMultimap;
import static java.util.concurrent.Executors.newScheduledThreadPool;
import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static org.onlab.util.Tools.groupedThreads;
import static org.onlab.util.Tools.isNullOrEmpty;
import static org.onosproject.net.LinkKey.linkKey;
import static org.onosproject.net.intent.IntentState.INSTALLED;
import static org.onosproject.net.intent.IntentState.INSTALLING;
import static org.onosproject.net.link.LinkEvent.Type.LINK_REMOVED;
import static org.onosproject.net.link.LinkEvent.Type.LINK_UPDATED;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Entity responsible for tracking installed flows and for monitoring topology
 * events to determine what flows are affected by topology changes.
 */
@Component(immediate = true)
@Service
public class ObjectiveTracker implements ObjectiveTrackerService {

    private final Logger log = getLogger(getClass());

    private final SetMultimap<LinkKey, Key> intentsByLink =
            //TODO this could be slow as a point of synchronization
            synchronizedSetMultimap(HashMultimap.<LinkKey, Key>create());

    private final SetMultimap<ElementId, Key> intentsByDevice =
            synchronizedSetMultimap(HashMultimap.<ElementId, Key>create());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected TopologyService topologyService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ResourceService resourceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected HostService hostService;

    @Reference(cardinality = ReferenceCardinality.OPTIONAL_UNARY,
               policy = ReferencePolicy.DYNAMIC)
    protected IntentService intentService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected WorkPartitionService partitionService;

    private ExecutorService executorService =
            newSingleThreadExecutor(groupedThreads("onos/intent", "objectivetracker", log));
    private ScheduledExecutorService executor =
            newScheduledThreadPool(1, groupedThreads("onos/intent", "scheduledIntentUpdate", log));

    private TopologyListener listener = new InternalTopologyListener();
    private ResourceListener resourceListener = new InternalResourceListener();
    private DeviceListener deviceListener = new InternalDeviceListener();
    private HostListener hostListener = new InternalHostListener();
    private WorkPartitionEventListener partitionListener = new InternalPartitionListener();
    private TopologyChangeDelegate delegate;

    protected final AtomicBoolean updateScheduled = new AtomicBoolean(false);

    @Activate
    public void activate() {
        topologyService.addListener(listener);
        resourceService.addListener(resourceListener);
        deviceService.addListener(deviceListener);
        hostService.addListener(hostListener);
        partitionService.addListener(partitionListener);
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        topologyService.removeListener(listener);
        resourceService.removeListener(resourceListener);
        deviceService.removeListener(deviceListener);
        hostService.removeListener(hostListener);
        partitionService.removeListener(partitionListener);
        log.info("Stopped");
    }

    protected void bindIntentService(IntentService service) {
        if (intentService == null) {
            intentService = service;
        }
     }

    protected void unbindIntentService(IntentService service) {
        if (intentService == service) {
            intentService = null;
        }
    }

    @Override
    public void setDelegate(TopologyChangeDelegate delegate) {
        checkNotNull(delegate, "Delegate cannot be null");
        checkArgument(this.delegate == null || this.delegate == delegate,
                      "Another delegate already set");
        this.delegate = delegate;
    }

    @Override
    public void unsetDelegate(TopologyChangeDelegate delegate) {
        checkArgument(this.delegate == delegate, "Not the current delegate");
        this.delegate = null;
    }

    @Override
    public void addTrackedResources(Key intentKey,
                                    Collection<NetworkResource> resources) {
        for (NetworkResource resource : resources) {
            if (resource instanceof Link) {
                intentsByLink.put(linkKey((Link) resource), intentKey);
            } else if (resource instanceof ElementId) {
                intentsByDevice.put((ElementId) resource, intentKey);
            }
        }
    }

    @Override
    public void removeTrackedResources(Key intentKey,
                                       Collection<NetworkResource> resources) {
        for (NetworkResource resource : resources) {
            if (resource instanceof Link) {
                intentsByLink.remove(linkKey((Link) resource), intentKey);
            } else if (resource instanceof ElementId) {
                intentsByDevice.remove(resource, intentKey);
            }
        }
    }

    @Override
    public void trackIntent(IntentData intentData) {

        //NOTE: This will be called for intents that are being added to the store
        //      locally (i.e. every intent update)

        Key key = intentData.key();
        Intent intent = intentData.intent();
        boolean isLocal = intentService.isLocal(key);
        boolean isInstalled = intentData.state() == INSTALLING ||
                              intentData.state() == INSTALLED;
        List<Intent> installables = intentData.installables();

        if (log.isTraceEnabled()) {
            log.trace("intent {}, old: {}, new: {}, installableCount: {}, resourceCount: {}",
                      key,
                      intentsByDevice.values().contains(key),
                      isLocal && isInstalled,
                      installables.size(),
                      intent.resources().size() +
                          installables.stream()
                              .mapToLong(i -> i.resources().size()).sum());
        }

        if (isNullOrEmpty(installables) && intentData.state() == INSTALLED) {
            log.warn("Intent {} is INSTALLED with no installables", key);
        }

        // FIXME Intents will be added 3 times (once directly using addTracked,
        //       then when installing and when installed)
        if (isLocal && isInstalled) {
            addTrackedResources(key, intent.resources());
            for (Intent installable : installables) {
                addTrackedResources(key, installable.resources());
            }
            // FIXME check all resources against current topo service(s); recompile if necessary
        } else {
            removeTrackedResources(key, intent.resources());
            for (Intent installable : installables) {
                removeTrackedResources(key, installable.resources());
            }
        }
    }

    // Internal re-actor to topology change events.
    private class InternalTopologyListener implements TopologyListener {
        @Override
        public void event(TopologyEvent event) {
            executorService.execute(new TopologyChangeHandler(event));
        }
    }

    // Re-dispatcher of topology change events.
    private class TopologyChangeHandler implements Runnable {

        private final TopologyEvent event;

        TopologyChangeHandler(TopologyEvent event) {
            this.event = event;
        }

        @Override
        public void run() {
            // If there is no delegate, why bother? Just bail.
            if (delegate == null) {
                return;
            }

            if (event.reasons() == null || event.reasons().isEmpty()) {
                delegate.triggerCompile(Collections.emptySet(), true);

            } else {
                Set<Key> intentsToRecompile = new HashSet<>();
                boolean dontRecompileAllFailedIntents = true;

                // Scan through the list of reasons and keep accruing all
                // intents that need to be recompiled.
                for (Event reason : event.reasons()) {
                    if (reason instanceof LinkEvent) {
                        LinkEvent linkEvent = (LinkEvent) reason;
                        final LinkKey linkKey = linkKey(linkEvent.subject());
                        synchronized (intentsByLink) {
                            Set<Key> intentKeys = intentsByLink.get(linkKey);
                            log.debug("recompile triggered by LinkEvent {} ({}) for {}",
                                    linkKey, linkEvent.type(), intentKeys);
                            intentsToRecompile.addAll(intentKeys);
                        }
                        dontRecompileAllFailedIntents = dontRecompileAllFailedIntents &&
                                (linkEvent.type() == LINK_REMOVED ||
                                (linkEvent.type() == LINK_UPDATED &&
                                linkEvent.subject().isDurable()));
                    }
                }
                delegate.triggerCompile(intentsToRecompile, !dontRecompileAllFailedIntents);
            }
        }
    }

    private class InternalResourceListener implements ResourceListener {
        @Override
        public void event(ResourceEvent event) {
            if (event.subject().isSubTypeOf(PortNumber.class)) {
                executorService.execute(() -> {
                    if (delegate == null) {
                        return;
                    }

                    delegate.triggerCompile(Collections.emptySet(), true);
                });
            }
        }
    }

    //TODO consider adding flow rule event tracking

    /*
     * Re-dispatcher of device and host events.
     */
    private class DeviceAvailabilityHandler implements Runnable {

        private final ElementId id;
        private final boolean available;

        DeviceAvailabilityHandler(ElementId id, boolean available) {
            this.id = checkNotNull(id);
            this.available = available;
        }

        @Override
        public void run() {
            // If there is no delegate, why bother? Just bail.
            if (delegate == null) {
                return;
            }

            // TODO should we recompile on available==true?

            final ImmutableSet<Key> snapshot;
            synchronized (intentsByDevice) {
                snapshot = ImmutableSet.copyOf(intentsByDevice.get(id));
            }
            delegate.triggerCompile(snapshot, available);
        }
    }


    private class InternalDeviceListener implements DeviceListener {
        @Override
        public void event(DeviceEvent event) {
            DeviceEvent.Type type = event.type();
            switch (type) {
            case DEVICE_ADDED:
            case DEVICE_AVAILABILITY_CHANGED:
            case DEVICE_REMOVED:
            case DEVICE_SUSPENDED:
            case DEVICE_UPDATED:
                DeviceId id = event.subject().id();
                // TODO we need to check whether AVAILABILITY_CHANGED means up or down
                boolean available = (type == DeviceEvent.Type.DEVICE_AVAILABILITY_CHANGED ||
                        type == DeviceEvent.Type.DEVICE_ADDED ||
                        type == DeviceEvent.Type.DEVICE_UPDATED);
                executorService.execute(new DeviceAvailabilityHandler(id, available));
                break;
            case PORT_ADDED:
            case PORT_REMOVED:
            case PORT_UPDATED:
            case PORT_STATS_UPDATED:
            default:
                // Don't handle port events for now
                break;
            }
        }
    }

    private class InternalHostListener implements HostListener {
        @Override
        public void event(HostEvent event) {
            HostId id = event.subject().id();
            switch (event.type()) {
                case HOST_ADDED:
                case HOST_MOVED:
                case HOST_REMOVED:
                    executorService.execute(new DeviceAvailabilityHandler(id, false));
                    break;
                case HOST_UPDATED:
                default:
                    // DO NOTHING
                    break;
            }
        }
    }

    private void doIntentUpdate() {
        updateScheduled.set(false);
        if (intentService == null) {
            log.warn("Intent service is not bound yet");
            return;
        }
        try {
            //FIXME very inefficient
            for (IntentData intentData : intentService.getIntentData()) {
                try {
                    trackIntent(intentData);
                } catch (NullPointerException npe) {
                    log.warn("intent error {}", intentData.key(), npe);
                }
            }
        } catch (Exception e) {
            log.warn("Exception caught during update task", e);
        }
    }

    private void scheduleIntentUpdate(int afterDelaySec) {
        if (updateScheduled.compareAndSet(false, true)) {
            executor.schedule(this::doIntentUpdate, afterDelaySec, TimeUnit.SECONDS);
        }
    }

    private final class InternalPartitionListener implements WorkPartitionEventListener {
        @Override
        public void event(WorkPartitionEvent event) {
            log.debug("got message {}", event.subject());
            scheduleIntentUpdate(1);
        }
    }
}
