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
package org.onosproject.sfc.util;

import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentHashMap;

import org.onosproject.vtnrsc.FlowClassifierId;
import org.onosproject.vtnrsc.FlowClassifier;
import org.onosproject.vtnrsc.flowclassifier.FlowClassifierListener;
import org.onosproject.vtnrsc.flowclassifier.FlowClassifierService;

import com.google.common.collect.ImmutableList;

/**
 * Provides implementation of the Flow Classifier Service.
 */
public class FlowClassifierAdapter implements FlowClassifierService {

    private final ConcurrentMap<FlowClassifierId, FlowClassifier> flowClassifierStore = new ConcurrentHashMap<>();

    @Override
    public boolean exists(FlowClassifierId id) {
        return flowClassifierStore.containsKey(id);
    }

    @Override
    public int getFlowClassifierCount() {
        return flowClassifierStore.size();
    }

    @Override
    public Iterable<FlowClassifier> getFlowClassifiers() {
        return ImmutableList.copyOf(flowClassifierStore.values());
    }

    @Override
    public FlowClassifier getFlowClassifier(FlowClassifierId id) {
        return flowClassifierStore.get(id);
    }

    @Override
    public boolean createFlowClassifier(FlowClassifier flowClassifier) {
        FlowClassifierId id = flowClassifier.flowClassifierId();

        flowClassifierStore.put(id, flowClassifier);
        if (!flowClassifierStore.containsKey(id)) {
            return false;
        }
        return true;
    }

    @Override
    public boolean updateFlowClassifier(FlowClassifier flowClassifier) {

        if (!flowClassifierStore.containsKey(flowClassifier.flowClassifierId())) {
            return false;
        }

        flowClassifierStore.put(flowClassifier.flowClassifierId(), flowClassifier);

        if (!flowClassifier.equals(flowClassifierStore.get(flowClassifier.flowClassifierId()))) {
            return false;
        }
        return true;
    }

    @Override
    public boolean removeFlowClassifier(FlowClassifierId id) {
        return true;
    }

    @Override
    public void addListener(FlowClassifierListener listener) {
    }

    @Override
    public void removeListener(FlowClassifierListener listener) {
    }
}
