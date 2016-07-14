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

package org.onosproject.bmv2.ctl;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.util.KryoNamespace;
import org.onosproject.bmv2.api.context.Bmv2FlowRuleTranslator;
import org.onosproject.bmv2.api.runtime.Bmv2ExactMatchParam;
import org.onosproject.bmv2.api.runtime.Bmv2FlowRuleWrapper;
import org.onosproject.bmv2.api.runtime.Bmv2LpmMatchParam;
import org.onosproject.bmv2.api.runtime.Bmv2MatchKey;
import org.onosproject.bmv2.api.runtime.Bmv2TableEntryReference;
import org.onosproject.bmv2.api.runtime.Bmv2TernaryMatchParam;
import org.onosproject.bmv2.api.runtime.Bmv2ValidMatchParam;
import org.onosproject.bmv2.api.service.Bmv2Controller;
import org.onosproject.bmv2.api.service.Bmv2DeviceContextService;
import org.onosproject.bmv2.api.service.Bmv2TableEntryService;
import org.onosproject.net.DeviceId;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.EventuallyConsistentMap;
import org.onosproject.store.service.StorageService;
import org.onosproject.store.service.WallClockTimestamp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Implementation of the Bmv2TableEntryService.
 */
@Component(immediate = true)
@Service
public class Bmv2TableEntryServiceImpl implements Bmv2TableEntryService {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private final Bmv2FlowRuleTranslator translator = new Bmv2FlowRuleTranslatorImpl();

    private EventuallyConsistentMap<Bmv2TableEntryReference, Bmv2FlowRuleWrapper> flowRules;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected StorageService storageService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected Bmv2Controller controller;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected Bmv2DeviceContextService contextService;


    @Activate
    public void activate() {
        KryoNamespace kryo = new KryoNamespace.Builder()
                .register(KryoNamespaces.API)
                .register(Bmv2TableEntryReference.class)
                .register(Bmv2MatchKey.class)
                .register(Bmv2ExactMatchParam.class)
                .register(Bmv2TernaryMatchParam.class)
                .register(Bmv2LpmMatchParam.class)
                .register(Bmv2ValidMatchParam.class)
                .register(Bmv2FlowRuleWrapper.class)
                .build();

        flowRules = storageService.<Bmv2TableEntryReference, Bmv2FlowRuleWrapper>eventuallyConsistentMapBuilder()
                .withSerializer(kryo)
                .withTimestampProvider((k, v) -> new WallClockTimestamp())
                .withName("onos-bmv2-flowrules")
                .build();

        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        log.info("Stopped");
    }

    @Override
    public Bmv2FlowRuleTranslator getFlowRuleTranslator() {
        return translator;
    }

    @Override
    public Bmv2FlowRuleWrapper lookup(Bmv2TableEntryReference entryRef) {
        checkNotNull(entryRef, "table entry reference cannot be null");
        return flowRules.get(entryRef);
    }

    @Override
    public void bind(Bmv2TableEntryReference entryRef, Bmv2FlowRuleWrapper rule) {
        checkNotNull(entryRef, "table entry reference cannot be null");
        checkNotNull(rule, "bmv2 flow rule cannot be null");
        flowRules.put(entryRef, rule);
    }

    @Override
    public void unbind(Bmv2TableEntryReference entryRef) {
        checkNotNull(entryRef, "table entry reference cannot be null");
        flowRules.remove(entryRef);
    }

    @Override
    public void unbindAll(DeviceId deviceId) {
        flowRules.keySet()
                .stream()
                .filter(entryRef -> entryRef.deviceId().equals(deviceId))
                .forEach(flowRules::remove);
    }
}
