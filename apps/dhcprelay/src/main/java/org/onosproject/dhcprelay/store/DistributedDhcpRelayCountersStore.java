/*
 * Copyright 2017-present Open Networking Foundation
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
package org.onosproject.dhcprelay.store;

import org.onlab.util.KryoNamespace;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.ConsistentMap;
import org.onosproject.store.service.Serializer;
import org.onosproject.store.service.StorageService;
import org.onosproject.store.service.Versioned;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static com.google.common.base.Preconditions.checkNotNull;

@Component(immediate = true, service = DhcpRelayCountersStore.class)
public class DistributedDhcpRelayCountersStore implements DhcpRelayCountersStore {
    private static final KryoNamespace.Builder APP_KYRO = KryoNamespace.newBuilder()
            .register(KryoNamespaces.API)
            .register(DhcpRelayCounters.class);

    private Logger log = LoggerFactory.getLogger(getClass());
    private ConsistentMap<String, DhcpRelayCounters> counters;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected StorageService storageService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected CoreService coreService;


    @Activate
    protected void activated() {
        ApplicationId appId = coreService.getAppId("org.onosproject.Dhcp6HandlerImpl");
        counters = storageService.<String, DhcpRelayCounters>consistentMapBuilder()
                .withSerializer(Serializer.using(APP_KYRO.build()))
                .withName("Dhcp-Relay-Counters")
                .withApplicationId(appId)
                .withPurgeOnUninstall()
                .build();
    }

    @Deactivate
    protected void deactivated() {
        counters.destroy().join();
    }
    @Override
    public void incrementCounter(String coutnerClass, String counterName) {
        DhcpRelayCounters countersRecord;

        Versioned<DhcpRelayCounters> vCounters = counters.get(coutnerClass);
        if (vCounters == null) {
            countersRecord = new DhcpRelayCounters();
        } else {
            countersRecord = vCounters.value();
        }
        countersRecord.incrementCounter(counterName);
        counters.put(coutnerClass, countersRecord);
    }

    @Override
    public Set<Map.Entry<String, DhcpRelayCounters>> getAllCounters() {
        final Set<Map.Entry<String, DhcpRelayCounters>> result =
                new HashSet<Map.Entry<String, DhcpRelayCounters>>();
        Set<Map.Entry<String, Versioned<DhcpRelayCounters>>> tmpCounters = counters.entrySet();
        tmpCounters.forEach(entry -> {
            String key = entry.getKey();
            DhcpRelayCounters value = entry.getValue().value();
            ConcurrentHashMap<String, DhcpRelayCounters> newMap = new ConcurrentHashMap();
            newMap.put(key, value);

            for (Map.Entry m: newMap.entrySet()) {
                result.add(m);
            }
        });
        return  result;
    }
    @Override
    public Optional<DhcpRelayCounters> getCounters(String counterClass) {
        DhcpRelayCounters countersRecord;
        checkNotNull(counterClass, "counter class can't be null");
        Versioned<DhcpRelayCounters> vCounters = counters.get(counterClass);
        if (vCounters == null) {
            return Optional.empty();
        }
        return Optional.of(vCounters.value());
    }
    @Override
    public void resetAllCounters() {
        counters.clear();
    }

    @Override
    public void resetCounters(String counterClass) {
        checkNotNull(counterClass, "counter class can't be null");
        DhcpRelayCounters countersRecord = counters.get(counterClass).value();
        countersRecord.resetCounters();
        counters.put(counterClass, countersRecord);
    }

}
