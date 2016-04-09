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
package org.onosproject.vtnrsc.classifier.impl;

import static org.slf4j.LoggerFactory.getLogger;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onosproject.net.DeviceId;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.DistributedSet;
import org.onosproject.store.service.Serializer;
import org.onosproject.store.service.StorageService;
import org.onosproject.vtnrsc.classifier.ClassifierService;
import org.slf4j.Logger;

import com.google.common.collect.ImmutableList;

/**
 * Provides implementation of the Classifier Service.
 */
@Component(immediate = true)
@Service
public class ClassifierManager implements ClassifierService {

    private final Logger log = getLogger(ClassifierManager.class);

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected StorageService storageService;

    private DistributedSet<DeviceId> classifierList;

    @Activate
    protected void activate() {
        classifierList = storageService.<DeviceId>setBuilder()
                .withName("classifier")
                .withSerializer(Serializer.using(KryoNamespaces.API))
                .build()
                .asDistributedSet();
        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        log.info("Stopped");
    }

    @Override
    public void addClassifier(DeviceId deviceId) {
        classifierList.add(deviceId);
    }

    @Override
    public Iterable<DeviceId> getClassifiers() {
        return ImmutableList.copyOf(classifierList);
    }

    @Override
    public void removeClassifier(DeviceId deviceId) {
        classifierList.remove(deviceId);
    }
}
