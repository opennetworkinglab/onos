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


package org.onosproject.clusterha;


import org.onlab.util.KryoNamespace;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.ConsistentMap;
import org.onosproject.store.service.Serializer;
import org.onosproject.store.service.StorageException;
import org.onosproject.store.service.StorageService;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;


/**
 * Cluster HA Test.
 */
@Component(immediate = true, service = ClusterHATest.class)
public class ClusterHATest {

    protected final Logger log = LoggerFactory.getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected StorageService storageService;

    private ApplicationId appId;
    private List<ConsistentMap<String, String>> stringStoreList;
    private int numStores = 10;

    @Activate
    protected void activate() {
        appId = coreService.registerApplication("org.onosproject.cluster");
        stringStoreList = new ArrayList<ConsistentMap<String, String>>();
        for (int i = 0; i < numStores; i++) {
            ConsistentMap<String, String> stringStore = storageService.<String, String>consistentMapBuilder()
                    .withName("cluster-ha-store" + i)
                    .withApplicationId(appId)
                    .withSerializer(Serializer.using(new KryoNamespace.Builder()
                                                             .register(KryoNamespaces.API)
                                                             .register(String.class)
                                                             .build("string")))
                    .build();
            stringStoreList.add(stringStore);
        }
        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        for (ConsistentMap<String, String> stringStore: stringStoreList) {
            stringStore.clear();
        }
        log.info("Stopped");
    }

    public void addToStringStore(int key, String str) {
        try {
            for (ConsistentMap<String, String> stringStore: stringStoreList) {
                stringStore.put(String.valueOf(key), str);
            }
        } catch (StorageException e) {
            log.error("StringStore - put got exception {} , for the key {} string {}", e, key, str);
        }
    }

    public void removeStringFromStore(int key) {
        try {
            for (ConsistentMap<String, String> stringStore: stringStoreList) {
                stringStore.remove(String.valueOf(key));
            }
        } catch (StorageException e) {
            log.error("StringStore - put got exception {} , for the key {}", e, key);
        }
    }
}