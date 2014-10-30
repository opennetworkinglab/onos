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
package org.onlab.onos.net.intent.impl;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.onos.event.Event;
import org.onlab.onos.net.Link;
import org.onlab.onos.net.LinkKey;
import org.onlab.onos.net.NetworkResource;
import org.onlab.onos.net.intent.IntentId;
import org.onlab.onos.net.link.LinkEvent;
import org.onlab.onos.net.topology.TopologyEvent;
import org.onlab.onos.net.topology.TopologyListener;
import org.onlab.onos.net.topology.TopologyService;
import org.slf4j.Logger;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Multimaps.synchronizedSetMultimap;
import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static org.onlab.onos.net.LinkKey.linkKey;
import static org.onlab.onos.net.link.LinkEvent.Type.LINK_REMOVED;
import static org.onlab.util.Tools.namedThreads;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Entity responsible for tracking installed flows and for monitoring topology
 * events to determine what flows are affected by topology changes.
 */
@Component
@Service
public class ObjectiveTracker implements ObjectiveTrackerService {

    private final Logger log = getLogger(getClass());

    private final SetMultimap<LinkKey, IntentId> intentsByLink =
            synchronizedSetMultimap(HashMultimap.<LinkKey, IntentId>create());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected TopologyService topologyService;

    private ExecutorService executorService =
            newSingleThreadExecutor(namedThreads("onos-flowtracker"));

    private TopologyListener listener = new InternalTopologyListener();
    private TopologyChangeDelegate delegate;

    @Activate
    public void activate() {
        topologyService.addListener(listener);
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        topologyService.removeListener(listener);
        log.info("Stopped");
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
    public void addTrackedResources(IntentId intentId,
                                    Collection<NetworkResource> resources) {
        for (NetworkResource resource : resources) {
            if (resource instanceof Link) {
                intentsByLink.put(linkKey((Link) resource), intentId);
            }
        }
    }

    @Override
    public void removeTrackedResources(IntentId intentId,
                                       Collection<NetworkResource> resources) {
        for (NetworkResource resource : resources) {
            if (resource instanceof Link) {
                intentsByLink.remove(linkKey((Link) resource), intentId);
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

            if (event.reasons() == null) {
                delegate.triggerCompile(new HashSet<IntentId>(), true);

            } else {
                Set<IntentId> toBeRecompiled = new HashSet<>();
                boolean recompileOnly = true;

                // Scan through the list of reasons and keep accruing all
                // intents that need to be recompiled.
                for (Event reason : event.reasons()) {
                    if (reason instanceof LinkEvent) {
                        LinkEvent linkEvent = (LinkEvent) reason;
                        if (linkEvent.type() == LINK_REMOVED) {
                            final LinkKey linkKey = linkKey(linkEvent.subject());
                            Set<IntentId> intentIds = intentsByLink.get(linkKey);
                            log.debug("recompile triggered by LinkDown {} {}", linkKey, intentIds);
                            toBeRecompiled.addAll(intentIds);
                        }
                        recompileOnly = recompileOnly && linkEvent.type() == LINK_REMOVED;
                    }
                }

                delegate.triggerCompile(toBeRecompiled, !recompileOnly);
            }
        }
    }

}
