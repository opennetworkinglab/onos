/*
 * Copyright 2015 Open Networking Laboratory
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

import com.google.common.collect.ImmutableList;
import org.onosproject.net.flow.FlowRuleBatchOperation;
import org.onosproject.net.intent.Intent;

import java.util.LinkedList;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

class Withdrawn implements CompletedIntentUpdate {

    // TODO: define an interface and use it, instead of IntentManager
    private final IntentManager intentManager;
    private final Intent intent;
    private final List<Intent> installables;
    private final List<FlowRuleBatchOperation> batches;
    private int currentBatch;

    Withdrawn(IntentManager intentManager,
              Intent intent, List<Intent> installables, List<FlowRuleBatchOperation> batches) {
        this.intentManager = checkNotNull(intentManager);
        this.intent = checkNotNull(intent);
        this.installables = ImmutableList.copyOf(installables);
        this.batches = new LinkedList<>(batches);
        this.currentBatch = 0;
    }

    @Override
    public List<Intent> allInstallables() {
        return installables;
    }

    @Override
    public void batchSuccess() {
        currentBatch++;
    }

    @Override
    public FlowRuleBatchOperation currentBatch() {
        return currentBatch < batches.size() ? batches.get(currentBatch) : null;
    }

    @Override
    public void batchFailed() {
        for (int i = batches.size() - 1; i >= currentBatch; i--) {
            batches.remove(i);
        }
        batches.addAll(intentManager.uninstallIntent(intent, installables));
    }
}
