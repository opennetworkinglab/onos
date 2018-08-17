/*
 * Copyright 2018-present Open Networking Foundation
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
package org.onosproject.store.host.impl;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalNotification;
import org.onlab.packet.MacAddress;
import org.onlab.util.KryoNamespace;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.Host;
import org.onosproject.net.host.HostProbe;
import org.onosproject.net.host.HostProbeStore;
import org.onosproject.net.host.HostProbingEvent;
import org.onosproject.net.host.HostProbingStoreDelegate;
import org.onosproject.net.host.ProbeMode;
import org.onosproject.store.AbstractStore;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.AtomicCounter;
import org.onosproject.store.service.ConsistentMap;
import org.onosproject.store.service.MapEvent;
import org.onosproject.store.service.MapEventListener;
import org.onosproject.store.service.Serializer;
import org.onosproject.store.service.StorageService;
import org.onosproject.store.service.Versioned;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;

import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;
import static org.onlab.util.Tools.groupedThreads;
import static org.slf4j.LoggerFactory.getLogger;

@Component(immediate = true, service = HostProbeStore.class)
public class DefaultHostProbeStore extends AbstractStore<HostProbingEvent, HostProbingStoreDelegate>
        implements HostProbeStore {
    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected StorageService storageService;

    private final Logger log = getLogger(getClass());

    // TODO make this configurable
    private static final int PROBE_TIMEOUT_MS = 3000;

    private AtomicCounter hostProbeIndex;
    private Cache<MacAddress, HostProbe> probingHostsCache;
    private ConsistentMap<MacAddress, HostProbe> probingHostsConsistentMap;
    private Map<MacAddress, HostProbe> probingHosts;
    private MapEventListener<MacAddress, HostProbe> probingHostListener = new ProbingHostListener();
    private ScheduledExecutorService cacheCleaner;
    private ScheduledExecutorService locationRemover;

    @Activate
    public void activate() {
        KryoNamespace.Builder pendingHostSerializer = KryoNamespace.newBuilder()
                .register(KryoNamespaces.API)
                .register(DefaultHostProbe.class)
                .register(ProbeMode.class);
        probingHostsConsistentMap = storageService.<MacAddress, HostProbe>consistentMapBuilder()
                .withName("onos-hosts-pending")
                .withRelaxedReadConsistency()
                .withSerializer(Serializer.using(pendingHostSerializer.build()))
                .build();
        probingHostsConsistentMap.addListener(probingHostListener);
        probingHosts = probingHostsConsistentMap.asJavaMap();

        hostProbeIndex = storageService.atomicCounterBuilder()
                .withName("onos-hosts-probe-index")
                .build()
                .asAtomicCounter();

        probingHostsCache = CacheBuilder.newBuilder()
                .expireAfterWrite(PROBE_TIMEOUT_MS, TimeUnit.MILLISECONDS)
                .removalListener((RemovalNotification<MacAddress, HostProbe> notification) -> {
                    MacAddress probeMac = notification.getKey();
                    switch (notification.getCause()) {
                        case EXPIRED:
                        case REPLACED:
                            probingHosts.computeIfPresent(probeMac, (k, v) -> {
                                v.decreaseRetry();
                                return v;
                            });
                            break;
                        case EXPLICIT:
                            break;
                        default:
                            log.warn("Remove {} from pendingHostLocations for unexpected reason {}",
                                    notification.getKey(), notification.getCause());
                    }
                }).build();

        cacheCleaner = newSingleThreadScheduledExecutor(
                groupedThreads("onos/host/hostprobestore", "cache-cleaner", log));
        cacheCleaner.scheduleAtFixedRate(probingHostsCache::cleanUp, 0, PROBE_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        locationRemover = newSingleThreadScheduledExecutor(
                groupedThreads("onos/host/hostprobestore", "loc-remover", log));

        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        cacheCleaner.shutdown();
        locationRemover.shutdown();
        probingHostsCache.cleanUp();

        log.info("Stopped");
    }

    @Override
    public MacAddress addProbingHost(Host host, ConnectPoint connectPoint, ProbeMode probeMode,
                                     MacAddress probeMac, int retry) {
        if (probeMac == null) {
            probeMac = generateProbeMac();
        }
        DefaultHostProbe probingHost = new DefaultHostProbe(host, connectPoint, probeMode, probeMac, retry);
        probingHostsCache.put(probeMac, probingHost);
        probingHosts.put(probeMac, probingHost);
        return probeMac;
    }

    @Override
    public void removeProbingHost(MacAddress probeMac) {
        probingHostsCache.invalidate(probeMac);
        probingHosts.remove(probeMac);
    }

    private MacAddress generateProbeMac() {
        // Use ONLab OUI (3 bytes) + atomic counter (3 bytes) as the source MAC of the probe
        long nextIndex = hostProbeIndex.incrementAndGet();
        return MacAddress.valueOf(MacAddress.NONE.toLong() + nextIndex);
    }

    private class ProbingHostListener implements MapEventListener<MacAddress, HostProbe> {
        @Override
        public void event(MapEvent<MacAddress, HostProbe> event) {
            HostProbe newValue = Versioned.valueOrNull(event.newValue());
            HostProbe oldValue = Versioned.valueOrNull(event.oldValue());

            HostProbingEvent hostProbingEvent;
            switch (event.type()) {
                case INSERT:
                    hostProbingEvent = new HostProbingEvent(HostProbingEvent.Type.PROBE_REQUESTED, newValue);
                    notifyDelegate(hostProbingEvent);
                    break;
                case UPDATE:
                    // Fail VERIFY probe immediately. Only allow DISCOVER probe to retry.
                    if (newValue.retry() > 0) {
                        if (newValue.mode() == ProbeMode.DISCOVER) {
                            hostProbingEvent = new HostProbingEvent(HostProbingEvent.Type.PROBE_TIMEOUT,
                                    newValue, oldValue);
                            notifyDelegate(hostProbingEvent);
                        } else {
                            hostProbingEvent = new HostProbingEvent(HostProbingEvent.Type.PROBE_FAIL,
                                    newValue, oldValue);
                            notifyDelegate(hostProbingEvent);
                        }
                    } else {
                        // Remove from pendingHost and let the remove listener generates the event
                        locationRemover.execute(() -> probingHosts.remove(event.key()));
                    }
                    break;
                case REMOVE:
                    if (oldValue.retry() > 0) {
                        hostProbingEvent = new HostProbingEvent(HostProbingEvent.Type.PROBE_COMPLETED, oldValue);
                        notifyDelegate(hostProbingEvent);
                    } else {
                        hostProbingEvent = new HostProbingEvent(HostProbingEvent.Type.PROBE_FAIL, oldValue);
                        notifyDelegate(hostProbingEvent);
                    }
                    break;
                default:
                    log.warn("Unknown map event type: {}", event.type());
            }
        }
    }
}
